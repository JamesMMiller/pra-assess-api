package controllers

import javax.inject._
import play.api.mvc._
import services.AssessmentOrchestrator
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssessmentController @Inject() (
    val controllerComponents: ControllerComponents,
    assessmentOrchestrator: AssessmentOrchestrator
)(implicit ec: ExecutionContext)
    extends BaseController {

  def assessBatch(repoUrl: String, templateId: Option[String] = None, model: Option[String] = None) = Action.async {
    val githubUrlPattern = "https://github.com/([^/]+)/([^/]+).*".r

    repoUrl match {
      case githubUrlPattern(owner, repo) =>
        val template = templateId
          .flatMap(templates.TemplateRegistry.get)
          .getOrElse(templates.TemplateRegistry.default)

        val selectedModel = model.getOrElse("gemini-2.5-flash")

        assessmentOrchestrator
          .runBatchAssessment(owner, repo, template, selectedModel)
          .map { results =>
            Ok(Json.toJson(results))
          }
          .recover {
            case e: models.GeminiRateLimitException =>
              TooManyRequests(
                Json.obj(
                  "error"      -> "Rate limit exceeded",
                  "message"    -> e.getMessage,
                  "retryAfter" -> e.retryAfter
                )
              )
            case e: Exception =>
              InternalServerError(Json.obj("error" -> e.getMessage))
          }

      case _ =>
        Future.successful(BadRequest(Json.obj("error" -> "Invalid GitHub URL format")))
    }
  }
}
