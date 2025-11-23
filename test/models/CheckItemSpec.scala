package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class CheckItemSpec extends AnyWordSpec with Matchers {

  "CheckItem" should {

    "serialize to JSON correctly" in {
      val checkItem = CheckItem("1.A", "Test description")
      val json      = Json.toJson(checkItem)

      (json \ "id").as[String] shouldBe "1.A"
      (json \ "description").as[String] shouldBe "Test description"
    }

    "deserialize from JSON correctly" in {
      val json      = Json.parse("""{"id":"2.B","description":"Security check"}""")
      val checkItem = json.as[CheckItem]

      checkItem.id shouldBe "2.B"
      checkItem.description shouldBe "Security check"
    }
  }
}
