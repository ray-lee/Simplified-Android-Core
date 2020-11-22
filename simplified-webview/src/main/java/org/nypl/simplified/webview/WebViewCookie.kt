package org.nypl.simplified.webview

import java.text.SimpleDateFormat
import java.util.TimeZone

/**
 * A cookie extracted from the WebViewUtilities cookie database.
 */
data class WebViewCookie(
  val hostKey: String,
  val name: String,
  val value: String,
  val path: String,
  val expiresUTC: Long,
  val isSecure: Int,
  val isHttpOnly: Int,
  val firstPartyOnly: Int
) {

  companion object {
    private val dateFormatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")

    init {
      dateFormatter.timeZone = TimeZone.getTimeZone("GMT")
    }

    /**
     * Convert a webkit timestamp (microseconds since 1 Jan 1601) to a unix timestamp (milliseconds
     * since 1 Jan 1970).
     */
    private fun webkitTimeToUnixTime(
      time: Long
    ): Long {
      return (time / 1000L - 11644473600000L)
    }

    fun formatTimestamp(
      timestamp: Long
    ): String {
      return dateFormatter.format(
        webkitTimeToUnixTime(timestamp)
      )
    }
  }

  fun toSetCookieString(): String {
    val pairs = mutableListOf<List<String>>()

    pairs.add(listOf(this.name, this.value))
    pairs.add(listOf("Domain", this.hostKey))
    pairs.add(listOf("Path", this.path))

    if (this.expiresUTC > 0) {
      pairs.add(listOf("Expires", formatTimestamp(this.expiresUTC)))
    }

    if (this.isSecure > 0) {
      pairs.add(listOf("Secure"))
    }

    if (this.isHttpOnly > 0) {
      pairs.add(listOf("HttpOnly"))
    }

    if (this.firstPartyOnly > 0) {
      pairs.add(listOf("SameSite", "Strict"))
    }

    return pairs.map({ pair ->
      pair.joinToString("=")
    }).joinToString("; ")
  }
}
