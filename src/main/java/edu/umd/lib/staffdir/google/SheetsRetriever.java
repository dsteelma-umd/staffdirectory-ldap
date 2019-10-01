package edu.umd.lib.staffdir.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetsRetriever {
  public static final Logger log = LoggerFactory.getLogger(SheetsRetriever.class);

  private final String appName;
  private final String clientSecretFile;

  public SheetsRetriever(String appName, String clientSecretFile) {
    this.appName = appName;
    this.clientSecretFile = clientSecretFile;
  }

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT
   *          The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException
   *           If the credentials.json file cannot be found.
   */
  private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    GoogleCredential credential = GoogleCredential.fromStream(
        new FileInputStream(this.clientSecretFile))
        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
    return credential;
  }

  /**
   * Returns a ValueRange comprising all the cells in the given sheet of the
   * spreadsheet document.
   *
   * @param spreadsheetDocId
   *          the document id (from the URL) of the document
   * @param sheetName
   *          the name of the sheet within the document
   * @return a ValueRange comprising all the cells in the given sheet of the
   *         spreadsheet document.
   */
  private ValueRange getSpreadsheetCells(String spreadsheetDocId, String sheetName) {
    try {
      // Build a new authorized API client service.
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      final String spreadsheetId = "1XCKyrL_fooOnDmcjq7WVo4zLfm5zWrkFbboyr_1Z1bY";
      JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      Credential credentials = getCredentials(HTTP_TRANSPORT);
      Sheets service = new Sheets.Builder(HTTP_TRANSPORT, jsonFactory, credentials)
          .setApplicationName(this.appName)
          .build();

      final String range = sheetName;
      ValueRange response = service.spreadsheets().values()
          .get(spreadsheetId, range)
          .execute();

      return response;
    } catch (GeneralSecurityException | IOException e) {
      System.out.println(e);
    }
    return null;
  }

  /**
   * Returns a List containing a Map for each row in the given sheet of the
   * spreadsheet document.
   * <p>
   * This method assumes that the headers, used as the keys for the Map, are in
   * the first row of the sheet.
   *
   * @param spreadsheetDocId
   *          the document id (from the URL) of the document
   * @param sheetName
   *          the name of the sheet within the document
   * @return a List containing a Map for each row in the given sheet of the
   *         spreadsheet document. The map uses the first row as the keys for
   *         the map.
   */
  public List<Map<String, String>> toMap(String spreadsheetDocId, String sheetName) {
    ValueRange valueRange = getSpreadsheetCells(spreadsheetDocId, sheetName);
    return toMap(valueRange);
  }

  /**
   * Returns a List containing a Map for each row in the given ValueRange
   * <p>
   * This method assumes that the headers, used as the keys for the Map, are in
   * the first row of the ValueRange.
   *
   * @param valueRange
   *          the ValueRange containing the spreadsheet cells.
   * @return a List containing a Map for each row in the ValueRange. The map
   *         uses the first row as the keys for the map.
   */
  public List<Map<String, String>> toMap(ValueRange valueRange) {
    List<Map<String, String>> results = new ArrayList<>();

    if (valueRange == null) {
      return results;
    }

    List<List<Object>> values = valueRange.getValues();
    if (values == null || values.isEmpty()) {
      return results;
    }

    List<String> headerRow = new ArrayList<>();
    for (List<Object> row : values) {
      List<String> rowValues = new ArrayList<>();
      for (Object col : row) {
        rowValues.add((String) col);
        log.debug((String) col);
      }
      if (headerRow.isEmpty()) {
        headerRow.addAll(rowValues);
        log.debug("\n");
        continue;
      }
      Map<String, String> rowMap = new HashMap<>();
      for (int i = 0; i < rowValues.size(); i++) {
        rowMap.put(headerRow.get(i), rowValues.get(i));
      }
      results.add(rowMap);
      log.debug("\n");
    }
    return results;
  }

  public static void main(String[] args) {
    SheetsRetriever sr = new SheetsRetriever("staffdirectory-ldap", "/tmp/service_account.json");
    List<Map<String, String>> results = sr.toMap("1-JIxCZVu759FzyqCmJudewaSNKQeap3KGQxd1kMiw5o", "Staff");
    Map<String, String> r = results.get(0);
    for (String key : r.keySet()) {
      System.out.println(key + ": '" + r.get(key) + "'");
    }
  }
}
