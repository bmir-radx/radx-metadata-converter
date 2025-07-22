package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import edu.stanford.bmir.radx.rad.metadata.compiler.AttributeValueFieldUtil;
import edu.stanford.bmir.radx.rad.metadata.compiler.ContextGenerator;
import edu.stanford.bmir.radx.rad.metadata.compiler.IdGenerator;
import org.metadatacenter.artifacts.model.core.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ElementInstanceGenerator {
  private final TemplateSchemaArtifact newTemplateSchemaArtifact;
  private final FieldInstanceGenerator fieldInstanceGenerator = new FieldInstanceGenerator();

  public ElementInstanceGenerator(TemplateSchemaArtifact newTemplateSchemaArtifact) {
    this.newTemplateSchemaArtifact = newTemplateSchemaArtifact;
  }

  public List<ElementInstanceArtifact> buildMultipleElementInstances(Map<String, List<FieldValues>> values,
                                                                     Map<String, List<String>> fieldPathMap,
                                                                     AttributeValueFieldGenerator attributeValueFieldGenerator,
                                                                     ElementSchemaArtifact elementSchemaArtifact,
                                                                     Integer instanceNumber){
    var instances = new ArrayList<ElementInstanceArtifact>();
    for(int i=0; i< instanceNumber; i++){
      instances.add(buildSingleElementInstances(values, fieldPathMap, attributeValueFieldGenerator, elementSchemaArtifact, i));
    }
    return instances;
  }

  public ElementInstanceArtifact buildSingleElementInstances(Map<String, List<FieldValues>> values,
                                                             Map<String, List<String>> fieldPathMap,
                                                             AttributeValueFieldGenerator attributeValueFieldGenerator,
                                                             ElementSchemaArtifact elementSchemaArtifact,
                                                             Integer i){
    var currentNewPath = "/" + elementSchemaArtifact.name();
    var childFieldSchemas = elementSchemaArtifact.fieldSchemas();
    var childElementSchemas = elementSchemaArtifact.elementSchemas();
    var elementInstanceArtifactBuilder = ElementInstanceArtifact.builder();

    buildChildFieldInstances(childFieldSchemas, values, fieldPathMap, attributeValueFieldGenerator, elementInstanceArtifactBuilder, currentNewPath, i);
    buildChildElementInstances(childElementSchemas, values, fieldPathMap, attributeValueFieldGenerator, elementInstanceArtifactBuilder, currentNewPath, i);

    ContextGenerator.generateElementInstanceContext(elementSchemaArtifact, elementInstanceArtifactBuilder);

    // add @id
    try {
      IdGenerator.generateElementId(elementInstanceArtifactBuilder);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return elementInstanceArtifactBuilder.build();
  }

  private void buildChildFieldInstances(Map<String, FieldSchemaArtifact>fieldSchemas,
                                        Map<String, List<FieldValues>> values,
                                        Map<String, List<String>> fieldPathMap,
                                        AttributeValueFieldGenerator attributeValueFields,
                                        ElementInstanceArtifact.Builder builder,
                                        String newPath,
                                        Integer i){

    for(var entry: fieldSchemas.entrySet()){
      var childFieldName = entry.getKey();
      var newFieldPath = newPath + "/" + childFieldName;

//      System.out.println(newFieldPath);

      var schema = entry.getValue();
      var isMultiple = schema.isMultiple();

      //Check if the child field is an attribute value type, which need to be build differently
      if(AttributeValueFieldUtil.isAttributeValue(newTemplateSchemaArtifact, newFieldPath)){
        var oldSpecificationPath = fieldPathMap.containsKey(newFieldPath) ? fieldPathMap.get(newFieldPath).get(0): null;
        var attributeValueFieldGroup = attributeValueFields.buildAttributeFields(oldSpecificationPath);
        builder.withAttributeValueFieldGroup(childFieldName, attributeValueFieldGroup);
        continue;
      }

      if(fieldPathMap.containsKey(newFieldPath)){ //not new added fields
        var oldFieldPath = fieldPathMap.get(newFieldPath).get(0);
        var fieldValues = values.get(oldFieldPath);
        var fieldValue = fieldValues.get(i);
        //dbgapStudyPage(a link field) map to Study Identifier, which is a text field, need to assign value to id
        if(childFieldName.equals("dbgapStudyPage")){
          fieldValue = patchDbGapUrl(fieldValue);
        }
        var valueNumber = fieldValues.size();
        if(valueNumber < i + 1){
          System.err.println("Value number is greater than field value instances.");
        }

        if(isMultiple){
          var fieldInstances = fieldInstanceGenerator.buildMultipleInstances(List.of(fieldValue), schema);
          builder.withMultiInstanceFieldInstances(childFieldName, fieldInstances);
        } else{ // is signal value
          var fieldInstance = fieldInstanceGenerator.buildSingleInstance(List.of(fieldValue), schema);
          builder.withSingleInstanceFieldInstance(childFieldName, fieldInstance);
        }
      } else{ //new added fields
        if(isMultiple){
          var fieldInstances = fieldInstanceGenerator.buildMultipleInstances(new ArrayList<>(), schema);
          builder.withMultiInstanceFieldInstances(childFieldName, fieldInstances);
        } else{
          var fieldInstance = fieldInstanceGenerator.buildSingleInstance(new ArrayList<>(), schema);
          builder.withSingleInstanceFieldInstance(childFieldName, fieldInstance);
        }
      }
    }
  }

 // In addition to Contributors, only Sample Variables have nested element, which is empty.
  private void buildChildElementInstances(Map<String, ElementSchemaArtifact> elementSchemas,
                                          Map<String, List<FieldValues>> values,
                                          Map<String, List<String>> fieldPathMap,
                                          AttributeValueFieldGenerator attributeValueFieldGenerator,
                                          ElementInstanceArtifact.Builder builder,
                                          String newPath,
                                          Integer i){
    for(var entry: elementSchemas.entrySet()){
      var childElementName = entry.getKey();
      var childElementSchema = entry.getValue();
      var isMultiple = childElementSchema.isMultiple();
      var childElementBuilder = ElementInstanceArtifact.builder();
      var currentPath = newPath + "/" + childElementName;
//      System.out.printf(currentPath);
      if(isMultiple){ // build empty child element list
//        var grandChildElementSchemas = childElementSchema.elementSchemas();
//        buildChildElementInstances(grandChildElementSchemas, values, fieldPathMap, attributeValueFieldGenerator, childElementBuilder, currentPath, i);
        builder.withMultiInstanceElementInstances(childElementName, Collections.emptyList());
      }
      else{
        var grandChildFieldSchemas = childElementSchema.fieldSchemas();
        buildChildFieldInstances(grandChildFieldSchemas, values, fieldPathMap, attributeValueFieldGenerator, childElementBuilder, currentPath, i);
        builder.withSingleInstanceElementInstance(childElementName, childElementBuilder.build());
      }
    }
  }

  private FieldValues patchDbGapUrl(FieldValues values){
    var value = values.jsonLdValue();
    if(value.isPresent()){
      try {
        var id = new URI(value.get());
        return new FieldValues(new ArrayList<>(), Optional.of(id), Optional.empty(), Optional.empty());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    return values;
  }
}
