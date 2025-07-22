package edu.stanford.bmir.radx.metadata.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PathMap {
    private Map<String, List<String>> fieldPathMap = new HashMap<>();
    private Map<String, Set<String>> elementPathMap = new HashMap<>();
    public void readCsvToMap(Path csvPath) {
        try (BufferedReader br = Files.newBufferedReader(csvPath)) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip header
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    String newFieldPath = parts[0].trim();
                    String newFieldPathCamelCase = convertPath2CamelCase(newFieldPath);
                    String oldFieldPath = parts[1].trim();
                    if (!oldFieldPath.isEmpty()) {
                        fieldPathMap.computeIfAbsent(newFieldPathCamelCase, k -> new ArrayList<>()).add(oldFieldPath);
                        addToElementPathMap(newFieldPathCamelCase, oldFieldPath);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read CSV: " + csvPath + " due to: " + e.getMessage());
        }
    }

    public Map<String, List<String>> getFieldPathMap() {
        return fieldPathMap;
    }

    public Map<String, Set<String>> getElementPathMap() {
        return elementPathMap;
    }

    private void addToElementPathMap(String newFieldPath, String oldFieldPath){
        var newFieldElementParts = newFieldPath.split("/");
        var oldElement = "/" + oldFieldPath.split("/")[1];
        if (newFieldElementParts.length > 2) {
            String newElement = "/" + newFieldElementParts[1];
            elementPathMap.computeIfAbsent(newElement, k -> new HashSet<>()).add(oldElement);
        }
    }

    /*
    This method convert field title to field name, e.g. /Individual contributors/Contributor email -> /individualContributors/contributorEmail
     */
    private String convertPath2CamelCase(String fieldPath){
        var parts = fieldPath.split("/");
        var result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            var words = part.split(" ");
            if (words.length == 0) continue;

            var camelPart = new StringBuilder(words[0].toLowerCase());
            for (int i = 1; i < words.length; i++) {
                camelPart.append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1).toLowerCase());
            }
            result.append("/").append(camelPart);
        }

        return result.toString();
    }
}
