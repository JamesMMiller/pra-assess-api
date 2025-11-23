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

  def generateSharedContext(fileTree: Seq[String]): Future[String] = {
    val prompt = s"""
      |Analyze this file tree and summarize the project structure, framework (e.g. Play, Spring), 
      |language (Scala, Java), and key architectural patterns.
      |Keep it concise (max 100 words).
      |
      |File Tree:
      |${fileTree.take(200).mkString("\n")} 
      |(truncated if too long)
      """.stripMargin

    callGemini(prompt, "You are a senior software architect.").map { response =>
      extractText(response)
    }
  }

  def selectFiles(sharedContext: String, item: String, fileTree: Seq[String]): Future[Seq[String]] = {
    val prompt = s"""
      |Context: $sharedContext
      |Checklist Item: $item
      |
      |Identify up to 5 most relevant files from the list below to assess this item.
      |Return ONLY a JSON array of strings. Example: ["build.sbt", "app/controllers/HomeController.scala"]
      |
      |Files:
      |${fileTree.take(500).mkString("\n")}
      """.stripMargin

    callGemini(prompt, "You are a file selector helper. Return only JSON.", jsonMode = true).map { response =>
      val content = extractText(response)
      try {
        val json = Json.parse(content)
        if (json.asOpt[Seq[String]].isDefined) json.as[Seq[String]]
        else (json \ "files").asOpt[Seq[String]].getOrElse(Seq.empty)
      } catch {
        case _: Exception => Seq.empty
      }
    }
  }

  def assessItem(sharedContext: String, item: String, fileContents: Map[String, String]): Future[AssessmentResult] = {
    val filesContext = fileContents
      .map { case (path, content) =>
        s"--- $path ---\n$content\n"
      }
      .mkString("\n")

    val prompt = s"""
      |Context: $sharedContext
      |Checklist Item: $item
      |
      |Assess if the code passes the check.
      |Return JSON matching this schema:
      |{
      |  "status": "PASS" | "FAIL" | "WARNING" | "N/A",
      |  "confidence": 0.0-1.0,
      |  "requiresReview": boolean,
      |  "reason": "string",
      |  "evidence": [{ "filePath": "string", "lineStart": int, "lineEnd": int, "snippet": "string" }]
      |}
      |
      |Code:
      |$filesContext
      """.stripMargin

    callGemini(prompt, "You are a PRA Assessor. Return only JSON.", jsonMode = true).map { response =>
      val content = extractText(response)
      try {
        Json.parse(content).as[AssessmentResult]
      } catch {
        case e: Exception =>
          AssessmentResult(
            "WARNING",
            0.0,
            true,
            s"Failed to parse LLM response: ${e.getMessage}. Raw: $content",
            Seq.empty
          )
      }
    }
  }

  private def callGemini(userPrompt: String, systemPrompt: String, jsonMode: Boolean = false): Future[JsValue] = {
    val url = s"$baseUrl/$model:generateContent?key=$apiKey"

    val payload = Json.obj(
      "contents" -> Json.arr(
        Json.obj(
          "parts" -> Json.arr(
            Json.obj("text" -> s"$systemPrompt\n\n$userPrompt")
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
