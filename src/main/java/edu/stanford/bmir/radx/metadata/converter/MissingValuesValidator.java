package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.AttributeValueFieldValues;
import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import edu.stanford.bmir.radx.metadata.validator.lib.TemplateInstanceValuesReporter;
import org.metadatacenter.artifacts.model.core.TemplateInstanceArtifact;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MissingValuesValidator {
  private final String date = "date";
  private final String givenName = "given name";
  private final String familyName = "family name";
  private Set<String> oldInstanceValues = new HashSet<>();
  private Set<String> newInstanceValues = new HashSet<>();
  public void checkMissingValues(TemplateInstanceArtifact oldInstance, TemplateInstanceArtifact newInstance){
    var oldReporter = new TemplateInstanceValuesReporter(oldInstance);
    var oldFieldValues = oldReporter.getValues();
    var oldAttributeValues = oldReporter.getAttributeValueFields();
    addValues(oldFieldValues, oldAttributeValues, true);

    var newReporter = new TemplateInstanceValuesReporter(newInstance);
    var newFieldValues = newReporter.getValues();
    var newAttributeValues =  newReporter.getAttributeValueFields();
    addValues(newFieldValues, newAttributeValues, false);
  }

  private void addValues(Map<String, FieldValues> fieldValues, List<AttributeValueFieldValues> attributeValueFieldValues, boolean isOldVersion){
    for(var entry: fieldValues.entrySet()){
      var path = entry.getKey();
      var fieldValue = entry.getValue();
      if (isOldVersion && !isModifiedValue(path)){
        addSingleFieldValue(fieldValue, oldInstanceValues);
      }
      if(!isOldVersion){
        addSingleFieldValue(fieldValue, newInstanceValues);
      }
    }

    for(var attributeValueFieldValue: attributeValueFieldValues){
      var path = attributeValueFieldValue.path();
      if (isOldVersion && !isModifiedValue(path)){
        addSingleAVFieldValue(attributeValueFieldValue, oldInstanceValues);
      }
      if(!isOldVersion){
        addSingleAVFieldValue(attributeValueFieldValue, newInstanceValues);
      }
    }
  }

  private boolean isModifiedValue(String path){
    return path.toLowerCase().contains(date) || path.toLowerCase().contains(givenName) || path.toLowerCase().contains(familyName);
  }

  private void addSingleFieldValue(FieldValues fieldValues, Set<String> instanceValues){
    var id = fieldValues.jsonLdId();
    var label = fieldValues.label();
    var value = fieldValues.jsonLdValue();
    id.ifPresent(uri -> instanceValues.add(uri.toString()));
    label.ifPresent(instanceValues::add);
    value.ifPresent(instanceValues::add);
  }

  private void addSingleAVFieldValue(AttributeValueFieldValues attributeValueFieldValues, Set<String> instanceValues){
    var fieldValues = attributeValueFieldValues.fieldValues();
    addSingleFieldValue(fieldValues, instanceValues);
  }

  private void check(){
    for (String value : oldInstanceValues) {
      if (!newInstanceValues.contains(value)) {
        System.err.println("Error: Value '" + value + "' is missing.");
      }
    }
  }
}
