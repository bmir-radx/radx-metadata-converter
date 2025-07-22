package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import org.metadatacenter.artifacts.model.core.ElementInstanceArtifact;
import org.metadatacenter.artifacts.model.core.ElementSchemaArtifact;
import org.metadatacenter.artifacts.model.core.TemplateInstanceArtifact;
import org.metadatacenter.artifacts.model.core.TemplateSchemaArtifact;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class ParentStudiesHandler {
  private final String STUDIES_ELEMENT_NAME = "studies";
  private final String DBGAP_STUDY_PAGE_FIELD_NAME = "dbgapStudyPage";
  private final String RADX_DATA_HUB_STUDY_PAGE_FIELD_NAME = "radxDataHubStudyPage";
  private final String PHS_NUMBER_FIELD_NAME = "phsNumber";
  private final String OLD_PARENT_STUDIES_ELEMENT_NAME = "/Data File Parent Studies";
  private final String DBGAP_PREFIX = "https://www.ncbi.nlm.nih.gov/projects/gap/cgi-bin/study.cgi?study_id=";
  private final String RADX_PREFIX = "https://radxdatahub.nih.gov/study/";
  private final TemplateSchemaArtifact newTemplateSchemaArtifact;
  private final ElementInstanceGenerator elementInstanceGenerator;
  private final FieldInstanceGenerator fieldInstanceGenerator;

  public ParentStudiesHandler(TemplateSchemaArtifact newTemplateSchemaArtifact) {
    this.newTemplateSchemaArtifact = newTemplateSchemaArtifact;
    this.elementInstanceGenerator = new ElementInstanceGenerator(newTemplateSchemaArtifact);
    this.fieldInstanceGenerator = new FieldInstanceGenerator();
  }

  public void patchParentStudiesElementInstances(TemplateInstanceArtifact.Builder builder,
                                         AttributeValueFieldGenerator attributeValueFieldGenerator,
                                         Map<String, Integer> elementsInstanceNumber,
                                         Map<String, ElementSchemaArtifact> elementSchemas,
                                         Map<String, List<FieldValues>> values,
                                         Map<String, List<String>> fieldPathMap
  ){
    var parentElementSchema = elementSchemas.get(STUDIES_ELEMENT_NAME);
    var instanceNumber = elementsInstanceNumber.get(OLD_PARENT_STUDIES_ELEMENT_NAME);
    var elementInstances = elementInstanceGenerator.buildMultipleElementInstances(values, fieldPathMap, null, parentElementSchema, instanceNumber);
    var dbGapFieldSchema = parentElementSchema.getFieldSchemaArtifact(DBGAP_STUDY_PAGE_FIELD_NAME);
    var radxFieldSchema = parentElementSchema.getFieldSchemaArtifact(RADX_DATA_HUB_STUDY_PAGE_FIELD_NAME);

    //Add dbGap study page and RADx Data Hub webpage
    var updatedElementInstances = new ArrayList<ElementInstanceArtifact>();
    for(var instance: elementInstances){
      var studyBuilder = ElementInstanceArtifact.builder(instance);
      var phsNumber = instance.singleInstanceFieldInstances().get(PHS_NUMBER_FIELD_NAME).jsonLdValue();
      if(phsNumber.isPresent()){
        var phsNumberString = phsNumber.get();
        var cleanedPhsNumber = truncateAfterFirstDot(phsNumberString);

        var dbGapLink = getUri(DBGAP_PREFIX, cleanedPhsNumber);
        var dbGapValue = new FieldValues(null, Optional.of(dbGapLink), Optional.empty(), Optional.empty());
        var dbGapFieldInstance = fieldInstanceGenerator.buildSingleInstance(List.of(dbGapValue), dbGapFieldSchema);
        studyBuilder.withoutSingleInstanceFieldInstance(DBGAP_STUDY_PAGE_FIELD_NAME);
        studyBuilder.withSingleInstanceFieldInstance(DBGAP_STUDY_PAGE_FIELD_NAME, dbGapFieldInstance);

        ExcelReader.readStudiesMetadata();
        var phs2StudyId = ExcelReader.getPhs2StudyId();
        if(phs2StudyId.containsKey(cleanedPhsNumber)){
          var studyId = phs2StudyId.get(cleanedPhsNumber);
          var radxLink = getUri(RADX_PREFIX, studyId);
          var radxValue = new FieldValues(null, Optional.of(radxLink), Optional.empty(), Optional.empty());
          var radxFieldInstance = fieldInstanceGenerator.buildSingleInstance(List.of(radxValue), dbGapFieldSchema);
          studyBuilder.withoutSingleInstanceFieldInstance(RADX_DATA_HUB_STUDY_PAGE_FIELD_NAME);
          studyBuilder.withSingleInstanceFieldInstance(RADX_DATA_HUB_STUDY_PAGE_FIELD_NAME, radxFieldInstance);
        } else{
          System.err.println("Could not find study id for the study " + cleanedPhsNumber);
        }
      }

      updatedElementInstances.add(studyBuilder.build());
    }

    builder.withMultiInstanceElementInstances(STUDIES_ELEMENT_NAME, updatedElementInstances);
  }

  private URI getUri(String prefix, String value){
    try {
      return new URI(prefix + value);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private String truncateAfterFirstDot(String input) {
    // Regular expression: "phs" followed by exactly 6 digits
    var pattern = Pattern.compile("phs\\d{6}");
    var matcher = pattern.matcher(input);

    if (matcher.find()) {
      return matcher.group();
    } else {
      System.out.println("No effect phs number found in " + input);
      return null;
    }
  }
}
