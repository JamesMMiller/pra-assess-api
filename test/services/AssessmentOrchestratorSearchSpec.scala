package services

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import scala.concurrent.{ExecutionContext, Future}
import org.scalatest.concurrent.ScalaFutures
import connectors.GitHubConnector
import models.{AssessmentResult, AssessmentStatus, AssessmentTemplate, CheckItem}

class AssessmentOrchestratorSearchSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "AssessmentOrchestrator.runBatchAssessment" should {

    "perform search and pass results to GeminiService" in {
      val mockGitHubConnector = mock[GitHubConnector]
      val mockGeminiService   = mock[GeminiService]
      val orchestrator        = new AssessmentOrchestrator(mockGitHubConnector, mockGeminiService)

      val checkWithSearch = CheckItem("1.A", "Check with search", Some(Seq("term1")))
      val template        = AssessmentTemplate("id", "name", "desc", "prompt", Seq.empty, Seq(checkWithSearch))

      // Mock GitHub calls
      when(mockGitHubConnector.getFileTree(any(), any(), any())).thenReturn(Future.successful(Seq("file1")))
      when(mockGitHubConnector.searchCode(any(), any(), ArgumentMatchers.eq("term1")))
        .thenReturn(Future.successful(Seq("foundFile.scala")))
      when(mockGitHubConnector.getFileContent(any(), any(), any(), any())).thenReturn(Future.successful("content"))

      // Mock Gemini calls
      when(mockGeminiService.generateSharedContext(any(), any(), any())).thenReturn(Future.successful("context"))

      // Verify selectFilesForCategory receives the search result
      when(mockGeminiService.selectFilesForCategory(any(), any(), any(), any(), any[Seq[String]]()))
        .thenReturn(Future.successful(Seq("foundFile.scala")))

      when(mockGeminiService.assessBatch(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
          Future.successful(
            Seq(AssessmentResult("1.A", "desc", AssessmentStatus.Pass, 1.0, false, "reason", Seq.empty))
          )
        )

      val result = orchestrator.runBatchAssessment("owner", "repo", template, "model")

      whenReady(result) { results =>
        results.length mustBe 1
        verify(mockGitHubConnector).searchCode(anyString(), anyString(), ArgumentMatchers.eq("term1"))
      }
    }
  }
}
