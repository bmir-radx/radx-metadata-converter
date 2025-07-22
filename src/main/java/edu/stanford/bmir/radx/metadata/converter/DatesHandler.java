package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import org.metadatacenter.artifacts.model.core.ElementInstanceArtifact;
import org.metadatacenter.artifacts.model.core.ElementSchemaArtifact;
import org.metadatacenter.artifacts.model.core.TemplateInstanceArtifact;
import org.metadatacenter.artifacts.model.core.TemplateSchemaArtifact;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatesHandler {
  private final String DATES_ELEMENT_NAME = "dates";
  private final String DATE_FIELD_NAME = "date";
  private final String CUSTOM_DATE_NAME = "data_file_creation_dateTime";
  private final String OLD_ATTRIBUTE_VALUE_SPECIFICATION_NAME = "/Auxiliary Metadata/Data File Descriptive Key-Value Pairs";
  private final String OLD_DATES_ELEMENT_NAME = "/Data File Dates";
  private final String EVENT_TYPE = "Created";
  private final String EVENT_TYPE_FIELD_NAME = "eventType";
  private final String EVENT_TYPE_URI = "https://w3id.org/gdmt/Created";

  private final TemplateSchemaArtifact newTemplateSchemaArtifact;
  private final ElementInstanceGenerator elementInstanceGenerator;
  private final FieldInstanceGenerator fieldInstanceGenerator;

  public DatesHandler(TemplateSchemaArtifact newTemplateSchemaArtifact) {
    this.newTemplateSchemaArtifact = newTemplateSchemaArtifact;
    this.elementInstanceGenerator = new ElementInstanceGenerator(newTemplateSchemaArtifact);
    this.fieldInstanceGenerator = new FieldInstanceGenerator();
  }

  public void patchDatesElementInstances(TemplateInstanceArtifact.Builder builder,
                                         AttributeValueFieldGenerator attributeValueFieldGenerator,
                                         Map<String, Integer> elementsInstanceNumber,
                                         Map<String, ElementSchemaArtifact> elementSchemas,
                                         Map<String, List<FieldValues>> values,
                                         Map<String, List<String>> fieldPathMap
  ){
    var attributeValueMap = attributeValueFieldGenerator.getAttributeValueFieldValuesMap();
    var datesElementSchema = elementSchemas.get(DATES_ELEMENT_NAME);
    var instanceNumber = elementsInstanceNumber.getOrDefault(OLD_DATES_ELEMENT_NAME, 0);
    var elementInstances = elementInstanceGenerator.buildMultipleElementInstances(values, fieldPathMap, null, datesElementSchema, instanceNumber);
    if(instanceNumber > 1){
      System.err.println("More than one Dates element instance");
    }

    String dateTime = null;
    //if AV contains data_file_creation_dateTime, then it needs to be updated
    if(attributeValueMap.containsKey(OLD_ATTRIBUTE_VALUE_SPECIFICATION_NAME) && attributeValueMap.get(OLD_ATTRIBUTE_VALUE_SPECIFICATION_NAME).containsKey(CUSTOM_DATE_NAME)){
      dateTime = attributeValueMap.get(OLD_ATTRIBUTE_VALUE_SPECIFICATION_NAME).get(CUSTOM_DATE_NAME);
      var datesElementInstances = elementInstances.get(0);
      var datesBuilder = ElementInstanceArtifact.builder(datesElementInstances);

      //patch
      var dateFieldSchema = datesElementSchema.getFieldSchemaArtifact(DATE_FIELD_NAME);
      var dateValue = new FieldValues(null, Optional.empty(), Optional.of(dateTime), Optional.empty());
      var dateFieldInstance = fieldInstanceGenerator.buildSingleInstance(List.of(dateValue), dateFieldSchema);
      datesBuilder.withoutSingleInstanceFieldInstance(DATE_FIELD_NAME);
      datesBuilder.withSingleInstanceFieldInstance(DATE_FIELD_NAME, dateFieldInstance);

      //add event type as "Created"
      try {
        var eventTypeUri = new URI(EVENT_TYPE_URI);
        var eventTypeValue = new FieldValues(null, Optional.of(eventTypeUri), Optional.empty(), Optional.of(EVENT_TYPE));
        var eventTypeFieldInstance = fieldInstanceGenerator.buildSingleControlledTermFieldInstance(eventTypeValue);

        datesBuilder.withoutSingleInstanceFieldInstance(EVENT_TYPE_FIELD_NAME);
        datesBuilder.withSingleInstanceFieldInstance(EVENT_TYPE_FIELD_NAME, eventTypeFieldInstance);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }

      elementInstances = List.of(datesBuilder.build());
    }

    builder.withMultiInstanceElementInstances(DATES_ELEMENT_NAME, elementInstances);
  }
}
