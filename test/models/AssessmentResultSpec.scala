package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class AssessmentResultSpec extends AnyWordSpec with Matchers {

  "AssessmentStatus" should {

    "parse PASS status" in {
      AssessmentStatus.fromString("PASS") shouldBe AssessmentStatus.Pass
      AssessmentStatus.fromString("pass") shouldBe AssessmentStatus.Pass
    }

    "parse FAIL status" in {
      AssessmentStatus.fromString("FAIL") shouldBe AssessmentStatus.Fail
      AssessmentStatus.fromString("fail") shouldBe AssessmentStatus.Fail
    }

    "parse WARNING status" in {
      AssessmentStatus.fromString("WARNING") shouldBe AssessmentStatus.Warning
      AssessmentStatus.fromString("warning") shouldBe AssessmentStatus.Warning
    }

    "parse N/A status" in {
      AssessmentStatus.fromString("N/A") shouldBe AssessmentStatus.NotApplicable
      AssessmentStatus.fromString("NA") shouldBe AssessmentStatus.NotApplicable
    }

    "default to WARNING for unknown status" in {
      AssessmentStatus.fromString("UNKNOWN") shouldBe AssessmentStatus.Warning
      AssessmentStatus.fromString("") shouldBe AssessmentStatus.Warning
    }

    "serialize to JSON correctly" in {
      Json.toJson(AssessmentStatus.Pass: AssessmentStatus).toString() shouldBe "\"PASS\""
      Json.toJson(AssessmentStatus.Fail: AssessmentStatus).toString() shouldBe "\"FAIL\""
      Json.toJson(AssessmentStatus.Warning: AssessmentStatus).toString() shouldBe "\"WARNING\""
      Json.toJson(AssessmentStatus.NotApplicable: AssessmentStatus).toString() shouldBe "\"N/A\""
    }

    "deserialize from JSON correctly" in {
      Json.parse("\"PASS\"").as[AssessmentStatus] shouldBe AssessmentStatus.Pass
      Json.parse("\"FAIL\"").as[AssessmentStatus] shouldBe AssessmentStatus.Fail
      Json.parse("\"WARNING\"").as[AssessmentStatus] shouldBe AssessmentStatus.Warning
      Json.parse("\"N/A\"").as[AssessmentStatus] shouldBe AssessmentStatus.NotApplicable
    }
  }

  "Evidence" should {

    "serialize to JSON correctly" in {
      val evidence = Evidence("https://github.com/hmrc/repo/blob/main/file.scala#L10-L20")
      val json     = Json.toJson(evidence)

      (json \ "githubUrl").as[String] shouldBe "https://github.com/hmrc/repo/blob/main/file.scala#L10-L20"
    }

    "deserialize from JSON correctly" in {
      val json     = Json.parse("""{"githubUrl":"https://github.com/hmrc/repo/blob/main/file.scala#L10"}""")
      val evidence = json.as[Evidence]

      evidence.githubUrl shouldBe "https://github.com/hmrc/repo/blob/main/file.scala#L10"
    }
  }

  "AssessmentResult" should {

    "serialize to JSON correctly" in {
      val result = AssessmentResult(
        checkId = "1.A",
        checkDescription = "Test check",
        status = AssessmentStatus.Pass,
        confidence = 0.95,
        requiresReview = false,
        reason = "All good",
        evidence = Seq(Evidence("https://github.com/hmrc/repo/blob/main/file.scala#L10"))
      )

      val json = Json.toJson(result)

      (json \ "checkId").as[String] shouldBe "1.A"
      (json \ "checkDescription").as[String] shouldBe "Test check"
      (json \ "status").as[String] shouldBe "PASS"
      (json \ "confidence").as[Double] shouldBe 0.95
      (json \ "requiresReview").as[Boolean] shouldBe false
      (json \ "reason").as[String] shouldBe "All good"
      (json \ "evidence").as[Seq[Evidence]].length shouldBe 1
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse("""{
        "checkId": "2.B",
        "checkDescription": "Security check",
        "status": "FAIL",
        "confidence": 0.8,
        "requiresReview": true,
        "reason": "Issue found",
        "evidence": [
          {"githubUrl": "https://github.com/hmrc/repo/blob/main/file.scala#L10"}
        ]
      }""")

      val result = json.as[AssessmentResult]

      result.checkId shouldBe "2.B"
      result.checkDescription shouldBe "Security check"
      result.status shouldBe AssessmentStatus.Fail
      result.confidence shouldBe 0.8
      result.requiresReview shouldBe true
      result.reason shouldBe "Issue found"
      result.evidence.length shouldBe 1
    }

    "handle empty evidence list" in {
      val result = AssessmentResult(
        checkId = "3.C",
        checkDescription = "Test",
        status = AssessmentStatus.NotApplicable,
        confidence = 1.0,
        requiresReview = false,
        reason = "Not applicable",
        evidence = Seq.empty
      )

      val json = Json.toJson(result)
      (json \ "evidence").as[Seq[Evidence]] shouldBe empty
    }
  }
}
