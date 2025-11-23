package models

import play.api.libs.json._

case class CheckItem(
    id: String,
    description: String
)

object CheckItem {
  implicit val format: OFormat[CheckItem] = Json.format[CheckItem]

  // Predefined PRA checklist items
  val items: List[CheckItem] = List(
    CheckItem("1.A", "Does your service implement any non-standard patterns, or contradict any of the MDTP Opinions?"),
    CheckItem("1.C", "Your service should not be using any deprecated HMRC Libraries."),
    CheckItem("2.A", "Are you using Mongo for JSON data persistence?"),
    CheckItem("4.B", "Public Microservices should be authenticated and authorised by default.")
  )
}
