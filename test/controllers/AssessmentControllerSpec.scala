package controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AssessmentControllerSpec extends AnyWordSpec with Matchers {

  "AssessmentController" should {

    "parse GitHub URLs correctly" in {
      val url     = "https://github.com/hmrc/pillar2-frontend"
      val pattern = "https://github.com/([^/]+)/([^/]+).*".r

      url match {
        case pattern(owner, repo) =>
          owner shouldBe "hmrc"
          repo shouldBe "pillar2-frontend"
        case _ => fail("URL should match pattern")
      }
    }

    "reject invalid GitHub URLs" in {
      val invalidUrls = Seq(
        "not-a-url",
        "https://github.com/invalid",
        "https://gitlab.com/owner/repo"
      )

      val pattern = "https://github.com/([^/]+)/([^/]+).*".r

      invalidUrls.foreach { url =>
        url match {
          case pattern(_, _) => fail(s"$url should not match")
          case _             => succeed
        }
      }
    }
  }

  // Note: AssessmentController is primarily tested via AssessmentIntegrationSpec
  // which tests the full end-to-end flow including SSE streaming
}
