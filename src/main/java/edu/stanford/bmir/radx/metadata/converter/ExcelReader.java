package edu.stanford.bmir.radx.metadata.converter;

import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static edu.stanford.bmir.radx.metadata.converter.HeaderName.*;

public class ExcelReader {
  private static Map<String, String> orgMeta = new HashMap<>();
  private static Map<String, Map<HeaderName, String>> personMeta = new HashMap<>();
  private static Map<String, String> phs2StudyId = new HashMap<>();

  public static void readCreatorsMetadata(){
    String fileName = "creators_metadata.xlsx";

    // Read from local resources folder
    try (var inputStream = ExcelReader.class.getClassLoader().getResourceAsStream(fileName);
         var workbook = WorkbookFactory.create(inputStream)) {

      // Sheet 1: data_file_orgs_metadata_dump_12
      orgMeta = readOrgMetadata(workbook, "data_file_orgs_metadata_dump_12");

      // Sheet 2: data_file_creators_metadata_dum
      personMeta = readCreatorMetadata(workbook, "data_file_creators_metadata_dum");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void readStudiesMetadata(){
    String fileName = "RADx_studies_metadata_01222025.xlsx";
    String sheetName = "RADx_studies";
    try (var inputStream = ExcelReader.class.getClassLoader().getResourceAsStream(fileName);
         var workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheet(sheetName);
      if (sheet == null) throw new IllegalArgumentException("Sheet not found: " + sheetName);

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        String studyId = getCellValue(row.getCell(0)); // Column A
        String phs = getCellValue(row.getCell(1)).trim();     // Column B

        if (!phs.isEmpty() && !studyId.isEmpty()) {
          phs2StudyId.put(phs, studyId);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, String> readOrgMetadata(Workbook workbook, String sheetName) {
    Map<String, String> map = new LinkedHashMap<>();
    Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) throw new IllegalArgumentException("Sheet not found: " + sheetName);

    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;

      String creator = getCellValue(row.getCell(4)); // Column E
      String org = getCellValue(row.getCell(5));     // Column F

      if (!creator.isEmpty()) {
        map.put(creator, org);
      }
    }
    return map;
  }

  private static Map<String, Map<HeaderName, String>> readCreatorMetadata(Workbook workbook, String sheetName) {
    Map<String, Map<HeaderName, String>> nestedMap = new HashMap<>();
    Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) throw new IllegalArgumentException("Sheet not found: " + sheetName);

    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;

      String creatorName = getCellValue(row.getCell(5)); // Column F
      if (creatorName.isEmpty()) continue;

      Map<HeaderName, String> details = new HashMap<>();
      details.put(CREATOR_NAME_CLEANED, getCellValue(row.getCell(8)));       // Column I
      details.put(CREATOR_GIVEN_NAME_CLEANED, getCellValue(row.getCell(9))); // Column J
      details.put(CREATOR_FAMILY_NAME_CLEANED, getCellValue(row.getCell(10)));// Column K
      details.put(ORCID_ID, getCellValue(row.getCell(11)));                 // Column L
      details.put(CREATOR_AFFILIATION_ROR, getCellValue(row.getCell(4)));   // Column E

      nestedMap.put(creatorName, details);
    }

    return nestedMap;
  }

  private static String getCellValue(Cell cell) {
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC -> {
        double num = cell.getNumericCellValue();
        if (num == Math.floor(num)) {
          // It's effectively an integer (e.g., 1.0)
          yield String.valueOf((long) num);
        } else {
          yield String.valueOf(num);
        }
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      default -> "";
    };
  }

  public static Map<String, String> getOrgMeta() {
    return orgMeta;
  }

  public static Map<String, Map<HeaderName, String>> getPersonMeta() {
    return personMeta;
  }

  public static Map<String, String> getPhs2StudyId() {
    return phs2StudyId;
  }
}
