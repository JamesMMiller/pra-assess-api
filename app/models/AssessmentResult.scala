package models

import play.api.libs.json._

// Type-safe enum for assessment status
sealed trait AssessmentStatus {
  def value: String
}

object AssessmentStatus {
  case object Pass          extends AssessmentStatus { val value = "PASS"    }
  case object Fail          extends AssessmentStatus { val value = "FAIL"    }
  case object Warning       extends AssessmentStatus { val value = "WARNING" }
  case object NotApplicable extends AssessmentStatus { val value = "N/A"     }

  def fromString(s: String): AssessmentStatus = s.toUpperCase match {
    case "PASS"       => Pass
    case "FAIL"       => Fail
    case "WARNING"    => Warning
    case "N/A" | "NA" => NotApplicable
    case _            => Warning // Default to warning for unknown statuses
  }

  implicit val format: Format[AssessmentStatus] = new Format[AssessmentStatus] {
    def reads(json: JsValue): JsResult[AssessmentStatus] = json match {
      case JsString(s) => JsSuccess(fromString(s))
      case _           => JsError("Expected string for AssessmentStatus")
    }
    def writes(status: AssessmentStatus): JsValue = JsString(status.value)
  }
}

case class Evidence(
    githubUrl: String
)

object Evidence {
  implicit val format: OFormat[Evidence] = Json.format[Evidence]
}

case class AssessmentResult(
    checkId: String,
    checkDescription: String,
    status: AssessmentStatus,
    confidence: Double,
    requiresReview: Boolean,
    reason: String,
    evidence: Seq[Evidence]
)

object AssessmentResult {
  implicit val format: OFormat[AssessmentResult] = Json.format[AssessmentResult]
}
