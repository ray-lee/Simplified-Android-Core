package org.nypl.simplified.accounts.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken;
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials;
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials;
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountAuthenticationProvider;
import org.nypl.simplified.accounts.api.AccountBarcode;
import org.nypl.simplified.accounts.api.AccountPIN;
import org.nypl.simplified.accounts.api.AccountPatron;
import org.nypl.simplified.http.core.HTTPOAuthToken;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;

/**
 * Functions for serializing/deserializing account credentials.
 */

public final class AccountAuthenticationCredentialsJSON {
  private AccountAuthenticationCredentialsJSON() {
    throw new UnreachableCodeException();
  }

  /**
   * Serialize the given credentials to a JSON object.
   *
   * @param credentials The credentials.
   * @return A JSON object
   */

  public static ObjectNode serializeToJSON(final AccountAuthenticationCredentials credentials) {
    NullCheck.notNull(credentials, "Credentials");

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode jo = jom.createObjectNode();
    jo.put("username", credentials.barcode().value());
    jo.put("password", credentials.pin().value());

    credentials.oAuthToken().map_(
      x -> jo.put("oauth_token", x.value()));

    credentials.adobeCredentials().map_(
      creds -> {
        final ObjectNode adobe_pre_jo = jom.createObjectNode();

        adobe_pre_jo.put("client_token", creds.clientToken().tokenRaw());
        adobe_pre_jo.put("device_manager_uri", creds.deviceManagerURI().toString());
        adobe_pre_jo.put("vendor_id", creds.vendorID().getValue());

        creds.postActivationCredentials().map_(
          post_creds -> {
            final ObjectNode adobe_post_jo = jom.createObjectNode();
            adobe_post_jo.put("device_id", post_creds.deviceID().getValue());
            adobe_post_jo.put("user_id", post_creds.userID().getValue());
            adobe_pre_jo.set("activation", adobe_post_jo);
          });

        jo.set("adobe_credentials", adobe_pre_jo);
      });

    credentials.authenticationProvider().map_(x -> jo.put("auth_provider", x.value()));
    credentials.patron().map_(x -> jo.put("patron", x.value()));
    return jo;
  }

  /**
   * Deserialize the given JSON node, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param node Credentials as a JSON node.
   * @return Account credentials
   * @throws JSONParseException On parse errors
   */

  public static AccountAuthenticationCredentials deserializeFromJSON(
    final JsonNode node)
    throws JSONParseException {
    NullCheck.notNull(node, "Node");

    final ObjectNode obj = JSONParserUtilities.checkObject(null, node);

    final AccountBarcode user =
      AccountBarcode.create(JSONParserUtilities.getString(obj, "username"));
    final AccountPIN pass =
      AccountPIN.create(JSONParserUtilities.getString(obj, "password"));
    final AccountAuthenticationCredentials.Builder builder =
      AccountAuthenticationCredentials.builder(pass, user);

    builder.setPatron(
      JSONParserUtilities.getStringOptional(obj, "patron")
        .map(AccountPatron::create));

    builder.setAuthenticationProvider(
      JSONParserUtilities.getStringOptional(obj, "auth_provider")
        .map(AccountAuthenticationProvider::create));

    builder.setOAuthToken(
      JSONParserUtilities.getStringOptional(obj, "oauth_token")
        .map(HTTPOAuthToken::create));

    builder.setAdobeCredentials(
      JSONParserUtilities.getObjectOptional(obj, "adobe_credentials")
        .mapPartial(jo_creds -> {

          final OptionType<AccountAuthenticationAdobePostActivationCredentials> creds_post =
            JSONParserUtilities.getObjectOptional(jo_creds, "activation")
              .mapPartial(act -> AccountAuthenticationAdobePostActivationCredentials.create(
                new AdobeDeviceID(JSONParserUtilities.getString(act, "device_id")),
                new AdobeUserID(JSONParserUtilities.getString(act, "user_id"))));

          return AccountAuthenticationAdobePreActivationCredentials.create(
            new AdobeVendorID(JSONParserUtilities.getString(jo_creds, "vendor_id")),
            AccountAuthenticationAdobeClientToken.create(JSONParserUtilities.getString(jo_creds, "client_token")),
            JSONParserUtilities.getURI(jo_creds, "device_manager_uri"),
            creds_post);
        }));

    return builder.build();
  }
}