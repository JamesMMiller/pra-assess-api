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
  private val model   = "gemini-2.0-flash"
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
      model: String
  ): Future[Seq[String]] = {
    val prompt = s"""
      |Given this project context:
      |$sharedContext
      |
      |And this file tree:
      |${fileTree.take(500).mkString("\n")}
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
        val json           = Json.parse(content)
        val status         = models.AssessmentStatus.fromString((json \ "status").as[String])
        val confidence     = (json \ "confidence").as[Double]
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
      "generationConfig" -> (if (jsonMode) Json.obj("responseMimeType" -> "application/json") else Json.obj())
    )

    ws.url(url)
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(payload)
      .map { response =>
        if (response.status == 200) {
          response.json
        } else {
          throw new RuntimeException(s"LLM call failed: ${response.status} ${response.body}")
        }
      }
  }

  private def extractText(json: JsValue): String = {
    (json \ "candidates" \ 0 \ "content" \ "parts" \ 0 \ "text").asOpt[String].getOrElse("")
  }
}
