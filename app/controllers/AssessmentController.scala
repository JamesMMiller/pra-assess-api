package controllers

import javax.inject._
import play.api.mvc._
import services.AssessmentOrchestrator
import play.api.libs.EventSource
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext

@Singleton
class AssessmentController @Inject() (
    val controllerComponents: ControllerComponents,
    orchestrator: AssessmentOrchestrator
)(implicit ec: ExecutionContext)
    extends BaseController {

  def assess(repoUrl: String) = Action {
    // Simple parsing of repoUrl: https://github.com/owner/repo
    val parts = repoUrl.stripPrefix("https://github.com/").split("/")
    if (parts.length >= 2) {
      val owner = parts(0)
      val repo  = parts(1)

      val source = orchestrator
        .runAssessment(owner, repo)
        .map(result => Json.toJson(result))
        .via(EventSource.flow)

      Ok.chunked(source).as("text/event-stream")
    } else {
      BadRequest("Invalid GitHub URL format. Expected https://github.com/owner/repo")
    }
  }
}
