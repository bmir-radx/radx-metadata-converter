package edu.stanford.bmir.radx.metadata.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.artifacts.model.core.TemplateInstanceArtifact;
import org.metadatacenter.artifacts.model.core.TemplateSchemaArtifact;
import org.metadatacenter.artifacts.model.reader.JsonArtifactReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CedarArtifactGetter {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final JsonArtifactReader jsonArtifactReader = new JsonArtifactReader();

  public static TemplateSchemaArtifact getTemplateSchemaArtifact(Path templatePath) {
    try (var inputStream = Files.newInputStream(templatePath)) {
      var templateNode = objectMapper.readTree(inputStream);
      return jsonArtifactReader.readTemplateSchemaArtifact((ObjectNode) templateNode);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static TemplateInstanceArtifact getInstanceArtifact(Path instancePath){
    try (var inputStream = Files.newInputStream(instancePath)) {
      var templateNode = objectMapper.readTree(inputStream);
      return jsonArtifactReader.readTemplateInstanceArtifact((ObjectNode) templateNode);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
