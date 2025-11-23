package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GitHubUrlHelperSpec extends AnyWordSpec with Matchers {

  "GitHubUrlHelper.generatePermalink" should {

    "generate URL without line numbers" in {
      val url = GitHubUrlHelper.generatePermalink(
        owner = "hmrc",
        repo = "pillar2-frontend",
        filePath = "app/controllers/HomeController.scala"
      )

      url shouldBe "https://github.com/hmrc/pillar2-frontend/blob/main/app/controllers/HomeController.scala"
    }

    "generate URL with single line number" in {
      val url = GitHubUrlHelper.generatePermalink(
        owner = "hmrc",
        repo = "pillar2-frontend",
        filePath = "app/controllers/HomeController.scala",
        lineStart = Some(15)
      )

      url shouldBe "https://github.com/hmrc/pillar2-frontend/blob/main/app/controllers/HomeController.scala#L15"
    }

    "generate URL with line range" in {
      val url = GitHubUrlHelper.generatePermalink(
        owner = "hmrc",
        repo = "pillar2-frontend",
        filePath = "app/controllers/HomeController.scala",
        lineStart = Some(15),
        lineEnd = Some(20)
      )

      url shouldBe "https://github.com/hmrc/pillar2-frontend/blob/main/app/controllers/HomeController.scala#L15-L20"
    }

    "generate URL with same start and end line" in {
      val url = GitHubUrlHelper.generatePermalink(
        owner = "hmrc",
        repo = "pillar2-frontend",
        filePath = "app/controllers/HomeController.scala",
        lineStart = Some(15),
        lineEnd = Some(15)
      )

      url shouldBe "https://github.com/hmrc/pillar2-frontend/blob/main/app/controllers/HomeController.scala#L15"
    }

    "generate URL with custom branch" in {
      val url = GitHubUrlHelper.generatePermalink(
        owner = "hmrc",
        repo = "pillar2-frontend",
        filePath = "app/controllers/HomeController.scala",
        branch = "develop",
        lineStart = Some(15),
        lineEnd = Some(20)
      )

      url shouldBe "https://github.com/hmrc/pillar2-frontend/blob/develop/app/controllers/HomeController.scala#L15-L20"
    }

    "handle file paths with special characters" in {
      val url = GitHubUrlHelper.generatePermalink(
        owner = "hmrc",
        repo = "pillar2-frontend",
        filePath = "app/views/some-view.scala.html"
      )

      url shouldBe "https://github.com/hmrc/pillar2-frontend/blob/main/app/views/some-view.scala.html"
    }
  }
}
