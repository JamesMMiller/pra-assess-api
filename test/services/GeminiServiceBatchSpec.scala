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

class GeminiServiceBatchSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "GeminiService Batch Assessment" should {

    "assess a batch of checks" in {
      val mockWs       = mock[WSClient]
      val mockConfig   = mock[Configuration]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockConfig.getOptional[String]("pra.assessment.gemini.apiKey")).thenReturn(Some("test-key"))
      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.withHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue])(any())).thenReturn(Future.successful(mockResponse))

      val jsonResponse = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "[{\"checkId\": \"1\", \"status\": \"PASS\", \"confidence\": 1.0, \"requiresReview\": false, \"reason\": \"Good\", \"evidence\": []}, {\"checkId\": \"2\", \"status\": \"FAIL\", \"confidence\": 0.9, \"requiresReview\": true, \"reason\": \"Bad\", \"evidence\": []}]"
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

      val service  = new GeminiService(mockConfig, mockWs)
      val template = AssessmentTemplate("id", "name", "desc", "basePrompt", Seq.empty, Seq.empty)
      val checks   = Seq(CheckItem("1", "Check 1"), CheckItem("2", "Check 2"))

      val results = service.assessBatch("owner", "repo", "context", template, checks, Map.empty, "model").futureValue

      results.length mustBe 2
      results.find(_.checkId == "1").get.status mustBe models.AssessmentStatus.Pass
      results.find(_.checkId == "2").get.status mustBe models.AssessmentStatus.Fail
    }
  }
}
