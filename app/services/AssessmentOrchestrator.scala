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

  def runAssessment(
      owner: String,
      repo: String,
      template: AssessmentTemplate,
      model: String
  ): Source[AssessmentResult, NotUsed] = {
    // 1. Fetch File Tree
    val fileTreeFuture = gitHubConnector.getFileTree(owner, repo)

    Source.future(fileTreeFuture).flatMapConcat { fileTree =>
      // 2. Generate Shared Context (with template context resources)
      Source.future(geminiService.generateSharedContext(fileTree, template, model)).flatMapConcat { sharedContext =>
        // 3. Stream Check Items from template
        Source(template.checks).mapAsync(parallelism = 2) { checkItem =>
          for {
            // a. Select Files
            relevantFiles <- geminiService.selectFiles(sharedContext, checkItem.description, fileTree, model)
            // b. Fetch Content
            fileContents <- Future
              .sequence(relevantFiles.map { path =>
                gitHubConnector.getFileContent(owner, repo, path).map(content => path -> content).recover { case _ =>
                  path -> "Error fetching file"
                }
              })
              .map(_.toMap)
            // c. Assess
            result <- geminiService.assessItem(owner, repo, sharedContext, template, checkItem, fileContents, model)
          } yield result
        }
      }
    }
  }

  def runBatchAssessment(
      owner: String,
      repo: String,
      template: AssessmentTemplate,
      model: String
  ): Future[Seq[AssessmentResult]] = {
    // 1. Fetch File Tree
    gitHubConnector.getFileTree(owner, repo).flatMap { fileTree =>
      // 2. Generate Shared Context
      geminiService.generateSharedContext(fileTree, template, model).flatMap { sharedContext =>
        // 3. Group Checks by Category (Prefix)
        // e.g. "1.A", "1.B" -> "1"
        val categories = template.checks.groupBy(c => c.id.split("\\.").headOption.getOrElse("Other"))

        // 4. Process Categories
        Future
          .sequence(categories.map { case (categoryId, checks) =>
            for {
              // a. Generate Search Terms Dynamically & Perform Search
              searchTerms <- geminiService.generateSearchTerms(
                sharedContext,
                s"Checks for category $categoryId",
                checks,
                model
              )
              searchResults <-
                if (searchTerms.nonEmpty) {
                  Future
                    .sequence(searchTerms.map(term => gitHubConnector.searchCode(owner, repo, term)))
                    .map(_.flatten.distinct)
                } else {
                  Future.successful(Seq.empty[String])
                }

              // b. Select Files for Category
              // We use a generic description for the category based on the first check or just the ID
              relevantFiles <- geminiService.selectFilesForCategory(
                sharedContext,
                s"Checks for category $categoryId",
                fileTree,
                model,
                searchResults
              )
              // b. Fetch Content
              fileContents <- Future
                .sequence(relevantFiles.map { path =>
                  gitHubConnector.getFileContent(owner, repo, path).map(content => path -> content).recover { case _ =>
                    path -> "Error fetching file"
                  }
                })
                .map(_.toMap)
              // c. Assess Batch
              results <- geminiService.assessBatch(owner, repo, sharedContext, template, checks, fileContents, model)
            } yield results
          })
          .map(_.flatten.toSeq)
      }
    }
  }
}
