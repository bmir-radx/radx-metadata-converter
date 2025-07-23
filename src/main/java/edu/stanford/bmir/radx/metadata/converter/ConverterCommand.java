package edu.stanford.bmir.radx.metadata.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.metadatacenter.artifacts.model.core.TemplateInstanceArtifact;
import org.metadatacenter.artifacts.model.renderer.JsonArtifactRenderer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;


@Command(name = "convertMetadataInstances", description = "Convert Metadata Instances in RADx Data Hub to align with update CEDAR template")
public class ConverterCommand implements Callable<Integer> {
  @Option(names = "--t1", description = "Path to the CEDAR template that the metadata instance is currently aligned with", required = true)
  private Path oldTemplatePath;

  @Option(names = "--t2", description = "Path to the updated CEDAR template used to convert and align metadata instances", required = true)
  private Path newTemplatePath;

  @Option(names = "--i", description = "Path to the metadata instances folder that convert to new template schema", required = true)
  private Path instancesPath;

  @Option(names = "--m", description = "Path to the CSV file that map new field path to old field path", required = true)
  private Path fieldPathMapPath;

  @Option(names = "--o", description = "Path to the converted metadata instances", required = true)
  private Path outputPath;
  private final PathMap pathMap = new PathMap();

  @Override
  public Integer call() throws Exception {
    var commandLine = new CommandLine(this);

    // Validate required options
    if (! validateRequiredOptions(commandLine)){
      return 1;
    }

    // Validate file existence
    if(!validateFileExistence()){
      return 1;
    }

    if(!Files.exists(outputPath)){
      Files.createDirectories(outputPath);
    }

    //Read csv file and convert to field path map
    pathMap.readCsvToMap(fieldPathMapPath);

    //Read templates
    var oldTemplateArtifact = CedarArtifactGetter.getTemplateSchemaArtifact(oldTemplatePath);
    var newTemplateArtifact = CedarArtifactGetter.getTemplateSchemaArtifact(newTemplatePath);

    var converter = new Converter(oldTemplateArtifact, newTemplateArtifact, pathMap);
    //Walk through each json file in the folder and convert into new template and save to output path
    try (var files = Files.walk(instancesPath)) {
      files.filter(path -> path.toString().endsWith(".json"))
          .forEach(jsonPath -> {
            try {
              System.err.println("######################### Converting < " + jsonPath + "> ##########################");
              // Read JSON-LD instance
              var instanceArtifact = CedarArtifactGetter.getInstanceArtifact(jsonPath);
              // Convert instance
              var convertedInstance = converter.convert(instanceArtifact);

              var missingValuesValidator = new MissingValuesValidator();
              missingValuesValidator.checkMissingValues(instanceArtifact, convertedInstance);

              //TODO validate converted metadata

              // Resolve output path
              var relativePath = instancesPath.relativize(jsonPath);
              Path outputFileParent = outputPath.resolve(relativePath).getParent();
              if (outputFileParent != null && !Files.exists(outputFileParent)) {
                Files.createDirectories(outputFileParent);
              }
              var outputFile = outputPath.resolve(relativePath);

              // Write result
              writeResult(outputFile, convertedInstance);
              System.err.println("Converted Successfully to " + outputFile);
            } catch (Exception e) {
              System.err.println("Failed to convert: " + jsonPath + " due to " + e.getMessage());
            }
          });
    }

    return 0;
  }

  private boolean validateRequiredOptions(CommandLine commandLine){
    if (oldTemplatePath == null || newTemplatePath == null || instancesPath == null || fieldPathMapPath == null || outputPath == null) {
      System.err.println("Error: All options are required. Please provide values for --t1, --t2, --i, --m, and --o.");
      commandLine.usage(System.err);
      return false;
    }
    return true;
  }

  private boolean validateFileExistence(){
    if (!oldTemplatePath.toFile().exists()) {
      System.err.println("Error: Old template file not found at " + oldTemplatePath);
      return false;
    }
    if (!newTemplatePath.toFile().exists()) {
      System.err.println("Error: New template file not found at " + newTemplatePath);
      return false;
    }
    if (!instancesPath.toFile().exists()) {
      System.err.println("Error: Instances folder not found at " + instancesPath);
      return false;
    }
    if (!fieldPathMapPath.toFile().exists()) {
      System.err.println("Error: Field path map CSV file not found at " + fieldPathMapPath);
      return false;
    }
    return true;
  }

  private void writeResult(Path outputPath, TemplateInstanceArtifact templateInstanceArtifact){
    var mapper = new ObjectMapper();
    var renderer = new JsonArtifactRenderer();
    var instanceNode = renderer.renderTemplateInstanceArtifact(templateInstanceArtifact);
    try {
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), instanceNode);
    } catch (IOException e) {
        System.err.println("Failed to write result to: " + outputPath + " due to: " + e.getMessage());
    }
  }
}
