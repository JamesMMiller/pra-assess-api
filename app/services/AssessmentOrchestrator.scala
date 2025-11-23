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
        // 3. Stream Check Items
        Source(models.CheckItem.items).mapAsync(parallelism = 2) { checkItem =>
          for {
            // a. Select Files
            relevantFiles <- geminiService.selectFiles(sharedContext, checkItem.description, fileTree)
            // b. Fetch Content
            fileContents <- Future
              .sequence(relevantFiles.map { path =>
                gitHubConnector.getFileContent(owner, repo, path).map(content => path -> content).recover { case _ =>
                  path -> "Error fetching file"
                }
              })
              .map(_.toMap)
            // c. Assess
            result <- geminiService.assessItem(owner, repo, sharedContext, checkItem, fileContents)
          } yield result
        }
      }
    }
  }
}
