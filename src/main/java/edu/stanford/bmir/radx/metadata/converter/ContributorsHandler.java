package edu.stanford.bmir.radx.metadata.converter;

import edu.stanford.bmir.radx.metadata.validator.lib.FieldValues;
import edu.stanford.bmir.radx.rad.metadata.compiler.ContextGenerator;
import edu.stanford.bmir.radx.rad.metadata.compiler.IdGenerator;
import org.metadatacenter.artifacts.model.core.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ContributorsHandler {
  private final String INDIVIDUAL_CONTRIBUTORS = "individualContributors";
  private final String ORGANIZATIONAL_CONTRIBUTORS = "organizationalContributors";
  private final String OLD_CREATORS_PATH = "/Data File Creators";
  private final String OLD_CONTRIBUTORS_PATH = "/Data File Contributors";
  private final String OLD_CREATOR_TYPE_PATH = OLD_CREATORS_PATH + "/Creator Type";
  private final String OLD_CONTRIBUTOR_TYPE_PATH = OLD_CONTRIBUTORS_PATH + "/Contributor Type";
  private final String OLD_CREATOR_GIVEN_NAME_PATH = OLD_CREATORS_PATH + "/Creator Given Name";
  private final String OLD_CONTRIBUTOR_GIVEN_NAME_PATH = OLD_CONTRIBUTORS_PATH + "/Contributor Given Name";
  private final String INDIVIDUAL_ROLES = "contributorRoles";
  private final String ORGANIZATION_ROLES = "organizationRoles";
  private final String CONTRIBUTOR_NAME = "contributorName";
  private final String ORGANIZATION_NAME = "organizationName";
  private final String CONTRIBUTOR_ORCID = "contributorOrcid";
  private final String NEW_INDIVIDUAL_ROLES_PATH = "/" + INDIVIDUAL_CONTRIBUTORS + "/contributorRoles";
  private final String NEW_ORGANIZATION_ROLES_PATH = "/" + ORGANIZATIONAL_CONTRIBUTORS + "/organizationRoles";
  private final String PERSON = "Person";
  private final String ORGANIZATION = "Organization";
  private final String CREATOR = "creator";
  private final String CONTRIBUTOR = "contributor";
  private final String ORCID_PREFIX = "https://orcid.org/";
  private final String ROR_PREFIX = "https://ror.org/";
  private final String CONTRIBUTOR_AFFILIATIONS = "contributorAffiliations";
  private final Map<String, List<String>> toCreatorPath = new HashMap<>();
  private final Map<String, List<String>> toContributorPath = new HashMap<>();
  private final ElementInstanceGenerator elementInstanceGenerator;
  private final TemplateSchemaArtifact newTemplateSchemaArtifact;
  private final TemplateSchemaArtifact oldTemplateSchemaArtifact;
  private final FieldInstanceGenerator fieldInstanceGenerator;
  private Map<String, String> orgMeta;
  private Map<String, Map<HeaderName, String>> personMeta;

  public ContributorsHandler(TemplateSchemaArtifact newtemplateSchemaArtifact, TemplateSchemaArtifact oldTemplateSchemaArtifact) {
    this.newTemplateSchemaArtifact = newtemplateSchemaArtifact;
    this.elementInstanceGenerator = new ElementInstanceGenerator(newtemplateSchemaArtifact);
    this.oldTemplateSchemaArtifact = oldTemplateSchemaArtifact;
    this.fieldInstanceGenerator = new FieldInstanceGenerator();
    //retrieve manual reviewed ror and orcid
    ExcelReader.readCreatorsMetadata();
    personMeta = ExcelReader.getPersonMeta();
    orgMeta = ExcelReader.getOrgMeta();
  }

  public void buildIndivAndOrgContributors(Map<String, Integer> elementsInstanceNumber,
                                           Map<String, ElementSchemaArtifact> elementSchemas,
                                           Map<String, List<FieldValues>> values,
                                           Map<String, List<String>> fieldPathMap,
                                           TemplateInstanceArtifact.Builder builder){
    var indivContributorSchema = elementSchemas.get(INDIVIDUAL_CONTRIBUTORS);
    var orgContributorSchema = elementSchemas.get(ORGANIZATIONAL_CONTRIBUTORS);

    var creatorInstancesNumber = elementsInstanceNumber.get(OLD_CREATORS_PATH);
    var contributorInstancesNumber = elementsInstanceNumber.get(OLD_CONTRIBUTORS_PATH);

    distributePath(fieldPathMap);

    //build individual contributors
    var indivContributors = buildContributorInstances(PERSON, creatorInstancesNumber, contributorInstancesNumber, values, indivContributorSchema);

    //merge duplicate instances
    var uniqueIndivContributors = mergeDuplicateContributorElementInstance(indivContributors, indivContributorSchema);

    //correct given and family names
    var correctedIndicContributors = fixIndividualContributorName(uniqueIndivContributors, indivContributorSchema);

    builder.withMultiInstanceElementInstances(INDIVIDUAL_CONTRIBUTORS, correctedIndicContributors);

    //build organization contributors
    var orgContributors = buildContributorInstances(ORGANIZATION, creatorInstancesNumber, contributorInstancesNumber, values, orgContributorSchema);

    // merge duplicate instances
    var uniqueOrgContributors = mergeDuplicateContributorElementInstance(orgContributors, orgContributorSchema);

    builder.withMultiInstanceElementInstances(ORGANIZATIONAL_CONTRIBUTORS, uniqueOrgContributors);
  }

  private List<ElementInstanceArtifact> buildContributorInstances(String contributorType,
                                                                  Integer creatorInstanceNumber,
                                                                  Integer contributorInstanceNumber,
                                                                  Map<String, List<FieldValues>> values,
                                                                  ElementSchemaArtifact specificContributorSchema){

    var instances = new ArrayList<ElementInstanceArtifact>();

    //Deal with Creator element instances, only add element instance that creator type equals target contributor type
    for(int i=0; i< creatorInstanceNumber; i++){
      var currentContributorType = getCreatorOrContributorType(CREATOR, i, values);
      if(currentContributorType.equals(contributorType) && !isEmptyContributorInstance(CREATOR, i, values)){
        var contributorInstance = buildSingleContributorInstance(specificContributorSchema, toCreatorPath, values, i);
        //add ror and orcid
        var updatedContributorInstance = addIdentifier(contributorInstance, specificContributorSchema, values, i, OLD_CREATORS_PATH);
        //fix multi-roles
        updatedContributorInstance = fixMultiRoles(updatedContributorInstance, specificContributorSchema, contributorType);
        instances.add(updatedContributorInstance);
      }

    }

    //Deal with Contributor element instances, only add element instance that contributor type equals target contributor type
    for(int i=0; i< contributorInstanceNumber; i++){
      var currentContributorType = getCreatorOrContributorType(CONTRIBUTOR, i, values);
      if(currentContributorType.equals(contributorType) && !isEmptyContributorInstance(CONTRIBUTOR, i, values)){
        var contributorInstance = buildSingleContributorInstance(specificContributorSchema, toContributorPath, values, i);
        //add ror and orcid
        var updatedContributorInstance = addIdentifier(contributorInstance, specificContributorSchema, values, i, OLD_CONTRIBUTORS_PATH);
        //fix multi-roles
        updatedContributorInstance = fixMultiRoles(updatedContributorInstance, specificContributorSchema, contributorType);
        instances.add(updatedContributorInstance);
      }
    }
    return instances;


  }

  private ElementInstanceArtifact buildSingleContributorInstance(ElementSchemaArtifact contributorSchema,
                                                                 Map<String, List<String>> specifiedFieldPathMap,
                                                                 Map<String, List<FieldValues>> values,
                                                                 Integer i){
    return elementInstanceGenerator.buildSingleElementInstances(values, specifiedFieldPathMap, null, contributorSchema, i);
  }


  private void distributePath(Map<String, List<String>> fieldPathMap){
    for(var entry: fieldPathMap.entrySet()){
      var newPath = entry.getKey();
      var oldPaths = entry.getValue();
      if (newPath.toLowerCase().contains("contributors")){
        for(var path: oldPaths){
          if (path.toLowerCase().contains("creator")){
            toCreatorPath.put(newPath, List.of(path));
          } else{
            toContributorPath.put(newPath, List.of(path));
          }
        }
      }
    }
  }

  private String getCreatorOrContributorType(String oldElementName, Integer i, Map<String, List<FieldValues>> values) {
    String creatorOrContributorTypePath = null;
    String creatorOrContributorGivenNamePath = null;
    if (oldElementName.equals(CREATOR)) {
      creatorOrContributorTypePath = OLD_CREATOR_TYPE_PATH;
      creatorOrContributorGivenNamePath = OLD_CREATOR_GIVEN_NAME_PATH;
    } else {
      creatorOrContributorTypePath = OLD_CONTRIBUTOR_TYPE_PATH;
      creatorOrContributorGivenNamePath = OLD_CONTRIBUTOR_GIVEN_NAME_PATH;
    }

    var creatorOrContributorTypeValue = values.get(creatorOrContributorTypePath).get(i);
    var creatorOrContributorGivenName = values.get(creatorOrContributorGivenNamePath).get(i);

    if (creatorOrContributorTypeValue.label().isPresent()) {
      if (creatorOrContributorTypeValue.label().get().equals(PERSON)) {
        return PERSON;
      } else {
        return ORGANIZATION;
      }
    }

    if (creatorOrContributorGivenName.jsonLdValue().isPresent()) {
      return PERSON;
    }

    System.err.println("Both Contributor Type and Contributor Given Name are missing");
    return ORGANIZATION;
  }

  private boolean isEmptyContributorInstance(String elementName, Integer instanceIndex, Map<String, List<FieldValues>> values){
    String elementPath = null;
    if(elementName.equals(CREATOR)){
      elementPath = OLD_CREATORS_PATH;
    } else{
      elementPath = OLD_CONTRIBUTORS_PATH;
    }

    var oldElementSchema = oldTemplateSchemaArtifact.getElementSchemaArtifact(elementPath.substring(1));
    var childFieldSchema = oldElementSchema.fieldSchemas();

    for(var entry: childFieldSchema.entrySet()){
      var childName = entry.getKey();
      var currentOldPath = elementPath + "/" + childName;
      if(currentOldPath.equals(OLD_CREATOR_TYPE_PATH) || currentOldPath.equals(OLD_CONTRIBUTOR_TYPE_PATH)){
        continue;
      }
      var value = values.get(currentOldPath).get(instanceIndex);
      if(!isEmptyFieldValue(value)){
        return false;
      }
    }

    return true;
  }

  private boolean isEmptyFieldValue(FieldValues fieldValues){
    return fieldValues.jsonLdValue().isEmpty() && fieldValues.label().isEmpty() && fieldValues.jsonLdId().isEmpty() && fieldValues.jsonLdTypes().isEmpty();
  }

  private List<ElementInstanceArtifact> mergeDuplicateContributorElementInstance(List<ElementInstanceArtifact> instances, ElementSchemaArtifact elementSchemaArtifact){
    Map<String, List<ElementInstanceArtifact>> uniqueByName = new HashMap<>();

    //build uniqueByName map
    for(var instance: instances){
      var childSingleFieldInstances = instance.singleInstanceFieldInstances();
      for(var entry: childSingleFieldInstances.entrySet()){
        var childFieldName = entry.getKey();
        if(childFieldName.equals(CONTRIBUTOR_NAME) || childFieldName.equals(ORGANIZATION_NAME)){
          var name = entry.getValue().jsonLdValue();
          name.ifPresent(s -> uniqueByName.computeIfAbsent(s, k -> new ArrayList<>()).add(instance));
        }
      }
    }

    //merge two element instance, and add unique element instance into the list
    var uniqueElementInstance = new ArrayList<ElementInstanceArtifact>();
    var elementName = elementSchemaArtifact.name();
    String rolesFieldName = null;
    if(elementName.toLowerCase().contains("individual")){
      rolesFieldName = INDIVIDUAL_ROLES;
    } else{
      rolesFieldName = ORGANIZATION_ROLES;
    }
    for(var entry: uniqueByName.entrySet()){
      var instanceList = entry.getValue();
      if (instanceList.size() > 1){
        if(instanceList.size() > 2){
          System.err.println(elementSchemaArtifact.name() + "- " + entry.getKey() + " has multiple duplicates");
        }

        //read the first and second controlled terms for role
        var firstInstance = instanceList.get(0);
        var secondInstance = instanceList.get(1);
        var builder = ElementInstanceArtifact.builder(firstInstance);

        var firstRoles = firstInstance.multiInstanceFieldInstances().get(rolesFieldName);
        var secondRoles = secondInstance.multiInstanceFieldInstances().get(rolesFieldName);
        var mergedRoles = new ArrayList<>(firstRoles);
        mergedRoles.addAll(secondRoles);

        // merge and add "Creator" role
        var rolesCollection = new ArrayList<FieldValues>();
        for(var roleArtifact: mergedRoles){
          var roleValue = new FieldValues(
              roleArtifact.jsonLdTypes(),
              roleArtifact.jsonLdId(),
              roleArtifact.jsonLdValue(),
              roleArtifact.label());
          rolesCollection.add(roleValue);
        }
//        try {
//          rolesCollection.add(new FieldValues(
//              new ArrayList<>(),
//              Optional.of(new URI("https://w3id.org/gdmt/Creator")),
//              Optional.empty(),
//              Optional.of("Creator")
//          ));
//        } catch (URISyntaxException e) {
//          throw new RuntimeException(e);
//        }

        // without this field and rebuild this field
        builder.withoutMultiInstanceFieldInstances(rolesFieldName);

        // update it in the map -> list only has this one unique element instance
        var newRolesArtifacts = fieldInstanceGenerator.buildMultiControlledTermFieldsInstance(rolesCollection);
        builder.withMultiInstanceFieldInstances(rolesFieldName, newRolesArtifacts);

        // add to the unique list
        uniqueElementInstance.add(builder.build());
      } else{
        uniqueElementInstance.add(instanceList.get(0));
      }
    }
    return uniqueElementInstance;
  }

  private ElementInstanceArtifact addIdentifier(ElementInstanceArtifact elementInstanceArtifact,
                                                ElementSchemaArtifact specificContributorSchema,
                                                Map<String, List<FieldValues>> values,
                                                Integer i,
                                                String oldElementPath){
    var builder = ElementInstanceArtifact.builder(elementInstanceArtifact);
    String oldOrcidIdPath = null;
    String oldOrcidNamePath = null;
    String oldAffiliationNamePath = null;
    String oldAffiliationIdPath = null;
    if(oldElementPath.equals(OLD_CREATORS_PATH)){
      oldOrcidIdPath = "/Data File Creators/Creator Identifier";
      oldOrcidNamePath = "/Data File Creators/Creator Name";
      oldAffiliationNamePath = "/Data File Creators/Creator Affiliation";
      oldAffiliationIdPath = "/Data File Creators/Creator Affiliation Identifier";
    } else{
      oldOrcidIdPath = "/Data File Contributors/Contributor Identifier";
      oldOrcidNamePath = "/Data File Contributors/Contributor Name";
      oldAffiliationNamePath = "/Data File Contributors/Contributor Affiliation";
      oldAffiliationIdPath = "/Data File Contributors/Contributor Affiliation Identifier";
    }

    if(specificContributorSchema.name().equals(INDIVIDUAL_CONTRIBUTORS)){
      //add individual contributor ORCID fields
      var orcidId = values.get(oldOrcidIdPath).get(i).jsonLdValue();
      var orcidName = values.get(oldOrcidNamePath).get(i).jsonLdValue();
      //add manual refined orcid id if empty
      if(orcidId.isEmpty() && orcidName.isPresent()){
        var id = personMeta.get(orcidName.get()).get(HeaderName.ORCID_ID);
        orcidId = Optional.of(id);
      }
      orcidId.ifPresent(s -> buildWithOrcid(builder, orcidName, s, CONTRIBUTOR_ORCID));

      //add individual contributor child element : affiliation ROR|UEI fields
      var affiliationName = values.get(oldAffiliationNamePath).get(i).jsonLdValue();
      var rorOrUei = values.get(oldAffiliationIdPath).get(i).jsonLdValue();

      if(affiliationName.isPresent() || rorOrUei.isPresent()){
        var affiliationSchemaArtifact = specificContributorSchema.elementSchemas().get(CONTRIBUTOR_AFFILIATIONS);
        var affiliationBuilder = ElementInstanceArtifact.builder();
        // build name
        var affiliationNameSchema = affiliationSchemaArtifact.getFieldSchemaArtifact("contributorAffiliationName");
        buildWithName(affiliationBuilder, affiliationName, affiliationNameSchema);

        //build uei
        var affiliationUeiSchema = affiliationSchemaArtifact.getFieldSchemaArtifact("contributorAffiliationUei");
        buildWithRorAndUei(affiliationBuilder, affiliationName, rorOrUei, affiliationUeiSchema);

        //build ror
        //add manual refined ror
        if(!isRorId(rorOrUei) && orcidName.isPresent()){
          var ror = personMeta.get(orcidName.get()).get(HeaderName.CREATOR_AFFILIATION_ROR);
          rorOrUei = Optional.of(ror);
        }
        var affiliationRorSchema = affiliationSchemaArtifact.getFieldSchemaArtifact("contributorAffiliationRor");
        buildWithRorAndUei(affiliationBuilder, affiliationName, rorOrUei, affiliationRorSchema);

        //add @id and context
        ContextGenerator.generateElementInstanceContext(affiliationSchemaArtifact, affiliationBuilder);
        try {
          IdGenerator.generateElementId(affiliationBuilder);
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }

        //add Affiliation Child Element instance
        builder.withoutMultiInstanceElementInstances("contributorAffiliations");
        builder.withMultiInstanceElementInstances("contributorAffiliations", List.of(affiliationBuilder.build()));
      }
    } else{
      //add organization contributor ROR|UEI fields
      // build uei
      String ueiFieldName = "organizationUei";
      var rorOrUei = values.get(oldOrcidIdPath).get(i).jsonLdValue();
      var orgName = values.get(oldOrcidNamePath).get(i).jsonLdValue();
      var organizationUeiSchema = specificContributorSchema.getFieldSchemaArtifact(ueiFieldName);
      if(elementInstanceArtifact.singleInstanceFieldInstances().containsKey(ueiFieldName)){
        builder.withoutSingleInstanceFieldInstance(ueiFieldName);
      }
      buildWithRorAndUei(builder, orgName, rorOrUei, organizationUeiSchema);

      //build ror
      String rorFieldName = "organizationRor";
      var organizationRorSchema = specificContributorSchema.getFieldSchemaArtifact(rorFieldName);
      if(elementInstanceArtifact.singleInstanceFieldInstances().containsKey(rorFieldName)){
        builder.withoutSingleInstanceFieldInstance(rorFieldName);
      }
      //add manual refined ror if empty
      if(!isRorId(rorOrUei) && orgName.isPresent()){
        var ror = orgMeta.get(orgName.get());
        rorOrUei = Optional.of(ror);
      }
      buildWithRorAndUei(builder, orgName, rorOrUei, organizationRorSchema);
    }

    return builder.build();
  }

  private List<ElementInstanceArtifact> fixIndividualContributorName(List<ElementInstanceArtifact> elementInstanceArtifacts, ElementSchemaArtifact elementSchemaArtifact){
    String contributorGivenName = "contributorGivenName";
    String contributorFamilyName = "contributorFamilyName";
    String contributorFullName = "contributorName";
    List<ElementInstanceArtifact> updatedElementInstances = new ArrayList<>();

    for(var elementInstanceArtifact: elementInstanceArtifacts){
      var fullName = elementInstanceArtifact.singleInstanceFieldInstances().get(contributorFullName).jsonLdValue();
      var givenNameFieldSchema = elementSchemaArtifact.getFieldSchemaArtifact(contributorGivenName);
      var familyNameFieldSchema = elementSchemaArtifact.getFieldSchemaArtifact(contributorFamilyName);
      if(fullName.isPresent()){
        var fullNameString = fullName.get();
        if(personMeta.containsKey(fullNameString)){
          var givenNameCleaned = personMeta.get(fullNameString.trim()).get(HeaderName.CREATOR_GIVEN_NAME_CLEANED);
          var familyNameCleaned = personMeta.get(fullNameString.trim()).get(HeaderName.CREATOR_FAMILY_NAME_CLEANED);

          var givenNameFieldValues = new FieldValues(null, Optional.empty(), Optional.of(givenNameCleaned), Optional.empty());
          var givenNameArtifact = fieldInstanceGenerator.buildSingleInstance(List.of(givenNameFieldValues), givenNameFieldSchema);
          var familyNameFieldValues = new FieldValues(null, Optional.empty(), Optional.of(familyNameCleaned), Optional.empty());
          var familyNameArtifact = fieldInstanceGenerator.buildSingleInstance(List.of(familyNameFieldValues), familyNameFieldSchema);

          var builder = ElementInstanceArtifact.builder(elementInstanceArtifact);
          builder.withoutSingleInstanceFieldInstance(contributorGivenName);
          builder.withoutSingleInstanceFieldInstance(contributorFamilyName);

          builder.withSingleInstanceFieldInstance(contributorGivenName, givenNameArtifact);
          builder.withSingleInstanceFieldInstance(contributorFamilyName, familyNameArtifact);

          elementInstanceArtifact = builder.build();
        } else{
          System.err.println(fullNameString + " is missing from manual review google spreadsheet");
        }
      }
      updatedElementInstances.add(elementInstanceArtifact);
    }

    return updatedElementInstances;
  }

  private void buildWithOrcid(ElementInstanceArtifact.Builder builder, Optional<String> contributorName, String id, String filedName){
    var uri = getIdUri(id, ORCID_PREFIX);
    var fieldValue = new FieldValues(new ArrayList<>(), Optional.of(uri), Optional.empty(), contributorName);
    var orcidFieldInstanceArtifact = fieldInstanceGenerator.buildSingleControlledTermFieldInstance(fieldValue);
    builder.withoutSingleInstanceFieldInstance(filedName);
    builder.withSingleInstanceFieldInstance(filedName, orcidFieldInstanceArtifact);

  }

  private void buildWithName(ElementInstanceArtifact.Builder builder, Optional<String> name, FieldSchemaArtifact fieldSchemaArtifact){
    var fieldValues = new FieldValues(new ArrayList<>(), Optional.empty(), name, Optional.empty());
    var fieldInstanceArtifact = fieldInstanceGenerator.buildSingleInstance(List.of(fieldValues), fieldSchemaArtifact);
    builder.withSingleInstanceFieldInstance(fieldSchemaArtifact.name(), fieldInstanceArtifact);
  }

  public void buildWithRorAndUei(ElementInstanceArtifact.Builder builder, Optional<String> name, Optional<String> id, FieldSchemaArtifact fieldSchemaArtifact) {
    var fieldName = fieldSchemaArtifact.name();
    boolean isRorField = fieldName.toLowerCase().contains("ror");
    boolean hasFilledId = id.isPresent();
    boolean isRorId = isRorId(id);

    if (isRorField && isRorId) { // if it is ror field and id is ror
      var idUri = getIdUri(id.get(), ROR_PREFIX);
      var fieldValue = new FieldValues(new ArrayList<>(), Optional.of(idUri), Optional.empty(), name);
      var fieldInstanceArtifact = fieldInstanceGenerator.buildSingleRorFieldInstance(fieldValue);
      builder.withSingleInstanceFieldInstance(fieldName, fieldInstanceArtifact);
    } else if (!isRorField && hasFilledId && !isRorId) { // if it is uei field and id is uei
      try {
        var idUri = new URI(id.get());
        var fieldValue = new FieldValues(new ArrayList<>(), Optional.of(idUri), Optional.empty(), name);
        var fieldInstanceArtifact = fieldInstanceGenerator.buildSingleInstance(List.of(fieldValue), fieldSchemaArtifact);
        builder.withSingleInstanceFieldInstance(fieldName, fieldInstanceArtifact);
      } catch (URISyntaxException e) {
        System.err.println(id.get() + " is not a valid URI");
      }
    }

    else {
      var fieldInstanceArtifact = fieldInstanceGenerator.buildSingleInstance(null, fieldSchemaArtifact);
      builder.withSingleInstanceFieldInstance(fieldName, fieldInstanceArtifact);
    }
  }

  private boolean isRorId(Optional<String> id){
    return id.isPresent() && id.get().contains("ror");
  }

  private URI getIdUri(String id, String prefix){
    try{
      if(id.contains(prefix)){
        return new URI(id);
      } else{
        var trimmedId = id.replaceAll("ror:", "");
        return new URI(prefix + trimmedId);
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private ElementInstanceArtifact fixMultiRoles(ElementInstanceArtifact elementInstanceArtifact,
                                                ElementSchemaArtifact specificContributorSchema,
                                                String contributorType){
    String roleFieldName = null;
    if(contributorType.equals(ORGANIZATION)){
      roleFieldName = "organizationRoles";
    } else{
      roleFieldName = "contributorRoles";
    }

    var piLabelMap = new HashMap<String, String>();
    piLabelMap.put("data-PI", "https://w3id.org/gdmt/DataPI");
    piLabelMap.put("contact-PI", "https://w3id.org/gdmt/ContactPI");

    var builder = ElementInstanceArtifact.builder(elementInstanceArtifact);
    var fieldInstances = elementInstanceArtifact.multiInstanceFieldInstances().get(roleFieldName);

    var fieldArtifact = specificContributorSchema.getFieldSchemaArtifact(roleFieldName);

    var updatedRoleInstances = new ArrayList<FieldInstanceArtifact>();
    for(var fieldInstance: fieldInstances){
      var value = fieldInstance.label();
      if(value.isPresent()){
        if (value.get().contains("|")){//meaning multiple label from rad
          var splitLabels = value.get().split("\\|");
          for(var label: splitLabels){
            if(piLabelMap.containsKey(label)){
              try {
                var iri = new URI(piLabelMap.get(label));
                var fieldValue = new FieldValues(null, Optional.of(iri), null, Optional.of(label));
                var updatedInstance = fieldInstanceGenerator.buildSingleInstance(List.of(fieldValue), fieldArtifact);
                updatedRoleInstances.add(updatedInstance);
              } catch (URISyntaxException e) {
                throw new RuntimeException(e);
              }

            } else{
              System.err.println(label + " can't find corresponding IRI");
            }
          }
        } else{ //valid label
          updatedRoleInstances.add(fieldInstance);
        }
      }
    }
    builder.withoutMultiInstanceFieldInstances(roleFieldName);
    builder.withMultiInstanceFieldInstances(roleFieldName, updatedRoleInstances);
    return builder.build();
  }
}
