package services

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.Configuration
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import models.{AssessmentTemplate, CheckItem, ContextResource}

import org.scalatest.concurrent.ScalaFutures

class GeminiServiceParsingSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "GeminiService Parsing" should {

    "handle 'result' field as fallback for 'status'" in {
      val mockWs       = mock[WSClient]
      val mockConfig   = mock[Configuration]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockConfig.getOptional[String]("pra.assessment.gemini.apiKey")).thenReturn(Some("test-key"))
      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.withHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue])(any())).thenReturn(Future.successful(mockResponse))

      // JSON with 'result' instead of 'status'
      val jsonResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "{\"result\": \"PASS\", \"confidence\": 0.9, \"requiresReview\": false, \"reason\": \"Good\", \"evidence\": []}"
                  }
                ]
              }
            }
          ]
        }
      """
      when(mockResponse.body).thenReturn(jsonResponse)
      when(mockResponse.status).thenReturn(200)
      when(mockResponse.json).thenReturn(Json.parse(jsonResponse))

      val service   = new GeminiService(mockConfig, mockWs)
      val template  = AssessmentTemplate("id", "name", "desc", "basePrompt", Seq.empty, Seq.empty)
      val checkItem = CheckItem("1", "desc")

      val result = service.assessItem("owner", "repo", "context", template, checkItem, Map.empty, "model").futureValue

      result.status mustBe models.AssessmentStatus.Pass
    }
  }
}
