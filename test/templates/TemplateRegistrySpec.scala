package templates

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TemplateRegistrySpec extends AnyWordSpec with Matchers {

  "TemplateRegistry" should {

    "have mdtpPra template" in {
      TemplateRegistry.mdtpPra.id shouldBe "mdtp-pra"
      TemplateRegistry.mdtpPra.name should not be empty
      TemplateRegistry.mdtpPra.description should not be empty
      TemplateRegistry.mdtpPra.basePrompt should include("Platform Readiness Assessor")
    }

    "have context resources in mdtpPra" in {
      TemplateRegistry.mdtpPra.contextResources should not be empty
      val mdtpHandbook = TemplateRegistry.mdtpPra.contextResources.head
      mdtpHandbook.name shouldBe "MDTP Handbook"
      mdtpHandbook.url should include("docs.tax.service.gov.uk")
    }

    "have checks in mdtpPra" in {
      TemplateRegistry.mdtpPra.checks should not be empty
      TemplateRegistry.mdtpPra.checks.length shouldBe 4
    }

    "have valid check IDs in mdtpPra" in {
      TemplateRegistry.mdtpPra.checks.foreach { item =>
        item.id should not be empty
        item.id should fullyMatch regex "[0-9]+\\.[A-Z]+"
      }
    }

    "have unique check IDs in mdtpPra" in {
      val ids = TemplateRegistry.mdtpPra.checks.map(_.id)
      ids.distinct.length shouldBe ids.length
    }

    "return template by ID" in {
      TemplateRegistry.get("mdtp-pra") shouldBe Some(TemplateRegistry.mdtpPra)
    }

    "return None for unknown template ID" in {
      TemplateRegistry.get("unknown") shouldBe None
    }

    "have default template" in {
      TemplateRegistry.default shouldBe TemplateRegistry.mdtpPra
    }

    "list all templates" in {
      val all = TemplateRegistry.listAll
      all should contain(TemplateRegistry.mdtpPra)
      all.length shouldBe 1
    }
  }
}
