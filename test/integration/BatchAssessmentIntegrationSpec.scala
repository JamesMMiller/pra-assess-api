package integration

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

class BatchAssessmentIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(1, Seconds))

  "AssessmentController Batch Endpoint" should {

    "return batch results for pillar2-frontend" in {
      val apiKey = app.configuration.getOptional[String]("pra.assessment.gemini.apiKey")
      if (apiKey.isEmpty || apiKey.contains("MISSING_KEY")) {
        cancel("Gemini API key not configured, skipping integration test")
      }

      val repoUrl = "https://github.com/hmrc/pillar2-frontend"
      // Use 'test' template to be faster and cheaper, but batch logic still applies
      val request = FakeRequest(GET, s"/assess/batch?repoUrl=$repoUrl&templateId=test")

      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val json    = contentAsJson(result)
      val results = json.as[Seq[play.api.libs.json.JsValue]]

      results.length must be > 0
      (results.head \ "checkId").asOpt[String] mustBe defined
      (results.head \ "status").asOpt[String] mustBe defined
    }
  }
}
