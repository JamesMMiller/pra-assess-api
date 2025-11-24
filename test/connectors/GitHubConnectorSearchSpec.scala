package connectors

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import org.scalatest.concurrent.ScalaFutures

class GitHubConnectorSearchSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "GitHubConnector.searchCode" should {

    "return a list of file paths when search is successful" in {
      val mockWs       = mock[WSClient]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.get()).thenReturn(Future.successful(mockResponse))

      when(mockResponse.status).thenReturn(200)
      val searchResult = Json.obj(
        "items" -> Json.arr(
          Json.obj("path" -> "app/connectors/AuthConnector.scala"),
          Json.obj("path" -> "app/controllers/AuthController.scala")
        )
      )
      when(mockResponse.json).thenReturn(searchResult)

      val connector = new GitHubConnector(mockWs)
      val result    = connector.searchCode("owner", "repo", "AuthConnector")

      whenReady(result) { paths =>
        paths must contain theSameElementsAs Seq(
          "app/connectors/AuthConnector.scala",
          "app/controllers/AuthController.scala"
        )
      }
    }

    "return empty list on failure (e.g. rate limit)" in {
      val mockWs       = mock[WSClient]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.get()).thenReturn(Future.successful(mockResponse))

      when(mockResponse.status).thenReturn(403) // Rate limit exceeded

      val connector = new GitHubConnector(mockWs)
      val result    = connector.searchCode("owner", "repo", "AuthConnector")

      whenReady(result) { paths =>
        paths mustBe empty
      }
    }
  }
}
