package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.AttributeValueFieldValues;
import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import edu.stanford.bmir.radx.metadata.validator.lib.TemplateInstanceValuesReporter;
import org.metadatacenter.artifacts.model.core.TemplateInstanceArtifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataValuesContainer {
  // {old path: [value1, value2]}
  // the index of value is the index of instance, so should insert null if it doesn't have a value.
  private Map<String, List<FieldValues>> values = new HashMap<>();
  //
  private Map<String, Integer> elementsInstanceNumber = new HashMap<>();
  private Map<String, Integer> fieldsInstanceNumber = new HashMap<>();
  private List<AttributeValueFieldValues> attributeValueFields = new ArrayList<>();

  public void readMetadataInstance(TemplateInstanceArtifact instance){
    var valuesReporter = new TemplateInstanceValuesReporter(instance);
    elementsInstanceNumber = valuesReporter.getElementCardinalities();
    fieldsInstanceNumber = valuesReporter.getFieldCardinalities();
    attributeValueFields = valuesReporter.getAttributeValueFields();
    var valuesWithIndex = valuesReporter.getValues();

    valuesWithIndex.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> {
          var normalizedPath = normalizePath(entry.getKey());
          values.computeIfAbsent(normalizedPath, k -> new ArrayList<>()).add(entry.getValue());
        });
//    for(var entry: valuesWithIndex.entrySet()){
//      var normalizedPath = normalizePath(entry.getKey());
//      values.computeIfAbsent(normalizedPath, k -> new ArrayList<>()).add(entry.getValue());
//    }
  }

  public Map<String, List<FieldValues>> getValues() {
    return values;
  }

  public Map<String, Integer> getElementsInstanceNumber() {
    return elementsInstanceNumber;
  }

  public Map<String, Integer> getFieldsInstanceNumber() {
    return fieldsInstanceNumber;
  }

  public List<AttributeValueFieldValues> getAttributeValueFields() {
    return attributeValueFields;
  }

  private String normalizePath(String pathWithIndex){
    return pathWithIndex.replaceAll("\\[\\d+\\]", "");
  }
}
