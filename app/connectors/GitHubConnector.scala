package connectors

import javax.inject.{Inject, Singleton}
import play.api.libs.ws.WSClient
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubConnector @Inject() (wsClient: WSClient)(implicit ec: ExecutionContext) {

  private val baseUrl = "https://api.github.com"

  def getFileTree(owner: String, repo: String, branch: String = "main"): Future[Seq[String]] = {
    val url = s"$baseUrl/repos/$owner/$repo/git/trees/$branch?recursive=1"
    wsClient.url(url).get().map { response =>
      if (response.status == 200) {
        (response.json \ "tree").as[Seq[JsObject]].map { node =>
          (node \ "path").as[String]
        }
      } else {
        throw new RuntimeException(s"Failed to fetch file tree: ${response.status} ${response.body}")
      }
    }
  }

  def getFileContent(owner: String, repo: String, path: String, branch: String = "main"): Future[String] = {
    // Using raw.githubusercontent.com for simpler content fetching without base64 decoding
    // Note: This might be subject to different rate limits or caching than the API
    val url = s"https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
    wsClient.url(url).get().map { response =>
      if (response.status == 200) {
        response.body
      } else {
        throw new RuntimeException(s"Failed to fetch file content for $path: ${response.status} ${response.body}")
      }
    }
  }

  def searchCode(owner: String, repo: String, query: String): Future[Seq[String]] = {
    val url = s"$baseUrl/search/code?q=repo:$owner/$repo+$query"
    wsClient.url(url).get().map { response =>
      if (response.status == 200) {
        (response.json \ "items").as[Seq[JsObject]].map { item =>
          (item \ "path").as[String]
        }
      } else {
        // Return empty on failure (e.g. rate limits) to avoid breaking the flow
        Seq.empty
      }
    }
  }
}
