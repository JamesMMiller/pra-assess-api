package models

import play.api.libs.json._

case class ContextResource(
    name: String,
    url: String,
    description: String
)

object ContextResource {
  implicit val format: OFormat[ContextResource] = Json.format[ContextResource]
}

case class AssessmentTemplate(
    id: String,
    name: String,
    description: String,
    contextResources: Seq[ContextResource],
    checks: Seq[CheckItem]
)

object AssessmentTemplate {
  implicit val format: OFormat[AssessmentTemplate] = Json.format[AssessmentTemplate]
}
