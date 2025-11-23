package utils

object GitHubUrlHelper {

  /** Generates a GitHub permalink URL for a file and line range
    * @param owner
    *   Repository owner
    * @param repo
    *   Repository name
    * @param branch
    *   Branch name (default: main)
    * @param filePath
    *   Path to file from repository root
    * @param lineStart
    *   Starting line number (optional)
    * @param lineEnd
    *   Ending line number (optional)
    * @return
    *   GitHub permalink URL
    */
  def generatePermalink(
      owner: String,
      repo: String,
      filePath: String,
      branch: String = "main",
      lineStart: Option[Int] = None,
      lineEnd: Option[Int] = None
  ): String = {
    val baseUrl = s"https://github.com/$owner/$repo/blob/$branch/$filePath"

    (lineStart, lineEnd) match {
      case (Some(start), Some(end)) if start == end => s"$baseUrl#L$start"
      case (Some(start), Some(end))                 => s"$baseUrl#L$start-L$end"
      case (Some(start), None)                      => s"$baseUrl#L$start"
      case _                                        => baseUrl
    }
  }
}
