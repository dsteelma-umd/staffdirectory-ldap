package edu.umd.lib.staffdir.drupal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.lib.staffdir.Person;

/**
 * Creates an Excel spreadsheet from a List of Persons
 */
public class DrupalGenerator {
  public static final Logger log = LoggerFactory.getLogger(DrupalGenerator.class);

  private List<Map<String, String>> fieldMappings;

  public DrupalGenerator(List<Map<String, String>> fieldMappings) {
    this.fieldMappings = fieldMappings;
  }

  public void generate(String filename, List<Person> persons) {
    // Header row
    String[] columnTitles = new String[fieldMappings.size()];
    for (int i = 0; i < fieldMappings.size(); i++) {
      columnTitles[i] = fieldMappings.get(i).get("Destination Field");
    }

    // Map columns in destination to fields in the field mappings
    Map<String, Map<String, String>> columnTitlesToSourceFields = new HashMap<>();
    for (String columnTitle : columnTitles) {
      for (Map<String, String> fieldMapping : fieldMappings) {
        if (columnTitle.equals(fieldMapping.get("Destination Field"))) {
          columnTitlesToSourceFields.put(columnTitle, fieldMapping);
        }
      }
    }

    // Data rows
    for (Person p : persons) {
      Map<String, String> rowValues = new HashMap<>();

      for (String columnTitle : columnTitles) {
        Map<String, String> fieldMapping = columnTitlesToSourceFields.get(columnTitle);
        if (fieldMapping != null) {
          String source = fieldMapping.get("Source");
          String sourceField = fieldMapping.get("Source Field");
          String value = p.getAllowNull(source, sourceField);
          if (value != null) {
            String displayValue = getDisplayValue(fieldMapping.get("Display Type"), value);
            rowValues.put(columnTitle, displayValue);
          }
        }
      }

      // Derived Values
      // Title
      String title = p.get("Staff", "Functional Title");
      if (title.isEmpty()) {
        title = p.get("LDAP", "umDisplayTitle");
      }
      String officialTitle = p.get("LDAP", "umOfficialTitle");
      if (officialTitle.startsWith("Librarian") && !officialTitle.equals(title)) {
        title = String.format("%s (%s)", title, officialTitle);
      }
      rowValues.put("Title", title);

      // Display Name
      String displayName = String.format("%s %s",
          p.get("LDAP", "givenName"),
          p.get("LDAP", "sn"));
      rowValues.put("Display Name", displayName);

      // Location
      String location = String.format("%s %s",
          p.get("LDAP", "umPrimaryCampusRoom"),
          p.get("LDAP", "umPrimaryCampusBuilding"));
      rowValues.put("Location", location);

      for (int colIndex = 0; colIndex < columnTitles.length; colIndex++) {
        String columnTitle = columnTitles[colIndex];
        String value = rowValues.get(columnTitle);
        System.out.print(value + ",");
      }
      System.out.println();
    }
  }

  public String getDisplayValue(String displayType, String value) {
    return value;
  }
}
