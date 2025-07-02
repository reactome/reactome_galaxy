# Reactome Galaxy Integration

A command-line utility and Galaxy platform integration for the Reactome pathway analysis service. This project provides both a standalone Java CLI tool and Galaxy tool wrappers for performing pathway analysis using the Reactome database.

## Overview

This application enables users to perform pathway analysis using Reactome's comprehensive biological pathway database. It supports multiple types of analysis including gene set analysis, tissue-specific analysis, and species comparison analysis. The tool can be used both as a standalone command-line application and as integrated tools within the Galaxy bioinformatics platform.

### Key Features

- **Multiple Analysis Types**: Supports gene, tissue, and species pathway analysis
- **Dual Integration**: Provides both Java CLI and R-based analysis tools
- **Galaxy Platform Ready**: Includes complete Galaxy tool configurations with tests
- **Flexible Input**: Accepts various identifier formats and gene lists
- **Comprehensive Output**: Generates detailed pathway analysis results in multiple formats

### Analysis Capabilities

1. **Gene Analysis**: Analyze gene lists against Reactome pathways
2. **Tissue Analysis**: Perform tissue-specific pathway analysis
3. **Species Analysis**: Compare pathways across different species
4. **GSA Analysis**: Gene Set Analysis using the ReactomeGSA R package

## Development Setup

### Prerequisites

- **Java Development Kit (JDK)**: Version 11 or higher
- **Apache Maven**: For building the Java CLI
- **Conda**: For managing Galaxy tool dependencies
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

1. **Make code changes** to the Java source files or Galaxy tool configurations
2. **Test and build** using Maven (automatically runs all tests and linting):
   ```bash
   mvn package -Dconda.location=/path/to/your/miniconda
   ```
3. **Verify results** - the build will fail if any tests or linting checks fail
4. **Commit changes** once all automated checks pass

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

Once installed in a Galaxy instance, the tools can be accessed through the Galaxy interface:

**Reactome Analysis Tool**:
1. Navigate to the **Tools** panel in Galaxy
2. Look for **Reactome** in the tool list (typically under pathway analysis or similar category)
3. Select your analysis type:
    - **Gene Analysis**: Upload a gene list file or paste identifiers
    - **Tissue Analysis**: Provide gene list and select target tissue
    - **Species Analysis**: Compare pathways across species
4. Configure parameters:
    - **Reactome URL**: Usually pre-set to the current release
    - **Input format**: Select appropriate identifier type (UniProt, Ensembl, etc.)
    - **Output options**: Choose desired result formats
5. **Execute** the analysis
6. Results will appear in your Galaxy history as downloadable files

**ReactomeGSA Tool**:
1. Find **ReactomeGSA** in the Tools panel
2. Upload your expression data or gene sets
3. Configure GSA-specific parameters:
    - **Analysis method**: Choose statistical approach
    - **Species**: Select organism
    - **Pathway database**: Configure Reactome options
4. **Run** the gene set analysis
5. Download results including pathway enrichment and visualization files

**Input File Formats**:
- Plain text files with one gene/protein identifier per line
- CSV files with gene lists and optional expression data
- Standard Galaxy datasets from previous analysis steps

**Output Files**:
- **CSV results**: Pathway enrichment statistics and gene mappings
- **HTML reports**: Interactive visualizations and summaries
- **Graphics**: Pathway diagrams and plots (when applicable)

## Distribution

### Conda Package

The Reactome Galaxy tools are distributed via Conda, which handles dependency management for the Galaxy platform. The tools automatically install required dependencies when deployed in a Galaxy instance.

**Installation in Galaxy**:
- Tools are installed through Galaxy's tool installation interface
- Dependencies are automatically resolved via Conda
- No manual dependency installation required

### Galaxy ToolShed

The tools are available through the Galaxy ToolShed, the official repository for Galaxy tools:

**ToolShed Distribution**:
- **Main Tool**: Available as `reactome` in the Galaxy ToolShed
- **GSA Tool**: Available as `reactome-gsa` in the Galaxy ToolShed
- **Automatic Updates**: New versions are automatically distributed through the ToolShed
- **Integration Testing**: All ToolShed releases include validated test cases

### Installation Notes for Galaxy Administrators

When installing the Reactome tools in a Galaxy instance:

1. **HTML Rendering**: Ensure tools are configured for "HTML Rendered" output in Admin → Manage Allowlist
2. **Dependencies**: Conda must be properly configured for automatic dependency resolution
3. **Testing**: Run the included test cases to verify proper installation
4. **Resources**: Tools may require adequate memory allocation for large gene sets

### Version Management

- **CLI Versions**: Tracked through Maven versioning in `pom.xml`
- **Galaxy Tool Versions**: Managed independently for each tool configuration
- **Dependency Versions**: Specified in tool configurations and automatically managed by Conda

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run the full test suite with `mvn package -Dconda.location=/path/to/your/miniconda`
5. Ensure all tests and linting checks pass
6. Submit a pull request

The Maven build process will automatically validate both Java code and Galaxy tool configurations.

## Support

For issues related to:
- **Reactome Analysis**: Contact the Reactome team
- **Galaxy Integration**: Use the Galaxy Community forums
- **Tool Development**: Open an issue in this repository

## Related Projects

- [ReactomeGSA](https://github.com/reactome/ReactomeGSA): R package for gene set analysis
- [Reactome Database](https://reactome.org): Main Reactome pathway database
- [Galaxy Project](https://galaxyproject.org): Open source bioinformatics platform