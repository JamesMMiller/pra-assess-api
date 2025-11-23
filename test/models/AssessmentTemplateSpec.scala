package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class AssessmentTemplateSpec extends AnyWordSpec with Matchers {

  "ContextResource" should {

    "serialize to JSON correctly" in {
      val resource = ContextResource("Test Resource", "https://example.com", "Test description")
      val json     = Json.toJson(resource)

      (json \ "name").as[String] shouldBe "Test Resource"
      (json \ "url").as[String] shouldBe "https://example.com"
      (json \ "description").as[String] shouldBe "Test description"
    }

    "deserialize from JSON correctly" in {
      val json     = Json.parse("""{"name":"Test","url":"https://test.com","description":"Desc"}""")
      val resource = json.as[ContextResource]

      resource.name shouldBe "Test"
      resource.url shouldBe "https://test.com"
      resource.description shouldBe "Desc"
    }
  }

  "AssessmentTemplate" should {

    "serialize to JSON correctly" in {
      val template = AssessmentTemplate(
        id = "test-template",
        name = "Test Template",
        description = "Test description",
        contextResources = Seq(ContextResource("Resource", "https://example.com", "Desc")),
        checks = Seq(CheckItem("1.A", "Test check"))
      )

      val json = Json.toJson(template)

      (json \ "id").as[String] shouldBe "test-template"
      (json \ "name").as[String] shouldBe "Test Template"
      (json \ "description").as[String] shouldBe "Test description"
      (json \ "contextResources").as[Seq[ContextResource]].length shouldBe 1
      (json \ "checks").as[Seq[CheckItem]].length shouldBe 1
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse("""{
        "id": "test",
        "name": "Test",
        "description": "Desc",
        "contextResources": [{"name":"R","url":"https://r.com","description":"D"}],
        "checks": [{"id":"1.A","description":"Check"}]
      }""")

      val template = json.as[AssessmentTemplate]

      template.id shouldBe "test"
      template.name shouldBe "Test"
      template.description shouldBe "Desc"
      template.contextResources.length shouldBe 1
      template.checks.length shouldBe 1
    }
  }
}
