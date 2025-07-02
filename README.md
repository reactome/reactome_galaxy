# Reactome Galaxy Integration

A command-line utility and Galaxy platform integration for the Reactome pathway analysis service. This project provides both a standalone Java CLI tool and Galaxy tool wrappers for performing pathway analysis using the Reactome database.

## Overview

This application enables users to perform pathway analysis using Reactome's comprehensive biological pathway database. It supports multiple types of analysis including gene set analysis, tissue-specific analysis, and species comparison analysis. The tool can be used both as a standalone command-line application and as integrated tools within the Galaxy bioinformatics platform.

### Key Features

- **Multiple Analysis Types**: Supports gene, tissue, and species pathway analysis
- **Galaxy Platform Ready**: Includes complete Galaxy tool configurations with tests
- **Comprehensive Input**: Accepts various identifier formats and gene lists
- **Comprehensive Output**: Generates detailed pathway analysis results in multiple formats

### Analysis Capabilities

1. **Gene Analysis**: Analyze gene lists against Reactome pathways
2. **Tissue Analysis**: Perform tissue-specific pathway analysis
3. **Species Analysis**: Compare pathways across different species
4. **GSA Analysis**: Gene Set Analysis using the ReactomeGSA R package

### CLI Technical Overview

The Reactome CLI is a simple command line interface which wraps calls to the Reactome web apis written in Kotlin. It provides an easy
entrypoint for workflow engines to call these apis, without needing to worry about constructing input HTTP requests and processing 
outputs.

## Development Setup

### Prerequisites

- **Java Development Kit (JDK)**: Version 11 or higher
- **Apache Maven**: For building the Java CLI
- **1Conda**: For managing Galaxy tool dependencies
- **Python**: For Galaxy integration and testing
- **Planemo**: For Galaxy tool testing and linting

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone https://github.com/reactome/reactome_galaxy.git
   cd reactome_galaxy
   ```

2. **Build the Java CLI**:
   ```bash
   mvn package -Dconda.location=/path/to/your/miniconda
   ```

3. **Test the CLI build**:
   ```bash
   java -jar target/reactome-jar-with-dependencies.jar \
     genes \
     --reactome_url https://release.reactome.org \
     --identifiers_file src/test/resources/uniprot_input.txt \
     --pathways pathways_output.csv
   ```

### Development Workflow

1. **Make an issue and branch** the branch correspond to the issue number.
2. **Make code changes** to the Java source files or Galaxy tool configurations
3. **Test and build** using Maven (automatically runs all tests and linting):
   ```bash
   mvn package -Dconda.location=/path/to/your/miniconda
   ```
4. **Verify results** the build will fail if any tests or linting checks fail
5. **Commit changes** once all automated checks pass
6. **Create a PR** and have someone review the changes
7. **Merge the PR** 

### Testing

The project includes comprehensive testing for both components, integrated into the Maven build process:

#### Integrated Testing
All testing (Java CLI and Galaxy tools) is automated through Maven:

```bash
# This single command runs:
# - Java unit tests
# - Planemo lint for both Galaxy tools
# - Planemo test for both Galaxy tools
mvn package -Dconda.location=/path/to/your/miniconda
```

The Maven build automatically executes:
- Java CLI unit tests
- `planemo lint` on `galaxy/local_tools/reactome`
- `planemo test` on `galaxy/local_tools/reactome`
- `planemo lint` on `galaxy/local_tools/reactome-gsa`
- `planemo test` on `galaxy/local_tools/reactome-gsa`

### Project Structure

```
reactome_galaxy/
├── src/                          # Java source code
├── target/                       # Maven build output
├── galaxy/
│   └── local_tools/
│       ├── reactome/            # Main Reactome Galaxy tool
│       └── reactome-gsa/        # ReactomeGSA R client tool
├── src/test/resources/          # Test data files
└── pom.xml                      # Maven configuration
```

## Usage

### Command Line Interface

The CLI supports multiple analysis types with comprehensive help documentation:

```bash
# View main help
java -jar target/reactome-jar-with-dependencies.jar --help

# View help for specific analysis type
java -jar target/reactome-jar-with-dependencies.jar genes --help
```

#### Example Commands

**Gene Analysis**:
```bash
java -jar target/reactome-jar-with-dependencies.jar \
  genes \
  --reactome_url https://release.reactome.org \
  --identifiers_file input_genes.txt \
  --pathways results.csv
```

**Tissue Analysis**:
```bash
java -jar target/reactome-jar-with-dependencies.jar \
  tissue \
  --reactome_url https://release.reactome.org \
  --identifiers_file src/test/resources/uniprot_input.txt \
  --tissue liver \
  --pathways tissue_results.csv
```

### Galaxy Integration

The project provides two Galaxy tools:

1. **reactome**: Java CLI-based tool for gene, tissue, and species analysis
2. **reactome-gsa**: R-based tool using the ReactomeGSA package for advanced gene set analysis

Both tools include complete Galaxy tool definitions with parameter validation, help documentation, and test cases.

#### Running Tools in Galaxy

Once installed in a Galaxy instance, the tools can be accessed through the Galaxy interface. Please refer to the
help provided in the Galaxy tool for documentation on running the tool and the input and output data supported.

## Distribution

### Bioconda Package

The Reactome Galaxy tools are distributed via bioconda, which handles dependency management for the Galaxy platform. The tools automatically install required dependencies when deployed in a Galaxy instance.

The conda packaging and integration with galaxy is tested as part of the integrated tests above, but to actually release to bioconda the recipes must
be added to the [central bioconda recipe repo](https://github.com/bioconda/bioconda-recipes). Follow these
steps to release to bioconda:
1. Read and get setup with the bioconda recipe repo using their [contribution workflow](https://bioconda.github.io/contributor/workflow.html)
2. Run `mvn clean package -Dconda.location=/path/to/your/miniconda` and ensure all tests pass and a jar is built.
3. Create a release in GitHub from the [release page](https://github.com/reactome/reactome_galaxy/releases). Add the local `[reactome-jar-with-dependencies.jar](target%2Freactome-jar-with-dependencies.jar)` you've built to the release, named of the format `reacome-v{new_version}.jar`
4. Update `bioconda-recipes/recipes/reactome-cli` with your new version, and follow their PR workflow to get it merged and released.
5. When complete you can now release a new version of the Galaxy XML to the toolshed. 

**Installation in Galaxy**:
- Tools are installed through Galaxy's tool installation interface
- Dependencies are automatically resolved via Conda
- No manual dependency installation required

### Galaxy ToolShed

The tools are available through the Galaxy ToolShed, the official repository for Galaxy tools:

**ToolShed Distribution**:

Both tools are packages and distributed together in the [central toolshed](https://toolshed.g2.bx.psu.edu/). Currently, distribution is a manual process. 
New revisions can be released to the toolshed by following the following steps: 
1. Update the tool versions in [reactome.xml](galaxy/local_tools/reactome/reactome.xml) and [reactome_gsa.xml](galaxy/local_tools/reactome-gsa/reactome-gsa.xml)
2. If the tool requires a new version of CLI, you must first release to bioconda by following the bioconda packaging above. Then update the dependency in [reactome.xml](galaxy/local_tools/reactome/reactome.xml)
3. `planemo shed_update --shed_target toolshed` from the `galaxy/local_tools` directory.

### Installation Notes for Galaxy Administrators

When installing the Reactome tools in a Galaxy instance:

1. **HTML Rendering**: Ensure tools are configured for "HTML Rendered" output in Admin → Manage Allowlist
2. **Dependencies**: Conda must be properly configured for automatic dependency resolution
3. **Testing**: Run the included test cases to verify proper installation

## Support

For any questions or issues with running these tools contact [Reactome HelpDesk](mailto:help@reactome.org)

## Related Projects

- [ReactomeGSA](https://github.com/reactome/ReactomeGSA): R package for gene set analysis
- [Reactome](https://reactome.org): Main Reactome pathway database
- [Galaxy Project](https://galaxyproject.org): Open source bioinformatics platform