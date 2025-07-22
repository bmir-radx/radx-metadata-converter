# RADx Metadata Converter

A command line application that converts metadata instances in RADx Data Hub to align with updated CEDAR templates.

## Requirements

- Java 17 or higher

## Building Dependencies

Before building the application, you must first clone and build the following dependencies locally on the `develop` branch, as they contain the latest updates required for this application:

### 1. Clone and build cedar-parent
```bash
git clone https://github.com/metadatacenter/cedar-parent.git
cd cedar-parent
git checkout develop
mvn clean install
```

### 2. Clone and build cedar-artifact-library
```bash
git clone https://github.com/metadatacenter/cedar-artifact-library
cd cedar-artifact-library
git checkout develop
mvn clean install
```

### 3. Clone and build radx-rad-metadata-compiler
```bash
git clone https://github.com/bmir-radx/radx-rad-metadata-compiler
cd radx-rad-metadata-compiler
git checkout develop
mvn clean install
```

## Building the Application

Build the application:

```bash
mvn clean install
```

## Running the Application

### Option 1: Using the convenience script

```bash
./run.sh [arguments]
```

### Option 2: Direct JAR execution

```bash
java -jar target/radx-metadata-converter-0.0.1-SNAPSHOT.jar [arguments]
```

## Usage

```
Usage: convertMetadataInstances --i=<instancesPath> --m=<fieldPathMapPath>
                                --o=<outputPath> --t1=<oldTemplatePath>
                                --t2=<newTemplatePath>

Convert Metadata Instances in RADx Data Hub to align with update CEDAR template

Required options:
  --t1=<oldTemplatePath>    Path to the CEDAR template that the metadata
                              instance is currently aligned with
  --t2=<newTemplatePath>    Path to the updated CEDAR template used to convert
                              and align metadata instances
  --i=<instancesPath>       Path to the metadata instances folder that convert
                              to new template schema
  --m=<fieldPathMapPath>    Path to the CSV file that map new field path to old
                              field path
  --o=<outputPath>          Path to the converted metadata instances
```

### Example

```bash
./run.sh \
  --t1=/path/to/old/template.json \
  --t2=/path/to/new/template.json \
  --i=/path/to/instances/folder \
  --m=/path/to/field/mapping.csv \
  --o=/path/to/output/folder
```

## Help

Display help information:

```bash
./run.sh --help
```
