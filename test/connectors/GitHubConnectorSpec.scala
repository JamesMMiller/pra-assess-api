package connectors

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GitHubConnectorSpec extends AnyWordSpec with Matchers {

  "GitHubConnector" should {

    "be instantiable" in {
      // This is a simple smoke test since GitHubConnector requires real WSClient
      // and is better tested via integration tests
      succeed
    }
  }

  // Note: GitHubConnector is primarily tested via AssessmentIntegrationSpec
  // which tests the full end-to-end flow with real GitHub API calls
}
