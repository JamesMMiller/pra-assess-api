package templates

import models.{AssessmentTemplate, CheckItem, ContextResource}

object TemplateRegistry {

  val mdtpPra = AssessmentTemplate(
    id = "mdtp-pra",
    name = "MDTP Platform Readiness Assessment",
    description = "Comprehensive check for compliance with MDTP standards and best practices.",
    basePrompt = """
      |You are an expert Platform Readiness Assessor for the MDTP platform (Multichannel Digital Tax Platform).
      |Your role is to review code against specific architectural standards and provide actionable feedback.
      |You should be strict but helpful, citing evidence for your findings.
      |Focus on security, maintainability, and adherence to HMRC patterns.
      |
      |When assessing:
      |- Verify if the code exists and is used correctly.
      |- Check for forbidden patterns or libraries.
      |- Look for configuration that matches the requirements.
      |- If you cannot determine the answer from the code provided, state "N/A" or "Requires Review" and explain why.
      |
      |You must return your assessment in the following JSON format:
      |{
      |  "status": "PASS" | "WARNING" | "FAIL",
      |  "confidence": <float between 0.0 and 1.0>,
      |  "requiresReview": <boolean>,
      |  "reason": "<concise explanation>",
      |  "evidence": [
      |    { "filePath": "<path>", "lineStart": <int>, "lineEnd": <int> }
      |  ]
      |}
      """.stripMargin.trim,
    contextResources = Seq(
      ContextResource(
        name = "MDTP Handbook",
        url = "https://docs.tax.service.gov.uk/mdtp-handbook/",
        description = "Official documentation for MDTP architectural standards and patterns."
      ),
      ContextResource(
        name = "HMRC Frontend Style Guide",
        url = "https://design.tax.service.gov.uk/",
        description = "Standards for frontend services."
      )
    ),
    checks = Seq(
      // 1. Build & Resilience
      CheckItem("1.A", "Does the service implement any non-standard patterns, or contradict MDTP Opinions?"),
      CheckItem("1.B", "Are there any Bobby Rule violations? (Check build.sbt for deprecated dependencies)"),
      CheckItem("1.C", "Is the service using any deprecated HMRC Libraries?"),
      CheckItem("1.D", "Are HTTP Verbs used for outbound calls instead of raw WSClient?"),
      CheckItem("1.E", "Is the README.md up to date and fit for purpose?"),
      CheckItem("1.F", "Are appropriate timeouts set on I/O operations?"),

      // 2. Data Persistence
      CheckItem("2.A", "If using Mongo, is it used for JSON data persistence?"),
      CheckItem("2.C", "Does public Mongo have a TTL <= 7 days?"),
      CheckItem("2.D", "Is Field Level Encryption used where necessary?"),
      CheckItem("2.E", "Does protected Mongo have a TTL <= 30 days?"),
      CheckItem("2.G", "Is Object Store used for Binary data persistence?"),

      // 4. Security
      CheckItem("4.A", "Is Frontend Authentication implemented via Stride/Auth services?"),
      CheckItem("4.B", "Are Public Microservices authenticated and authorised by default?"),
      CheckItem("4.C", "Are Protected Microservices authenticated and authorised by default?"),

      // 5. Admin Services
      CheckItem("5.A", "Ensure there is no public route to the service in MDTP Frontend Routes."),
      CheckItem("5.C", "Is access to this admin service secured with Internal or Stride Auth?"),

      // 6. Caching
      CheckItem("6.A", "Is caching used? (Should not be used by default unless necessary)"),

      // 8. Logging
      CheckItem("8.A", "Ensure no PII (Personally Identifiable Information) is being logged."),
      CheckItem("8.B", "Are logging levels correct? (WARN for production)"),
      CheckItem("8.C", "Are exceptions logged according to MDTP best practices?"),

      // 9. Error Handling
      CheckItem("9.A", "Are HTTP Verbs returns handled correctly?"),

      // 11. Auditing
      CheckItem("11.A", "Are explicit events audited in accordance with requirements?"),
      CheckItem("11.B", "Is implicit auditing enabled in configuration?"),

      // 13. Testing
      CheckItem("13.A", "Does the service have Unit Tests?"),
      CheckItem("13.B", "Does the service have Integration Tests?"),
      CheckItem("13.C", "Does the service have UI Journey Tests?"),
      CheckItem("13.E", "Does the service have Security Tests?"),
      CheckItem("13.F", "Does the service have Accessibility Tests?"),

      // 15. Virus Checking
      CheckItem("15.A", "If uploading files, is the Upscan service incorporated?"),

      // 16. Tracking Consent
      CheckItem("16.A", "Does the frontend service include the tracking consent link?")
    )
  )

  val test = AssessmentTemplate(
    id = "test",
    name = "Test Template",
    description = "Minimal template for integration testing.",
    basePrompt = """
      |You are a test assessor.
      |Return JSON: {"status": "PASS", "confidence": 1.0, "requiresReview": false, "reason": "Test passed", "evidence": []}
      """.stripMargin.trim,
    contextResources = Seq.empty,
    checks = Seq(
      CheckItem("TEST-1", "Is this a valid project?")
    )
  )

  val all: Map[String, AssessmentTemplate] = Map(
    mdtpPra.id -> mdtpPra,
    test.id    -> test
  )

  def get(id: String): Option[AssessmentTemplate] = all.get(id)

  def default: AssessmentTemplate = mdtpPra

  def listAll: Seq[AssessmentTemplate] = all.values.toSeq
}
