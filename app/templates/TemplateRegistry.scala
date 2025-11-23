package templates

import models.{AssessmentTemplate, CheckItem, ContextResource}

object TemplateRegistry {

  val mdtpPra: AssessmentTemplate = AssessmentTemplate(
    id = "mdtp-pra",
    name = "MDTP Platform Readiness Assessment",
    description = "Assesses HMRC digital services against MDTP architectural standards and best practices",
    contextResources = Seq(
      ContextResource(
        name = "MDTP Handbook",
        url = "https://docs.tax.service.gov.uk/mdtp-handbook/",
        description =
          "Official HMRC Multi-channel Digital Tax Platform handbook containing patterns, standards, and best practices for building digital services"
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
