package models

import play.api.libs.json._

case class Evidence(
    filePath: String,
    lineStart: Option[Int],
    lineEnd: Option[Int],
    snippet: Option[String]
)

object Evidence {
  implicit val format: OFormat[Evidence] = Json.format[Evidence]
}

case class AssessmentResult(
    status: String, // PASS, FAIL, WARNING, N/A
    confidence: Double,
    requiresReview: Boolean,
    reason: String,
    evidence: Seq[Evidence]
)

object AssessmentResult {
  implicit val format: OFormat[AssessmentResult] = Json.format[AssessmentResult]
}
