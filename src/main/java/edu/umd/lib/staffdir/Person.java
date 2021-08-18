package edu.umd.lib.staffdir;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates information for a single person.
 */
public class Person {
  public static final Logger log = LoggerFactory.getLogger(Person.class);

  /**
   * The unique identifier of the person associated with this object.
   */
  public final String uid;

  /**
   * The Map of sources for this person.
   */
  public Map<String, Map<String, String>> sources;

  /**
   * Constructs a Person object with the given UID and Map of sources. The
   * sources Map will typically include a "Staff", and "LDAP" derived from the
   * Google sheets document containing information about a single person.
   *
   * @param uid
   *          the unique identifier for the person
   * @param sources
   *          a Map of Map<String, String>, keyed by a source identifier such as
   *          "Staff", or "LDAP".
   */
  @JsonCreator
  public Person(@JsonProperty("uid") String uid,
      @JsonProperty("sources") Map<String, Map<String, String>> sources) {
    if (uid == null) {
      throw new IllegalArgumentException("uid is null.");
    }

    if (sources == null) {
      throw new IllegalArgumentException("sources is null.");
    }

    this.uid = uid;
    this.sources = sources;
  }

  /**
   * Returns the value from the given source and field, or an empty String.
   *
   * @param source
   *          the key of the source Map to retrieve from the "sources" Map
   * @param field
   *          the key for the field to retrieve from the source Map
   * @return the value from the given source and field, or an empty String.
   */
  public String get(String source, String field) {
    Map<String, String> src = sources.get(source);
    if (src == null) {
      log.warn("WARNING: uid: '{}' - Source '{}' is null. Returning empty string.", uid, source);
      return "";
    }

    if (src.containsKey(field)) {
      String value = src.get(field);
      if (value == null) {
        log.warn("WARNING: uid: '{}' - Value for field '{}' in source '{}' is null. Returning empty string.", uid,
            field, source);
        return "";
      }
      return value;

    } else {
      log.warn("WARNING: uid: '{}' - Field '{}' not found in source '{}'. Returning empty string.", uid, field, source);
      return "";
    }

  }

  public String getAllowNull(String source, String field) {
    Map<String, String> src = sources.get(source);
    if (src == null) {
      log.warn("WARNING: uid: '{}' - Source '{}' is null. Returning null.", uid, source);
      return null;
    }

    return src.getOrDefault(field, null);
  }

  @Override
  public String toString() {
    String str = String.format("Person@%s[uid: %s]",
        Integer.toHexString(System.identityHashCode(this)),
        uid);
    return str;
  }
}
