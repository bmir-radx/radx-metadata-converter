package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import edu.stanford.bmir.radx.rad.metadata.compiler.ContextGenerator;
import edu.stanford.bmir.radx.rad.metadata.compiler.IdGenerator;
import org.metadatacenter.artifacts.model.core.*;
import org.metadatacenter.artifacts.model.core.fields.FieldInputType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.stanford.bmir.radx.rad.metadata.compiler.RadxSpecificationMetadataConstant.*;

public class Converter {
  private final TemplateSchemaArtifact oldTemplate;
  private final TemplateSchemaArtifact newTemplate;
  private final PathMap pathMap;
  private final ElementInstanceGenerator elementInstanceGenerator;
  private final FieldInstanceGenerator fieldInstanceGenerator;


  public Converter(TemplateSchemaArtifact oldTemplate, TemplateSchemaArtifact newTemplate, PathMap pathMap) {
    this.oldTemplate = oldTemplate;
    this.newTemplate = newTemplate;
    this.pathMap = pathMap;
    this.elementInstanceGenerator = new ElementInstanceGenerator(newTemplate);
    this.fieldInstanceGenerator = new FieldInstanceGenerator();
  }

  public TemplateInstanceArtifact convert(TemplateInstanceArtifact templateInstanceArtifact){
    var metadataValuesContainer = new MetadataValuesContainer();
    metadataValuesContainer.readMetadataInstance(templateInstanceArtifact);
    var values = metadataValuesContainer.getValues();
    var elementInstanceNumber = metadataValuesContainer.getElementsInstanceNumber();
    var attributeValueFields = metadataValuesContainer.getAttributeValueFields();

    var attributeValueFieldGenerator = new AttributeValueFieldGenerator(attributeValueFields);
    attributeValueFieldGenerator.createAttributeValueFieldValuesMap();

    var fieldSchemas = newTemplate.fieldSchemas();
    var elementSchemas = newTemplate.elementSchemas();

    var templateInstanceArtifactBuilder = TemplateInstanceArtifact.builder();
    buildChildFieldInstances(fieldSchemas, values, templateInstanceArtifactBuilder);
    buildChildElementInstances(elementSchemas, values,templateInstanceArtifactBuilder, elementInstanceNumber, attributeValueFieldGenerator);

    //generate JsonLdContext
    ContextGenerator.generateTemplateInstanceContext(newTemplate, templateInstanceArtifactBuilder);

    try {
      //generate JsonLdId
      IdGenerator.generateTemplateId(templateInstanceArtifactBuilder);

      //with IsBasedOn
      var id = newTemplate.jsonLdId();
      if (id.isPresent()) {
        templateInstanceArtifactBuilder.withIsBasedOn(id.get());
      } else {
        templateInstanceArtifactBuilder.withIsBasedOn(new URI(IS_BASED_ON.getValue()));
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    //with schemaName
    templateInstanceArtifactBuilder.withName(SCHEMA_NAME.getValue());

    //with description
    templateInstanceArtifactBuilder.withDescription(SCHEMA_DESCRIPTION.getValue());


    return templateInstanceArtifactBuilder.build();
  }

  private void buildChildFieldInstances(Map<String, FieldSchemaArtifact>fieldSchemas,
                                        Map<String, List<FieldValues>> values,
                                        TemplateInstanceArtifact.Builder builder){
    var fieldPathMap = pathMap.getFieldPathMap();
    for(var entry: fieldSchemas.entrySet()){
      var childFieldName = entry.getKey();
      var newFieldPath = "/" + childFieldName;
      var schema = entry.getValue();
      var type = schema.fieldUi().inputType();
      if(type.equals(FieldInputType.SECTION_BREAK)){
        continue;
      }

      var isMultiple = schema.isMultiple();
      if(fieldPathMap.containsKey(newFieldPath)){ //not new added fields
        var oldFieldPath = fieldPathMap.get(newFieldPath).get(0); //top level field should only map to one old field path
        var fieldValues = values.get(oldFieldPath);
        if(isMultiple){
          var fieldInstances = fieldInstanceGenerator.buildMultipleInstances(fieldValues, schema);
          builder.withMultiInstanceFieldInstances(childFieldName, fieldInstances);
        } else{ // is signal value
          if(fieldValues!= null && fieldValues.size() > 1){
            System.err.println("multiple values are found for single field at " + childFieldName);
          }
          var fieldInstance = fieldInstanceGenerator.buildSingleInstance(fieldValues, schema);
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

  private void buildChildElementInstances(Map<String, ElementSchemaArtifact> elementSchemas,
                                          Map<String, List<FieldValues>> values,
                                          TemplateInstanceArtifact.Builder builder,
                                          Map<String, Integer> elementsInstanceNumber,
                                          AttributeValueFieldGenerator attributeValueFieldGenerator){
    var elementPathMap = pathMap.getElementPathMap();
    var fieldPathMap = pathMap.getFieldPathMap();

    //Special handling for Individual Contributors and Organization Contributors
    var contributorHandler = new ContributorsHandler(newTemplate, oldTemplate);
    contributorHandler.buildIndivAndOrgContributors(elementsInstanceNumber, elementSchemas, values, fieldPathMap, builder);

    //Special handling for Funding Sources
    var funderHandler = new FunderHandler(newTemplate);
    funderHandler.patchFunderElementInstances(builder, contributorHandler, elementsInstanceNumber, elementSchemas, values, fieldPathMap);

    //Special handling for Dates
    var datesHandler = new DatesHandler(newTemplate);
    datesHandler.patchDatesElementInstances(builder, attributeValueFieldGenerator, elementsInstanceNumber, elementSchemas, values, fieldPathMap);

    //Special handling for Publications
    var publicationHandler = new PublicationHandler(newTemplate);
    publicationHandler.patchPublications(builder, elementsInstanceNumber, elementSchemas, values, fieldPathMap);

    //Special handling for Studies
    var parentStudiesHandler = new ParentStudiesHandler(newTemplate);
    parentStudiesHandler.patchParentStudiesElementInstances(builder, null, elementsInstanceNumber, elementSchemas, values, fieldPathMap);

    var skippedElements = Set.of(
        "individualContributors", "organizationalContributors", "fundingSources", "dates", "publications", "studies"
    );

    for(var entry: elementSchemas.entrySet()){
      var childElementName = entry.getKey();
      var childElementPath = "/" + childElementName;

      var elementSchema = entry.getValue();
      var isMultiple = elementSchema.isMultiple();
      if (!skippedElements.contains(childElementName)){
        var instanceNumber = elementPathMap.containsKey(childElementPath) ?
            elementsInstanceNumber.getOrDefault(elementPathMap.get(childElementPath).iterator().next(), 0) : 0;
        if (isMultiple) {
          if(elementPathMap.containsKey(childElementPath)){ //indicate it is not new added element
            var elementInstances = elementInstanceGenerator.buildMultipleElementInstances(values, fieldPathMap, attributeValueFieldGenerator, elementSchema, instanceNumber);
            builder.withMultiInstanceElementInstances(childElementName, elementInstances);
          } else{
            builder.withMultiInstanceElementInstances(childElementName, Collections.emptyList());
          }
        } else {
          var elementInstance = elementInstanceGenerator.buildSingleElementInstances(values, fieldPathMap, attributeValueFieldGenerator, elementSchema, instanceNumber-1);
          builder.withSingleInstanceElementInstance(childElementName, elementInstance);
        }
      }
    }
  }
}
