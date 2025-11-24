package integration

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

class AssessmentIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(1, Seconds))

  "AssessmentController" should {

    "return batch assessment results for pillar2-frontend" in {
      val apiKey = app.configuration.getOptional[String]("pra.assessment.gemini.apiKey")
      if (apiKey.isEmpty || apiKey.contains("MISSING_KEY")) {
        cancel("Gemini API key not configured, skipping integration test")
      }

      val repoUrl = "https://github.com/hmrc/pillar2-frontend"
      val request = FakeRequest(GET, s"/assess/batch?repoUrl=$repoUrl&templateId=test")

      val result = route(app, request).get

      whenReady(result) { _ =>
        val statusVal = status(result)
        if (statusVal != OK) {
          println(s"Test failed with status: $statusVal")
          println(s"Body: ${contentAsString(result)}")
        }
        statusVal mustBe OK
        contentType(result) mustBe Some("application/json")

        val bodyText = contentAsString(result)
        val results  = play.api.libs.json.Json.parse(bodyText).as[Seq[play.api.libs.json.JsValue]]

        results.length must be > 0

        // Check that we can parse at least one result
        val firstResult = results.head
        (firstResult \ "checkId").asOpt[String] mustBe Some("TEST-1")
        (firstResult \ "checkDescription").asOpt[String] mustBe defined
        (firstResult \ "status").asOpt[String] mustBe defined
        (firstResult \ "confidence").asOpt[Double] mustBe defined
        (firstResult \ "requiresReview").asOpt[Boolean] mustBe defined
        (firstResult \ "reason").asOpt[String] mustBe defined

        println(s"Batch assessment finished for $repoUrl. Received ${results.length} results.")
      }
    }
  }
}
