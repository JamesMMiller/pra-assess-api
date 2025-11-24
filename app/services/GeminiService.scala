package services

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import scala.concurrent.{ExecutionContext, Future}
import models.{AssessmentResult, Evidence}

@Singleton
class GeminiService @Inject() (config: Configuration, ws: WSClient)(implicit ec: ExecutionContext) {

  private val apiKey  = config.getOptional[String]("pra.assessment.gemini.apiKey").getOrElse("MISSING_KEY")
  private val model   = "gemini-2.5-flash"
  private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

  // Note on Caching:
  // Gemini Context Caching requires a minimum of 32,768 tokens.
  // Our current strategy uses "Dynamic Discovery" to select only relevant files, keeping the context size
  // well below this limit (typically < 5k tokens). Therefore, explicit caching APIs are not used here.
  // Instead, we optimize for token efficiency by:
  // 1. Summarizing the file tree (Shared Context) once.
  // 2. Using a Base Prompt in the system instruction.
  // 3. Selecting only relevant files for each check.
  def generateSharedContext(
      fileTree: Seq[String],
      template: models.AssessmentTemplate,
      model: String
  ): Future[String] = {
    val contextResourcesText = if (template.contextResources.nonEmpty) {
      val resources = template.contextResources
        .map { resource =>
          s"- ${resource.name} (${resource.url}): ${resource.description}"
        }
        .mkString("\n")
      s"""
         |
         |Reference Resources:
         |$resources
         |
         |Use these resources to understand the standards and patterns this assessment is based on.
         |""".stripMargin
    } else ""

    val prompt = s"""
      |Analyze this file tree and summarize the project structure, framework (e.g. Play, Spring), 
      |language (Scala, Java), and key architectural patterns.
      |Keep it concise (max 100 words).
      |$contextResourcesText
      |File Tree:
      |${fileTree.take(200).mkString("\n")} 
      |(truncated if too long)
      """.stripMargin

    callGemini(prompt, "You are a senior software architect.", model).map { response =>
      extractText(response)
    }
  }

  def selectFiles(
      sharedContext: String,
      checkDescription: String,
      fileTree: Seq[String],
      model: String,
      searchFiles: Seq[String] = Seq.empty
  ): Future[Seq[String]] = {
    val searchFilesContext = if (searchFiles.nonEmpty) {
      s"""
         |The following files were found via code search and might be relevant:
         |${searchFiles.take(20).map(f => s"- $f").mkString("\n")}
         |""".stripMargin
    } else ""

    val prompt = s"""
      |Given this project context:
      |$sharedContext
      |
      |And this file tree:
      |${fileTree.take(500).mkString("\n")}
      |
      |$searchFilesContext
      |
      |Identify the top 3-5 files that are most relevant to checking: "$checkDescription"
      |Return ONLY a JSON array of file paths. Example: ["app/controllers/HomeController.scala", "conf/application.conf"]
      """.stripMargin

    callGemini(prompt, "You are a code analyzer. Return only JSON.", model, jsonMode = true).map { response =>
      val content = extractText(response)
      try {
        Json.parse(content).as[Seq[String]]
      } catch {
        case _: Exception => Seq.empty
      }
    }
  }

  def assessItem(
      owner: String,
      repo: String,
      sharedContext: String,
      template: models.AssessmentTemplate,
      checkItem: models.CheckItem,
      fileContents: Map[String, String],
      model: String
  ): Future[AssessmentResult] = {
    val filesContext = fileContents
      .map { case (path, content) =>
        s"--- $path ---\n$content\n"
      }
      .mkString("\n")

    val prompt = s"""
      |Assess the following files against this check: "${checkItem.description}"
      |
      |Shared Context:
      |$sharedContext
      |
      |Files:
      |$filesContext
      """.stripMargin

    callGemini(prompt, template.basePrompt, model, jsonMode = true).map { response =>
      val content = extractText(response)
      try {
        val json       = Json.parse(content)
        val statusStr  = (json \ "status").asOpt[String].orElse((json \ "result").asOpt[String]).getOrElse("WARNING")
        val status     = models.AssessmentStatus.fromString(statusStr)
        val confidence = (json \ "confidence").as[Double]
        val requiresReview = (json \ "requiresReview").as[Boolean]
        val reason         = (json \ "reason").as[String]

        // Convert evidence to GitHub URLs
        val evidence = (json \ "evidence").as[Seq[JsValue]].map { ev =>
          val filePath  = (ev \ "filePath").as[String]
          val lineStart = (ev \ "lineStart").asOpt[Int]
          val lineEnd   = (ev \ "lineEnd").asOpt[Int]

          val githubUrl = utils.GitHubUrlHelper.generatePermalink(
            owner = owner,
            repo = repo,
            filePath = filePath,
            lineStart = lineStart,
            lineEnd = lineEnd
          )

          models.Evidence(githubUrl)
        }

        AssessmentResult(
          checkId = checkItem.id,
          checkDescription = checkItem.description,
          status = status,
          confidence = confidence,
          requiresReview = requiresReview,
          reason = reason,
          evidence = evidence
        )
      } catch {
        case e: Exception =>
          AssessmentResult(
            checkId = checkItem.id,
            checkDescription = checkItem.description,
            status = models.AssessmentStatus.Warning,
            confidence = 0.0,
            requiresReview = true,
            reason = s"Failed to parse LLM response: ${e.getMessage}. Raw: $content",
            evidence = Seq.empty
          )
      }
    }
  }

  def selectFilesForCategory(
      sharedContext: String,
      categoryDescription: String,
      fileTree: Seq[String],
      model: String,
      searchFiles: Seq[String] = Seq.empty
  ): Future[Seq[String]] = {
    val searchFilesContext = if (searchFiles.nonEmpty) {
      s"""
         |The following files were found via code search and might be relevant:
         |${searchFiles.take(20).map(f => s"- $f").mkString("\n")}
         |""".stripMargin
    } else ""

    val prompt = s"""
      |Given this project context:
      |$sharedContext
      |
      |And this file tree:
      |${fileTree.take(500).mkString("\n")}
      |
      |$searchFilesContext
      |
      |Identify the top 5-10 files that are most relevant to checking the category: "$categoryDescription"
      |Return ONLY a JSON array of file paths. Example: ["app/controllers/HomeController.scala", "conf/application.conf"]
      """.stripMargin

    callGemini(prompt, "You are a code analyzer. Return only JSON.", model, jsonMode = true).map { response =>
      val content = extractText(response)
      try {
        Json.parse(content).as[Seq[String]]
      } catch {
        case _: Exception => Seq.empty
      }
    }
  }

  def generateSearchTerms(
      sharedContext: String,
      categoryDescription: String,
      checks: Seq[models.CheckItem],
      model: String
  ): Future[Seq[String]] = {
    val checksJson = checks.map(c => s"${c.id}: ${c.description}").mkString("\n")

    val prompt = s"""
      |Given this project context:
      |$sharedContext
      |
      |And these checks to assess:
      |$checksJson
      |
      |What search terms would help find relevant files in the codebase?
      |Consider:
      |- Technology names (e.g., "Upscan", "Mongo", "Auth")
      |- Class/interface names (e.g., "AuthConnector", "Repository")
      |- Configuration keys (e.g., "TTL", "timeout")
      |
      |Return ONLY a JSON array of 3-5 search terms. Example: ["Upscan", "AuthConnector", "Mongo"]
      """.stripMargin

    callGemini(prompt, "You are a code search expert. Return only JSON.", model, jsonMode = true).map { response =>
      val content = extractText(response)
      try {
        Json.parse(content).as[Seq[String]]
      } catch {
        case _: Exception => Seq.empty
      }
    }
  }

  def assessBatch(
      owner: String,
      repo: String,
      sharedContext: String,
      template: models.AssessmentTemplate,
      checks: Seq[models.CheckItem],
      fileContents: Map[String, String],
      model: String
  ): Future[Seq[AssessmentResult]] = {
    val filesContext = fileContents
      .map { case (path, content) =>
        s"--- $path ---\n$content\n"
      }
      .mkString("\n")

    val checksJson = Json.toJson(checks.map(c => Json.obj("id" -> c.id, "description" -> c.description))).toString()

    val prompt = s"""
      |Assess the following files against these checks:
      |$checksJson
      |
      |Shared Context:
      |$sharedContext
      |
      |Files:
      |$filesContext
      |
      |Return a JSON array of assessment results, one for each check.
      |Each result must follow this structure:
      |{
      |  "checkId": "id",
      |  "status": "PASS|FAIL|WARNING|N/A",
      |  "confidence": 0.0-1.0,
      |  "requiresReview": true|false,
      |  "reason": "explanation",
      |  "evidence": [{"filePath": "...", "lineStart": 1, "lineEnd": 10}]
      |}
      """.stripMargin

    callGemini(prompt, template.basePrompt, model, jsonMode = true).map { response =>
      val content = extractText(response)
      try {
        val jsonArray = Json.parse(content).as[Seq[JsValue]]
        jsonArray.map { json =>
          val checkId   = (json \ "checkId").as[String]
          val checkItem = checks.find(_.id == checkId).getOrElse(models.CheckItem(checkId, "Unknown check"))

          val statusStr  = (json \ "status").asOpt[String].orElse((json \ "result").asOpt[String]).getOrElse("WARNING")
          val status     = models.AssessmentStatus.fromString(statusStr)
          val confidence = (json \ "confidence").as[Double]
          val requiresReview = (json \ "requiresReview").as[Boolean]
          val reason         = (json \ "reason").as[String]

          val evidence = (json \ "evidence").as[Seq[JsValue]].map { ev =>
            val filePath  = (ev \ "filePath").as[String]
            val lineStart = (ev \ "lineStart").asOpt[Int]
            val lineEnd   = (ev \ "lineEnd").asOpt[Int]

            val githubUrl = utils.GitHubUrlHelper.generatePermalink(
              owner = owner,
              repo = repo,
              filePath = filePath,
              lineStart = lineStart,
              lineEnd = lineEnd
            )

            models.Evidence(githubUrl)
          }

          AssessmentResult(
            checkId = checkItem.id,
            checkDescription = checkItem.description,
            status = status,
            confidence = confidence,
            requiresReview = requiresReview,
            reason = reason,
            evidence = evidence
          )
        }
      } catch {
        case e: Exception =>
          checks.map { check =>
            AssessmentResult(
              checkId = check.id,
              checkDescription = check.description,
              status = models.AssessmentStatus.Warning,
              confidence = 0.0,
              requiresReview = true,
              reason = s"Failed to parse LLM response: ${e.getMessage}. Raw: $content",
              evidence = Seq.empty
            )
          }
      }
    }
  }

  private def callGemini(
      prompt: String,
      systemInstruction: String,
      model: String = this.model,
      jsonMode: Boolean = false
  ): Future[JsValue] = {
    val url = s"$baseUrl/$model:generateContent?key=$apiKey"

    val payload = Json.obj(
      "systemInstruction" -> Json.obj(
        "parts" -> Json.arr(
          Json.obj("text" -> systemInstruction)
        )
      ),
      "contents" -> Json.arr(
        Json.obj(
          "role" -> "user",
          "parts" -> Json.arr(
            Json.obj("text" -> prompt)
          )
        )
      ),
      "generationConfig" -> {
        val baseConfig = if (jsonMode) {
          Json.obj("responseMimeType" -> "application/json")
        } else {
          Json.obj()
        }
        // Enable dynamic thinking for Gemini 2.5+ models
        // thinkingConfig.thinkingBudget: -1 = dynamic (model adjusts based on complexity)
        baseConfig ++ Json.obj(
          "thinkingConfig" -> Json.obj(
            "thinkingBudget" -> -1
          )
        )
      }
    )

    ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(payload)
      .map { response =>
        response.status match {
          case 200 =>
            response.json
          case 429 =>
            throw models.GeminiRateLimitException(
              s"Gemini Rate Limit Exceeded: ${response.body}",
              retryAfter = None // Could parse from headers if available
            )
          case _ =>
            throw new RuntimeException(s"LLM call failed: ${response.status} ${response.body}")
        }
      }
  }

  private def extractText(json: JsValue): String = {
    (json \ "candidates" \ 0 \ "content" \ "parts" \ 0 \ "text").asOpt[String].getOrElse("")
  }
}
