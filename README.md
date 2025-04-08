# reactome_galaxy

Command-line utility and for running the Reactome analysis service and
integration into Galaxy platform.

To get started:

```bash
$ mvn package
$ java -jar target/reactome-cli-local-SNAPSHOT-jar-with-dependencies.jar \
    --reactome_url http://reactome.org \
    --protein_file_path src/test/resources/proteins.txt
```

## OpenAPI client

To create an openapi client,

1. Retrieve the OpenAPI spec from the Reactome API: https://reactome.org/AnalysisService/v3/api-docs into a file `api-docs.json`
2. Edit the `api-docs.json` and change " 400" to "400", producing `api-docs-fixed.json`
3. Install the api generator cli, e.g. on macOS with `brew install openapi-generator`
4. Generate the client code:

```bash
$ openapi-generator generate -i api-docs-fixed.json \
    -g kotlin \
    -o target/generated-sources/openapi-client \
    --additional-properties=packageName=org.reactome.analysis
```

The resulting client can be used as e.g.:

```kotlin
//import org.reactome.analysis.apis.IdentifierApi

val apiInstance = IdentifierApi(basePath = "https://reactome.org/AnalysisService")
val result = apiInstance.getIdentifierToHuman("BRCA2", interactors = true)
```