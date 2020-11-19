package org.nypl.simplified.webview

import android.database.sqlite.SQLiteDatabase
import java.io.File

class WebViewCookieDatabase(
  val dataDir: File
) : WebViewCookieDatabaseType {

  companion object {
    const val DB_FILE_NAME = "Cookies"
    const val DB_TABLE_NAME = "cookies"
  }

  var db: SQLiteDatabase

  init {
    val cookiesFile = File(this.dataDir, DB_FILE_NAME)

    db = SQLiteDatabase.openDatabase(
      cookiesFile.absolutePath,
      null,
      SQLiteDatabase.OPEN_READONLY
    )
  }

  override fun getAll(): List<WebViewCookie> {
    val result = mutableListOf<WebViewCookie>()

    val columns = arrayOf(
      "host_key",
      "name",
      "value",
      "path",
      "expires_utc",
      "is_secure",
      "is_httponly",
      "firstpartyonly"
    )

    db.query(
      DB_TABLE_NAME,
      columns,
      null,
      null,
      null,
      null,
      null
    ).use { cursor ->
      while (cursor.moveToNext()) {
        result.add(
          WebViewCookie(
            hostKey = cursor.getString(0),
            name = cursor.getString(1),
            value = cursor.getString(2),
            path = cursor.getString(3),
            expiresUTC = cursor.getLong(4),
            isSecure = cursor.getInt(5),
            isHttpOnly = cursor.getInt(6),
            firstPartyOnly = cursor.getInt(7)
          )
        )
      }
    }

    return result
  }

  override fun close() {
    db.close()
  }
}
