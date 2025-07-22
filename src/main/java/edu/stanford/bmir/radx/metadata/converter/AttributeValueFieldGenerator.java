package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.AttributeValueFieldValues;
import edu.stanford.bmir.radx.rad.metadata.compiler.FieldInstanceArtifactGenerator;
import org.metadatacenter.artifacts.model.core.FieldInstanceArtifact;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AttributeValueFieldGenerator {
  private final List<AttributeValueFieldValues> attributeValueFieldValuesList;

  // {old_specificationPath: {old_actual_path_without_element_name: value}}
  //e.g., {"/Auxiliary Metadata/Key-Value Pairs": {subproject: value1}}
  private Map<String, Map<String, String>> attributeValueFieldValuesMap = new HashMap<>();
  private final FieldInstanceArtifactGenerator fieldInstanceArtifactGenerator = new FieldInstanceArtifactGenerator();

  public AttributeValueFieldGenerator(List<AttributeValueFieldValues> attributeValueFieldValuesList) {
    this.attributeValueFieldValuesList = attributeValueFieldValuesList;
  }

  public LinkedHashMap<String, FieldInstanceArtifact> buildAttributeFields(String oldSpecificationPath){
    if(oldSpecificationPath == null){
      return new LinkedHashMap<>();
    }

    if(attributeValueFieldValuesMap.containsKey(oldSpecificationPath)){
       var valuesMap = attributeValueFieldValuesMap.get(oldSpecificationPath);
       //specific delete data_file_creation_dateTime
      if (valuesMap.containsKey("data_file_creation_dateTime")){
        valuesMap.remove("data_file_creation_dateTime");
      }
      var customFieldName = valuesMap.keySet();
       return fieldInstanceArtifactGenerator.buildAttributeValueField(valuesMap, customFieldName.stream().toList());
    } else{
      return new LinkedHashMap<>();
    }
  }

  public void createAttributeValueFieldValuesMap(){
    for(var attributeValueFieldValue: attributeValueFieldValuesList) {
      var path = attributeValueFieldValue.path();                            //the actual path, for example: /Auxiliary Metadata/subproject
      var specificationPath = attributeValueFieldValue.specificationPath(); //the path in the template, e.g., /Auxiliary Metadata/Key-Value Pairs
      var value = attributeValueFieldValue.fieldValues().jsonLdValue();

      if(value.isPresent()){
        var customFieldName = path.split("/")[2];
        attributeValueFieldValuesMap
            .computeIfAbsent(specificationPath, k -> new HashMap<>())
            .put(customFieldName, value.get());
      }
    }
  }

  public Map<String, Map<String, String>> getAttributeValueFieldValuesMap() {
    return attributeValueFieldValuesMap;
  }
}
