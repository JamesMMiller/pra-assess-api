package controllers

import javax.inject._
import play.api.mvc._
import services.AssessmentOrchestrator
import org.apache.pekko.stream.scaladsl.Source
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext

@Singleton
class AssessmentController @Inject() (
    val controllerComponents: ControllerComponents,
    assessmentOrchestrator: AssessmentOrchestrator
)(implicit ec: ExecutionContext)
    extends BaseController {

  def assess(repoUrl: String, templateId: Option[String] = None) = Action {
    val githubUrlPattern = "https://github.com/([^/]+)/([^/]+).*".r

    repoUrl match {
      case githubUrlPattern(owner, repo) =>
        val template = templateId
          .flatMap(templates.TemplateRegistry.get)
          .getOrElse(templates.TemplateRegistry.default)

        val resultSource = assessmentOrchestrator.runAssessment(owner, repo, template)
        val eventSource  = resultSource.map(result => Json.toJson(result))

        Ok.chunked(eventSource via EventSource.flow)
          .as(ContentTypes.EVENT_STREAM)

      case _ =>
        BadRequest(Json.obj("error" -> "Invalid GitHub URL format"))
    }
  }
}
