package models

class GeminiException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

case class GeminiRateLimitException(message: String, retryAfter: Option[String] = None) extends GeminiException(message)
