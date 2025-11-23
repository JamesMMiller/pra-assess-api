package services

import javax.inject.{Inject, Singleton}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.NotUsed
import scala.concurrent.{ExecutionContext, Future}
import connectors.GitHubConnector
import models.{AssessmentResult, AssessmentTemplate}

@Singleton
class AssessmentOrchestrator @Inject() (
    gitHubConnector: GitHubConnector,
    geminiService: GeminiService
)(implicit ec: ExecutionContext) {

  def runAssessment(owner: String, repo: String, template: AssessmentTemplate): Source[AssessmentResult, NotUsed] = {
    // 1. Fetch File Tree
    val fileTreeFuture = gitHubConnector.getFileTree(owner, repo)

    Source.future(fileTreeFuture).flatMapConcat { fileTree =>
      // 2. Generate Shared Context (with template context resources)
      Source.future(geminiService.generateSharedContext(fileTree, template)).flatMapConcat { sharedContext =>
        // 3. Stream Check Items from template
        Source(template.checks).mapAsync(parallelism = 2) { checkItem =>
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
            result <- geminiService.assessItem(owner, repo, sharedContext, template, checkItem, fileContents)
          } yield result
        }
      }
    }
  }
}
