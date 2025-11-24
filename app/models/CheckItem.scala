package models

import play.api.libs.json._

case class CheckItem(
    id: String,
    description: String,
    searchTerms: Option[Seq[String]] = None
)

object CheckItem {
  implicit val format: OFormat[CheckItem] = Json.format[CheckItem]
}
