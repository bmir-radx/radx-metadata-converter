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

public class PublicationHandler {
  private final String DOI = "doi";
  private final String DOI_URI = "https://w3id.org/gdmt/DOI";
  private final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
  private final String PUBLICATION_IDENTIFIER_TYPE = "publicationIdentifierType";
  private final String PUBLICATIONS = "publications";
  private final String OLD_PUBLICATION_ELEMENT_NAME =  "/Data File Related Resources";
  private final ElementInstanceGenerator elementInstanceGenerator;
  private final FieldInstanceGenerator fieldInstanceGenerator;
  private final TemplateSchemaArtifact newTemplateSchemaArtifact;

  public PublicationHandler(TemplateSchemaArtifact newTemplateSchemaArtifact) {
    this.newTemplateSchemaArtifact = newTemplateSchemaArtifact;
    this.elementInstanceGenerator = new ElementInstanceGenerator(newTemplateSchemaArtifact);
    this.fieldInstanceGenerator = new FieldInstanceGenerator();
  }

  public void patchPublications(TemplateInstanceArtifact.Builder builder,
                                Map<String, Integer> elementsInstanceNumber,
                                Map<String, ElementSchemaArtifact> elementSchemas,
                                Map<String, List<FieldValues>> values,
                                Map<String, List<String>> fieldPathMap){
    var publicationElementSchema = elementSchemas.get(PUBLICATIONS);
    var instanceNumber = elementsInstanceNumber.get(OLD_PUBLICATION_ELEMENT_NAME);
    var elementInstances = elementInstanceGenerator.buildMultipleElementInstances(values, fieldPathMap, null, publicationElementSchema, instanceNumber);

    //patch identifier type to DOI if the id is DOI
    var updatedElementInstances = new ArrayList<ElementInstanceArtifact>();
    for(var instance: elementInstances){
      var publicationBuilder = ElementInstanceArtifact.builder(instance);
      var id = instance.singleInstanceFieldInstances().get(PUBLICATION_IDENTIFIER).jsonLdValue();
      if (id.isPresent() && id.get().contains(DOI)){
        try {
          var uri = new URI(DOI_URI);
          var fieldValue = new FieldValues(null, Optional.of(uri), Optional.empty(), Optional.of("DOI"));
          var identifierTypeField = fieldInstanceGenerator.buildSingleControlledTermFieldInstance(fieldValue);
          publicationBuilder.withoutSingleInstanceFieldInstance(PUBLICATION_IDENTIFIER_TYPE);
          publicationBuilder.withSingleInstanceFieldInstance(PUBLICATION_IDENTIFIER_TYPE, identifierTypeField);
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      }
      updatedElementInstances.add(publicationBuilder.build());
    }

    builder.withMultiInstanceElementInstances(PUBLICATIONS, updatedElementInstances);
  }
}
