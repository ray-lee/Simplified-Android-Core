package org.nypl.simplified.tests.books.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.nypl.simplified.json.core.JSONParseException
import org.slf4j.Logger
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

abstract class AccountAuthenticationCredentialsStoreContract {

  protected abstract val logger: Logger

  private lateinit var fileTemp: File
  private lateinit var file: File

  @JvmField
  @Rule
  val expectedException = ExpectedException.none()

  @Before
  fun testSetup() {
    this.file =
      File.createTempFile("test-simplified-auth-credentials-store", ".json")
    this.fileTemp =
      File(file.toString() + ".tmp")

    this.logger.debug("file:     {}", this.file)
    this.logger.debug("fileTemp: {}", this.fileTemp)
  }

  @Test
  fun testLoadEmpty() {
    val store =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assert.assertEquals(0, store.size())
  }

  @Test
  fun testLoadAfterSave() {
    val store0 =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    val accountID = org.nypl.simplified.accounts.api.AccountID.generate()
    val credentials =
      org.nypl.simplified.accounts.api.AccountAuthenticationCredentials.builder(
        org.nypl.simplified.accounts.api.AccountPIN.create("abcd"),
        org.nypl.simplified.accounts.api.AccountBarcode.create("1234"))
        .build()

    store0.put(accountID, credentials)
    Assert.assertEquals(credentials, store0.get(accountID))

    val store1 =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assert.assertEquals(credentials, store1.get(accountID))
  }

  @Test
  fun testLoadNoVersion() {
    val mapper = ObjectMapper()
    val obj = mapper.createObjectNode()
    obj.set("credentials", mapper.createObjectNode())

    FileOutputStream(this.file)
      .use { stream ->
        stream.write(mapper.writeValueAsBytes(obj))
        stream.flush()
      }

    val store =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assert.assertEquals(0, store.size())
  }

  @Test
  fun testLoadUnsupportedVersion() {
    val mapper = ObjectMapper()
    val obj = mapper.createObjectNode()
    obj.put("@version", 20000101)
    obj.set("credentials", mapper.createObjectNode())

    FileOutputStream(this.file)
      .use { stream ->
        stream.write(mapper.writeValueAsBytes(obj))
        stream.flush()
      }

    this.expectedException.expect(JSONParseException::class.java)
    org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)
  }

  @Test
  fun testLoadCorruptedCredentials() {
    val mapper = ObjectMapper()
    val obj = mapper.createObjectNode()
    val creds = mapper.createObjectNode()
    obj.set("credentials", creds)

    /*
     * Invalid credential value.
     */

    val cred0 = mapper.createObjectNode()
    creds.set("347ae11b-cb5c-4084-8954-8629fd971bda", cred0)

    /*
     * Invalid credential value.
     */

    val cred1 =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsJSON.serializeToJSON(
        org.nypl.simplified.accounts.api.AccountAuthenticationCredentials.builder(
          org.nypl.simplified.accounts.api.AccountPIN.create("abcd"),
          org.nypl.simplified.accounts.api.AccountBarcode.create("1234"))
          .build())
    cred1.remove("username")
    creds.set("8e058c17-6c59-490c-92c5-d950463c8632", cred1)

    /*
     * Valid credential value but invalid UUID.
     */

    val cred2 =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsJSON.serializeToJSON(
        org.nypl.simplified.accounts.api.AccountAuthenticationCredentials.builder(
          org.nypl.simplified.accounts.api.AccountPIN.create("abcd"),
          org.nypl.simplified.accounts.api.AccountBarcode.create("1234"))
          .build())
    creds.set("not a uuid", cred2)

    /*
     * Valid credential values.
     */

    val cred3 =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsJSON.serializeToJSON(
        org.nypl.simplified.accounts.api.AccountAuthenticationCredentials.builder(
          org.nypl.simplified.accounts.api.AccountPIN.create("abcd"),
          org.nypl.simplified.accounts.api.AccountBarcode.create("1234"))
          .build())
    creds.set("37452e48-2235-4098-ad67-e72bce45ccb6", cred3)

    FileOutputStream(this.file)
      .use { stream ->
        stream.write(mapper.writeValueAsBytes(obj))
        stream.flush()
      }

    val store =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assert.assertEquals(1, store.size())
    Assert.assertNotNull(store.get(org.nypl.simplified.accounts.api.AccountID(UUID.fromString("37452e48-2235-4098-ad67-e72bce45ccb6"))))
  }

  @Test
  fun testPutRemove() {
    val store =
      org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    val accountID = org.nypl.simplified.accounts.api.AccountID.generate()
    val credentials =
      org.nypl.simplified.accounts.api.AccountAuthenticationCredentials.builder(
        org.nypl.simplified.accounts.api.AccountPIN.create("abcd"),
        org.nypl.simplified.accounts.api.AccountBarcode.create("1234"))
        .build()

    store.put(accountID, credentials)
    Assert.assertEquals(credentials, store.get(accountID))
    Assert.assertEquals(1, store.size())
    store.delete(accountID)
    Assert.assertEquals(null, store.get(accountID))
    Assert.assertEquals(0, store.size())
  }
}