package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import org.metadatacenter.artifacts.model.core.ElementInstanceArtifact;
import org.metadatacenter.artifacts.model.core.ElementSchemaArtifact;
import org.metadatacenter.artifacts.model.core.TemplateInstanceArtifact;
import org.metadatacenter.artifacts.model.core.TemplateSchemaArtifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunderHandler {
  private final String FUNDER_ELEMENT_NAME = "fundingSources";
  private final String FUNDER_NAME = "funderName";
  private final String FUNDER_ROR = "funderRor";
  private final String OLD_FUNDER_ELEMENT_NAME = "/Data File Funding Sources";
  private final TemplateSchemaArtifact newTemplateSchemaArtifact;
  private final ElementInstanceGenerator elementInstanceGenerator;

  public FunderHandler(TemplateSchemaArtifact newTemplateSchemaArtifact) {
    this.newTemplateSchemaArtifact = newTemplateSchemaArtifact;
    this.elementInstanceGenerator = new ElementInstanceGenerator(newTemplateSchemaArtifact);
  }

  public void patchFunderElementInstances(TemplateInstanceArtifact.Builder builder,
                                          ContributorsHandler contributorsHandler,
                                          Map<String, Integer> elementsInstanceNumber,
                                          Map<String, ElementSchemaArtifact> elementSchemas,
                                          Map<String, List<FieldValues>> values,
                                          Map<String, List<String>> fieldPathMap
                                          ){
    var funderElementSchema = elementSchemas.get(FUNDER_ELEMENT_NAME);
    var instanceNumber = elementsInstanceNumber.getOrDefault(OLD_FUNDER_ELEMENT_NAME, 0);
    var elementInstances = elementInstanceGenerator.buildMultipleElementInstances(values, fieldPathMap, null, funderElementSchema, instanceNumber);
    // update funder ror
    var updatedElementInstances = new ArrayList<ElementInstanceArtifact>();
    var funderRorSchema = funderElementSchema.getFieldSchemaArtifact(FUNDER_ROR);
    for(var instance: elementInstances){
      var funderBuilder = ElementInstanceArtifact.builder(instance);
      funderBuilder.withoutSingleInstanceFieldInstance(FUNDER_ROR);

      var name = instance.singleInstanceFieldInstances().get(FUNDER_NAME).jsonLdValue();
      var id = instance.singleInstanceFieldInstances().get(FUNDER_ROR).jsonLdValue();
      contributorsHandler.buildWithRorAndUei(funderBuilder, name, id, funderRorSchema);
      updatedElementInstances.add(funderBuilder.build());
    }

    builder.withMultiInstanceElementInstances(FUNDER_ELEMENT_NAME, updatedElementInstances);
  }
}
