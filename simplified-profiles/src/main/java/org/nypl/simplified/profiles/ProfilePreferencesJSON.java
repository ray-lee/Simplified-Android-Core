package org.nypl.simplified.profiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;
import org.nypl.simplified.profiles.api.ProfileDateOfBirth;
import org.nypl.simplified.profiles.api.ProfilePreferences;
import org.nypl.simplified.reader.api.ReaderPreferences;
import org.nypl.simplified.reader.api.ReaderPreferencesJSON;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Functions to serialize and deserialize profile preferences to/from JSON.
 */

public final class ProfilePreferencesJSON {

  private ProfilePreferencesJSON() {
    throw new UnreachableCodeException();
  }

  /**
   * Deserialize profile preferences from the given file.
   *
   * @param jom  A JSON object mapper
   * @param file A file
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  public static ProfilePreferences deserializeFromFile(
    final ObjectMapper jom,
    final File file)
    throws IOException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(file, "File");
    return deserializeFromText(jom, FileUtilities.fileReadUTF8(file));
  }

  /**
   * Deserialize profile preferences from the given text.
   *
   * @param jom  A JSON object mapper
   * @param text A JSON string
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  public static ProfilePreferences deserializeFromText(
    final ObjectMapper jom,
    final String text) throws IOException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(text, "Text");
    return deserializeFromJSON(jom, jom.readTree(text));
  }

  /**
   * Deserialize profile preferences from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  public static ProfilePreferences deserializeFromJSON(
    final ObjectMapper jom,
    final JsonNode node)
    throws JSONParseException {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(node, "JSON");

    final DateTimeFormatter date_formatter = standardDateFormatter();

    final ObjectNode obj =
      JSONParserUtilities.checkObject(null, node);

    final OptionType<String> gender =
      JSONParserUtilities.getStringOptional(obj, "gender");

    final boolean showTestingLibraries =
      JSONParserUtilities.getBooleanDefault(obj, "show-testing-libraries", false);

    final OptionType<LocalDate> date_of_birth_date =
      JSONParserUtilities.getStringOptional(obj, "date-of-birth")
        .mapPartial(text -> {
          try {
            return LocalDate.parse(text, date_formatter);
          } catch (final IllegalArgumentException e) {
            throw new JSONParseException(e);
          }
        });

    final boolean date_of_birth_synthesized =
      JSONParserUtilities.getBooleanDefault(obj, "date-of-birth-synthesized", false);

    final OptionType<ProfileDateOfBirth> date_of_birth =
      date_of_birth_date.map(
        date_value -> new ProfileDateOfBirth(date_value, date_of_birth_synthesized));

    final ReaderPreferences reader_prefs =
      JSONParserUtilities.getObjectOptional(obj, "reader-preferences")
        .mapPartial(prefs_node -> ReaderPreferencesJSON.deserializeFromJSON(jom, prefs_node))
        .accept(new OptionVisitorType<ReaderPreferences, ReaderPreferences>() {
          @Override
          public ReaderPreferences none(final None<ReaderPreferences> none) {
            return ReaderPreferences.builder().build();
          }

          @Override
          public ReaderPreferences some(final Some<ReaderPreferences> some) {
            return some.get();
          }
        });

    return ProfilePreferences.builder()
      .setGender(gender)
      .setDateOfBirth(date_of_birth)
      .setShowTestingLibraries(showTestingLibraries)
      .setReaderPreferences(reader_prefs)
      .build();
  }

  private static DateTimeFormatter standardDateFormatter() {
    return new DateTimeFormatterBuilder()
      .appendYear(4, 5)
      .appendLiteral("-")
      .appendMonthOfYear(2)
      .appendLiteral("-")
      .appendDayOfMonth(2)
      .toFormatter();
  }

  /**
   * Serialize profile preferences to JSON.
   *
   * @param jom A JSON object mapper
   * @return A serialized object
   */

  public static ObjectNode serializeToJSON(
    final ObjectMapper jom,
    final ProfilePreferences description) {

    NullCheck.notNull(jom, "Object mapper");
    NullCheck.notNull(description, "Description");

    final DateTimeFormatter date_formatter = standardDateFormatter();
    final ObjectNode jo = jom.createObjectNode();

    description.gender().map_(
      gender -> jo.put("gender", gender));

    description.dateOfBirth().map_(
      date -> {
        jo.put("date-of-birth", date_formatter.print(date.getDate()));
        jo.put("date-of-birth-synthesized", date.isSynthesized());
      });

    jo.put("show-testing-libraries", description.showTestingLibraries());
    jo.set("reader-preferences", ReaderPreferencesJSON.serializeToJSON(jom, description.readerPreferences()));
    return jo;
  }

  /**
   * Serialize profile preferences to a JSON string.
   *
   * @param jom A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  public static String serializeToString(
    final ObjectMapper jom,
    final ProfilePreferences description)
    throws IOException {

    final ObjectNode jo = serializeToJSON(jom, description);
    final ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
    JSONSerializerUtilities.serialize(jo, bao);
    return bao.toString("UTF-8");
  }
}
