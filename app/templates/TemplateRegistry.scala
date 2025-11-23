package templates

import models.{AssessmentTemplate, CheckItem, ContextResource}

object TemplateRegistry {

  val mdtpPra = AssessmentTemplate(
    id = "mdtp-pra",
    name = "MDTP Platform Readiness Assessment",
    description = "Checks for compliance with MDTP standards and best practices.",
    basePrompt = """
      |You are an expert Platform Readiness Assessor for the MDTP platform (Multichannel Digital Tax Platform).
      |Your role is to review code against specific architectural standards and provide actionable feedback.
      |You should be strict but helpful, citing evidence for your findings.
      |Focus on security, maintainability, and adherence to HMRC patterns.
      """.stripMargin.trim,
    contextResources = Seq(
      ContextResource(
        name = "MDTP Handbook",
        url = "https://docs.tax.service.gov.uk/mdtp-handbook/",
        description = "Official documentation for MDTP architectural standards and patterns."
      )
    ),
    checks = Seq(
      CheckItem(
        "1.A",
        "Does your service implement any non-standard patterns, or contradict any of the MDTP Opinions?"
      ),
      CheckItem("1.C", "Your service should not be using any deprecated HMRC Libraries."),
      CheckItem("2.A", "Are you using Mongo for JSON data persistence?"),
      CheckItem("4.B", "Public Microservices should be authenticated and authorised by default.")
    )
  )

  val all: Map[String, AssessmentTemplate] = Map(
    mdtpPra.id -> mdtpPra
  )

  def get(id: String): Option[AssessmentTemplate] = all.get(id)

  def default: AssessmentTemplate = mdtpPra

  def listAll: Seq[AssessmentTemplate] = all.values.toSeq
}
