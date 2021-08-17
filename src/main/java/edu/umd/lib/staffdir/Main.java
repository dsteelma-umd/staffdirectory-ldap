package edu.umd.lib.staffdir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.lib.staffdir.drupal.DrupalGenerator;
import edu.umd.lib.staffdir.excel.ExcelGenerator;
import edu.umd.lib.staffdir.google.Organizations;
import edu.umd.lib.staffdir.google.SheetsRetriever;
import edu.umd.lib.staffdir.ldap.Ldap;

/**
 * Application entrypoint for generating an Excel spreadsheet from LDAP
 */
public class Main {
  public static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Options options = getOptions();
    CommandLineParser cmdLineParser = new DefaultParser();

    CommandLine cmdLine = null;
    try {
      cmdLine = cmdLineParser.parse(options, args);
    } catch (ParseException pe) {
      printHelp(options);
      System.exit(1);
    }

    if (cmdLine == null) {
      printHelp(options);
      System.exit(1);
    }

    if (cmdLine.hasOption("help")) {
      printHelp(options);
      System.exit(0);
    }

    String propFilename = cmdLine.getOptionValue("config");
    String outputFilename = cmdLine.getOptionValue("output");

    File propFile = new File(propFilename);

    if (!propFile.exists()) {
      System.err.println("ERROR: Properties file '" + propFilename + "' cannot be found or accessed.");
      System.exit(1);
    }

    Properties props = null;
    try {
      FileInputStream propFileIn = new FileInputStream(propFile);
      props = new Properties(System.getProperties());
      props.load(propFileIn);
    } catch (IOException ioe) {
      System.out.println(ioe);
      System.exit(1);
    }

    // Google configuration Settings
    String appName = props.getProperty("appName");
    String serviceAccountCredentialsFile = props.getProperty("serviceAccountCredentialsFile");
    String spreadsheetDocId = props.getProperty("spreadsheetDocId");
    SheetsRetriever sr = new SheetsRetriever(appName, serviceAccountCredentialsFile);

    List<Map<String, String>> rawOrganizationsList = sr.toMap(spreadsheetDocId, "Organization");
    List<Map<String, String>> rawStaffMap = sr.toMap(spreadsheetDocId, "Staff");

    String costCenterField = "Cost Center";
    String uidField = "Directory ID";
    Map<String, Map<String, String>> organizationsMap = Organizations.getCostCentersMap(rawOrganizationsList,
        costCenterField);
    Map<String, Map<String, String>> staffMap = createStaffMap(rawStaffMap, uidField);

    Set<String> uids = staffMap.keySet();

    // LDAP configuration settings
    String ldapUrl = props.getProperty("ldap.url");
    String authentication = props.getProperty("ldap.authentication");
    String bindDn = props.getProperty("ldap.bindDn");
    String credentials = props.getProperty("ldap.credentials");
    String searchBaseDn = props.getProperty("ldap.searchBaseDn");

    Ldap ldap = new Ldap(ldapUrl, authentication, bindDn, credentials, searchBaseDn);
    Map<String, Map<String, String>> ldapResults = ldap.getUsers(uids);

    List<Person> persons = new ArrayList<>();
    for (String uid : uids) {
      Map<String, String> staffEntry = staffMap.get(uid);
      Map<String, String> ldapEntry = ldapResults.get(uid);
      String costCenter = staffEntry.get("Cost Center");
      Map<String, String> organization = organizationsMap.get(costCenter);
      Map<String, Map<String, String>> sources = new HashMap<>();
      sources.put("Staff", staffEntry);
      sources.put("Organization", organization);
      sources.put("LDAP", ldapEntry);

      if (ldapEntry != null) {
        persons.add(new Person(uid, sources));
      } else {
        // TODO figure out how to handle errors
        System.err.println("---Skipping " + uid + ". Could not find in LDAP!");
      }
    }

    // Sort the persons by last name
    Collections.sort(persons,
        (Person p1, Person p2) -> p1.get("LDAP", "sn").toLowerCase().compareTo(p2.get("LDAP", "sn").toLowerCase()));

    List<Map<String, String>> allStaffListMapping = sr.toMap(spreadsheetDocId, "All Staff List Mapping");
    List<Map<String, String>> categoryStatusAbbreviations = sr.toMap(spreadsheetDocId, "CategoryStatus");
    ExcelGenerator excelGenerator = new ExcelGenerator(allStaffListMapping, categoryStatusAbbreviations);
    excelGenerator.generate(outputFilename, persons, "abcd");

    List<Map<String, String>> drupalMapping = sr.toMap(spreadsheetDocId, "Drupal Mapping");
    DrupalGenerator drupalGenerator = new DrupalGenerator(drupalMapping);
    drupalGenerator.generate("foo.txt", persons);
  }

  public static Map<String, Map<String, String>> createStaffMap(List<Map<String, String>> rawStaffMap,
      String uidField) {
    Map<String, Map<String, String>> results = new HashMap<>();
    for (Map<String, String> entry : rawStaffMap) {
      String uid = entry.get(uidField);
      results.put(uid, entry);
    }
    return results;
  }

  /**
   * Create the Options used for the command-line.
   *
   * @return the Options used for the command-line.
   */
  private static Options getOptions() {
    Option outputOption = Option.builder("o")
        .longOpt("output")
        .hasArg()
        .argName("output file")
        .required()
        .desc("The Excel output filename")
        .build();
    Option configOption = Option.builder("c")
        .longOpt("config")
        .hasArg()
        .argName("properties file")
        .required()
        .desc("The properties file for configuring LDAP")
        .build();
    Option helpOption = Option.builder("h")
        .longOpt("help")
        .desc("Print this message")
        .build();

    Options options = new Options();
    options.addOption(outputOption);
    options.addOption(configOption);
    options.addOption(helpOption);

    return options;
  }

  /**
   * Prints a help message describing the command-line options
   *
   * @param options
   *          the Options for the command-line
   */
  private static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    String header = "Generates an Excel spreadsheet from LDAP.";
    formatter.printHelp(120, "staffdirectory-ldap", header, options, "", true);
  }
}
