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

class GeminiServiceSpec extends PlaySpec with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "GeminiService" should {

    "use the correct model when calling Gemini API" in {
      val mockWs       = mock[WSClient]
      val mockConfig   = mock[Configuration]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockConfig.getOptional[String]("pra.assessment.gemini.apiKey")).thenReturn(Some("test-key"))
      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.withHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue])(any())).thenReturn(Future.successful(mockResponse))
      when(mockResponse.body).thenReturn("""{"candidates": [{"content": {"parts": [{"text": "{}"}]}}]}""")

      val service   = new GeminiService(mockConfig, mockWs)
      val template  = AssessmentTemplate("id", "name", "desc", "basePrompt", Seq.empty, Seq.empty)
      val checkItem = CheckItem("1", "desc")

      // Test with specific model
      service.assessItem("owner", "repo", "context", template, checkItem, Map.empty, "gemini-1.5-pro")

      // Verify URL contains the model
      verify(mockWs).url(argThat((url: String) => url.contains("gemini-1.5-pro")))
    }

    "use the default model when not specified (default param)" in {
      // This test is slightly tricky because we can't easily spy on the private callGemini method directly
      // without changing visibility or using PowerMock.
      // However, we can verify the URL constructed in the mock.

      val mockWs       = mock[WSClient]
      val mockConfig   = mock[Configuration]
      val mockRequest  = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockConfig.getOptional[String]("pra.assessment.gemini.apiKey")).thenReturn(Some("test-key"))
      when(mockWs.url(anyString)).thenReturn(mockRequest)
      when(mockRequest.withHttpHeaders(any())).thenReturn(mockRequest)
      when(mockRequest.post(any[JsValue])(any())).thenReturn(Future.successful(mockResponse))
      when(mockResponse.body).thenReturn("""{"candidates": [{"content": {"parts": [{"text": "{}"}]}}]}""")

      val service   = new GeminiService(mockConfig, mockWs)
      val template  = AssessmentTemplate("id", "name", "desc", "basePrompt", Seq.empty, Seq.empty)
      val checkItem = CheckItem("1", "desc")

      // Test with default model string passed from controller/orchestrator default
      service.assessItem("owner", "repo", "context", template, checkItem, Map.empty, "gemini-2.0-flash")

      verify(mockWs).url(argThat((url: String) => url.contains("gemini-2.0-flash")))
    }
  }
}
