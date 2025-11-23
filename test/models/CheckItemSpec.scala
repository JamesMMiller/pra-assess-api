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

    "have predefined items" in {
      CheckItem.items should not be empty
      CheckItem.items.length shouldBe 4
    }

    "have items with valid IDs" in {
      CheckItem.items.foreach { item =>
        item.id should not be empty
        item.id should fullyMatch regex "[0-9]+\\.[A-Z]+"
      }
    }

    "have items with descriptions" in {
      CheckItem.items.foreach { item =>
        item.description should not be empty
        item.description.length should be > 10
      }
    }

    "contain expected check items" in {
      val ids = CheckItem.items.map(_.id)
      ids should contain("1.A")
      ids should contain("1.C")
      ids should contain("2.A")
      ids should contain("4.B")
    }

    "have unique IDs" in {
      val ids = CheckItem.items.map(_.id)
      ids.distinct.length shouldBe ids.length
    }
  }
}
