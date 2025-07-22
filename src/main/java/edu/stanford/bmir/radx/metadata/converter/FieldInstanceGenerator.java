package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import edu.stanford.bmir.radx.rad.metadata.compiler.FieldInstanceArtifactGenerator;
import edu.stanford.bmir.radx.rad.metadata.compiler.FieldType;
import org.metadatacenter.artifacts.model.core.*;

import java.util.*;

import static edu.stanford.bmir.radx.rad.metadata.compiler.FieldType.*;

public class FieldInstanceGenerator {
  private final FieldInstanceArtifactGenerator fieldInstanceArtifactGenerator = new FieldInstanceArtifactGenerator();
  public <T extends FieldInstanceArtifact> List<T> buildMultipleInstances(List<FieldValues> fieldValues, FieldSchemaArtifact fieldSchemaArtifact){
    var valueConstraints = fieldSchemaArtifact.valueConstraints();
    var fieldType = FieldType.getFieldType(fieldSchemaArtifact);
    if(fieldValues == null || fieldValues.isEmpty() || emptyValues(fieldValues, fieldType)){ // if empty strings, build empty field instance list
      return new ArrayList<>();
    } else{
      if(fieldType.equals(CONTROLLED_TERM)){
        return buildMultiControlledTermFieldsInstance(fieldValues);
      } else if (fieldType.equals(ROR)) {
        return buildMultiRorFieldsInstance(fieldValues);
      } else if (fieldType.equals(ORCID)) {
        return buildMultiOrcidFieldsInstance(fieldValues);
      } else{
        var valueStrings = getValueStrings(fieldValues, fieldType);
        return fieldInstanceArtifactGenerator.buildMultipleInstancesWithValues(fieldType, valueStrings, valueConstraints);
      }
    }
  }

  public <T extends FieldInstanceArtifact> T buildSingleInstance(List<FieldValues> fieldValuesList, FieldSchemaArtifact fieldSchemaArtifact){
    var valueConstraints = fieldSchemaArtifact.valueConstraints();
    var fieldType = FieldType.getFieldType(fieldSchemaArtifact);
    var buildEmptyFieldInstance = false;

    //Determine if it is an empty list or empty field value. If so, build a FieldValue with empty label, id, value
    if(fieldValuesList == null || fieldValuesList.isEmpty() || emptyValues(List.of(fieldValuesList.get(0)), fieldType)){
      buildEmptyFieldInstance = true;
      var fieldValues = new FieldValues(new ArrayList<>(), Optional.empty(), Optional.empty(), Optional.empty());
      fieldValuesList = List.of(fieldValues);
    }

    //If it is Controlled Term, ROR or Orcid type, build separately
    if(fieldType.equals(CONTROLLED_TERM)){
       return buildSingleControlledTermFieldInstance(fieldValuesList.get(0));
    }else if (fieldType.equals(ROR)) {
      return buildSingleRorFieldInstance(fieldValuesList.get(0));
    } else if (fieldType.equals(ORCID)) {
      return buildSingleOrcidFieldInstance(fieldValuesList.get(0));
    }

    //For other types of field, use the FieldInstanceArtifactGenerator
    if(buildEmptyFieldInstance){
      return fieldInstanceArtifactGenerator.buildSingleEmptyInstance(fieldType, valueConstraints);
    } else{
      var valueString = getValueStrings(fieldValuesList, fieldType).get(0);
      return fieldInstanceArtifactGenerator.buildSingleInstanceWithValue(fieldType, valueString, valueConstraints);
    }
  }

  public <T extends FieldInstanceArtifact> List<T> buildMultiControlledTermFieldsInstance(List<FieldValues> fieldValues){
    List<T> controlledTermFieldInstances = new ArrayList<>();
    for(var fieldValue: fieldValues){
      controlledTermFieldInstances.add(buildSingleControlledTermFieldInstance(fieldValue));
    }

    return controlledTermFieldInstances;
  }

  public <T extends FieldInstanceArtifact> List<T> buildMultiRorFieldsInstance(List<FieldValues> fieldValues){
    List<T> rorFieldInstances = new ArrayList<>();
    for(var fieldValue: fieldValues){
      rorFieldInstances.add(buildSingleRorFieldInstance(fieldValue));
    }

    return rorFieldInstances;
  }

  public <T extends FieldInstanceArtifact> List<T> buildMultiOrcidFieldsInstance(List<FieldValues> fieldValues){
    List<T> orcidFieldInstances = new ArrayList<>();
    for(var fieldValue: fieldValues){
      orcidFieldInstances.add(buildSingleOrcidFieldInstance(fieldValue));
    }

    return orcidFieldInstances;
  }

  private List<String> getValueStrings(List<FieldValues> fieldValues, FieldType fieldType) {
    List<String> values = new ArrayList<>();
    for (var fieldValue : fieldValues) {
      if (fieldType.equals(LINK)) {
        var currentValue = fieldValue.jsonLdId();
        currentValue.ifPresent(uri -> values.add(uri.toString()));
      } else {
        var currentValue = fieldValue.jsonLdValue();
        currentValue.ifPresent(values::add);
      }
    }
    return values;
  }

  public <T extends FieldInstanceArtifact> T buildSingleControlledTermFieldInstance(FieldValues fieldValues){
    var label = fieldValues.label();
    var fieldId = fieldValues.jsonLdId();
    var fieldInstanceArtifactBuilder = ControlledTermFieldInstance.builder();

    if(label.isPresent() && fieldId.isPresent()){
      return (T) fieldInstanceArtifactBuilder
          .withLabel(label.get())
          .withValue(fieldId.get())
          .build();
    } else {
      if (label.isPresent()) {
        System.err.println("Warning: Label is present, but ID is missing.");
      }
      if (fieldId.isPresent()) {
        System.err.println("Warning: ID is present, but label is missing.");
      }
      return (T) fieldInstanceArtifactBuilder.build();
    }
  }

  public <T extends FieldInstanceArtifact> T buildSingleRorFieldInstance(FieldValues fieldValues){
    var label = fieldValues.label();
    var fieldId = fieldValues.jsonLdId();
    var fieldInstanceArtifactBuilder = RorFieldInstance.builder();

    if(label.isPresent() && fieldId.isPresent()){
      return (T) fieldInstanceArtifactBuilder
          .withLabel(label.get())
          .withValue(fieldId.get())
          .build();
    } else {
      if (label.isPresent()) {
        System.err.println("Warning: Label is present, but ID is missing.");
      }
      if (fieldId.isPresent()) {
        System.err.println("Warning: ID is present, but label is missing.");

      }
      return (T) fieldInstanceArtifactBuilder.build();
    }
  }

  public <T extends FieldInstanceArtifact> T buildSingleOrcidFieldInstance(FieldValues fieldValues){
    var label = fieldValues.label();
    var fieldId = fieldValues.jsonLdId();
    var fieldInstanceArtifactBuilder = OrcidFieldInstance.builder();

    if(label.isPresent() && fieldId.isPresent()){
      return (T) fieldInstanceArtifactBuilder
          .withLabel(label.get())
          .withValue(fieldId.get())
          .build();
    } else {
      if (label.isPresent()) {
        System.err.println("Warning: Label is present, but ID is missing.");
      }
      if (fieldId.isPresent()) {
        System.err.println("Warning: ID is present, but label is missing.");

      }
      return (T) fieldInstanceArtifactBuilder.build();
    }
  }

  private boolean emptyValues(List<FieldValues> fieldValues, FieldType fieldType){
    var isEmpty = true;
    for (var fieldValue : fieldValues) {
      if (fieldType.equals(LINK)) {
        var currentValue = fieldValue.jsonLdId();
        if(currentValue.isPresent() && !currentValue.get().toString().equals(" ")){
          isEmpty = false;
        }
      } else if (fieldType.equals(CONTROLLED_TERM) || fieldType.equals(ROR) || fieldType.equals(ORCID)) {
//        var currentLabel = fieldValue.label();
        var currentId = fieldValue.jsonLdId();
//        if(currentLabel.isPresent() && !currentLabel.get().equals(" ")){
          if(currentId.isPresent()){
          isEmpty = false;
        }
      } else {
        var currentValue = fieldValue.jsonLdValue();
        if(currentValue.isPresent() && !currentValue.get().equals(" ")){
          isEmpty = false;
        }
      }
    }
    return isEmpty;
  }
}
