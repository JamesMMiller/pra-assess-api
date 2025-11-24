package integration

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

class AssessmentIntegrationSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(1, Seconds))
  implicit lazy val materializer: org.apache.pekko.stream.Materializer = app.materializer

  "AssessmentController" should {

    "stream assessment results for pillar2-frontend" in {
      val apiKey = app.configuration.getOptional[String]("pra.assessment.gemini.apiKey")
      if (apiKey.isEmpty || apiKey.contains("MISSING_KEY")) {
        cancel("Gemini API key not configured, skipping integration test")
      }

      val repoUrl = "https://github.com/hmrc/pillar2-frontend"
      val request = FakeRequest(GET, s"/assess/stream?repoUrl=$repoUrl&templateId=test")

      val result = route(app, request).get

      val statusVal = status(result)
      if (statusVal != OK) {
        println(s"Test failed with status: $statusVal")
        println(s"Body: ${contentAsString(result)}")
      }
      statusVal mustBe OK
      contentType(result) mustBe Some("text/event-stream")

      // We can't easily consume the full stream in a simple unit test helper without a materializer,
      // but checking status and content type confirms the endpoint is up and running.
      // For a deeper check, we'd need to consume the source.

      // Let's just ensure it doesn't crash immediately
      val bodyText = contentAsString(result)
      val events   = bodyText.split("\n\n").filter(_.startsWith("data: ")).map(_.stripPrefix("data: "))

      events.length must be > 0

      // Check that we can parse at least one result with new structure
      val firstResult = play.api.libs.json.Json.parse(events.head)
      (firstResult \ "checkId").asOpt[String] mustBe Some("TEST-1")
      (firstResult \ "checkDescription").asOpt[String] mustBe defined
      (firstResult \ "status").asOpt[String] mustBe defined
      (firstResult \ "confidence").asOpt[Double] mustBe defined
      (firstResult \ "requiresReview").asOpt[Boolean] mustBe defined
      (firstResult \ "reason").asOpt[String] mustBe defined

      // Verify evidence contains GitHub URLs
      val evidence = (firstResult \ "evidence").asOpt[Seq[play.api.libs.json.JsValue]].getOrElse(Seq.empty)
      if (evidence.nonEmpty) {
        val firstEvidence = evidence.head
        val githubUrl     = (firstEvidence \ "githubUrl").asOpt[String]
        githubUrl mustBe defined
        githubUrl.get must include("github.com")
      }

      println(s"Stream finished for $repoUrl. Received ${events.length} events.")
      println(s"First event: ${events.head}")
      for (event <- events) {
        println(s"Event number: ${events.indexOf(event)}")
        println(s"Event: $event")
      }
    }

    "accept model parameter" in {
      val apiKey = app.configuration.getOptional[String]("pra.assessment.gemini.apiKey")
      if (apiKey.isEmpty || apiKey.contains("MISSING_KEY")) {
        cancel("Gemini API key not configured, skipping integration test")
      }

      val repoUrl = "https://github.com/hmrc/pillar2-frontend"
      // Use a valid model name to ensure it doesn't error out on the API side
      val request = FakeRequest(GET, s"/assess/stream?repoUrl=$repoUrl&model=gemini-2.0-flash&templateId=test")

      val result = route(app, request).get
      status(result) mustBe OK
    }
  }
}
