package services

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.Configuration
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import org.scalatest.concurrent.ScalaFutures
import models.CheckItem

class GeminiServiceSearchTermsSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "GeminiService.generateSearchTerms" should {

    "return search terms from LLM response" in {
      val mockWs       = mock[WSClient]
      val mockConfig   = mock[Configuration]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockConfig.getOptional[String]("pra.assessment.gemini.apiKey")).thenReturn(Some("test-key"))
      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.withHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue])(any())).thenReturn(Future.successful(mockResponse))

      when(mockResponse.status).thenReturn(200)
      val llmResponse = Json.obj(
        "candidates" -> Json.arr(
          Json.obj(
            "content" -> Json.obj(
              "parts" -> Json.arr(
                Json.obj("text" -> """["Upscan", "AuthConnector", "Mongo"]""")
              )
            )
          )
        )
      )
      when(mockResponse.json).thenReturn(llmResponse)

      val service = new GeminiService(mockConfig, mockWs)
      val checks  = Seq(CheckItem("1.A", "Check for Upscan"))

      val result = service.generateSearchTerms("context", "category", checks, "model")

      whenReady(result) { terms =>
        terms must contain theSameElementsAs Seq("Upscan", "AuthConnector", "Mongo")
      }
    }

    "return empty list on parse failure" in {
      val mockWs       = mock[WSClient]
      val mockConfig   = mock[Configuration]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockConfig.getOptional[String]("pra.assessment.gemini.apiKey")).thenReturn(Some("test-key"))
      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.withHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue])(any())).thenReturn(Future.successful(mockResponse))

      when(mockResponse.status).thenReturn(200)
      val llmResponse = Json.obj(
        "candidates" -> Json.arr(
          Json.obj(
            "content" -> Json.obj(
              "parts" -> Json.arr(
                Json.obj("text" -> "invalid json")
              )
            )
          )
        )
      )
      when(mockResponse.json).thenReturn(llmResponse)

      val service = new GeminiService(mockConfig, mockWs)
      val checks  = Seq(CheckItem("1.A", "Check"))

      val result = service.generateSearchTerms("context", "category", checks, "model")

      whenReady(result) { terms =>
        terms mustBe empty
      }
    }
  }
}
