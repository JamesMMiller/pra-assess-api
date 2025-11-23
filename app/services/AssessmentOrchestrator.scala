package services

import javax.inject.{Inject, Singleton}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.NotUsed
import scala.concurrent.{ExecutionContext, Future}
import connectors.GitHubConnector
import models.AssessmentResult

@Singleton
class AssessmentOrchestrator @Inject() (
    gitHubConnector: GitHubConnector,
    geminiService: GeminiService
)(implicit ec: ExecutionContext) {

  def runAssessment(owner: String, repo: String): Source[AssessmentResult, NotUsed] = {
    // 1. Fetch File Tree
    val fileTreeFuture = gitHubConnector.getFileTree(owner, repo)

    Source.future(fileTreeFuture).flatMapConcat { fileTree =>
      // 2. Generate Shared Context
      Source.future(geminiService.generateSharedContext(fileTree)).flatMapConcat { sharedContext =>

        // 3. Stream Items (Hardcoded for prototype)
        val items = List(
          "1.A - Does your service implement any non-standard patterns, or contradict any of the MDTP Opinions?",
          "1.C - Your service should not be using any deprecated HMRC Libraries.",
          "2.A - Are you using Mongo for JSON data persistence?",
          "4.B - Public Microservices should be authenticated and authorised by default."
        )

        Source(items).mapAsync(parallelism = 2) { item =>
          for {
            // a. Select Files
            relevantFiles <- geminiService.selectFiles(sharedContext, item, fileTree)
            // b. Fetch Content
            fileContents <- Future
              .sequence(relevantFiles.map { path =>
                gitHubConnector.getFileContent(owner, repo, path).map(content => path -> content).recover { case _ =>
                  path -> "Error fetching file"
                }
              })
              .map(_.toMap)
            // c. Assess
            result <- geminiService.assessItem(sharedContext, item, fileContents)
          } yield result
        }
      }
    }
  }
}
