# reactome_galaxy

Command-line utility and for running the Reactome analysis service and
integration into Galaxy platform.

Getting started requires a jdk >= 11 for the cli and conda for the galaxy integration.
Build the cli and run an sample gene list analysis with the following commands:

```bash
$ mvn package -Dconda.location=/your/miniconda/location

$ java -jar target/reactome-jar-with-dependencies.jar \
      genes \
      --reactome_url https://release.reactome.org \
      --identifiers_file src/test/resources/uniprot.txt \
      --pathways /tmp/pathways.csv
```

The client supports gene, tissue, and species analysis. Use `--help` on the main
command and on each sub-command to see the full range of options.

## Integration with Galaxy

Galaxy integration is via a pair of tool configurations, `galaxy/local_tools/reactome` integrates
the java cli for gene, tissue, and species analysis, while `galaxy/local_tools/reactome-gsa` integrates
the standalone R client [ReactomeGSA](https://github.com/reactome/ReactomeGSA) for GSA analysis.

Both tools include integration tests that can be run using [planemo](https://planemo.readthedocs.io/en/latest/).

```bash
$ planemo lint galaxy/local_tools/reactome
$ planemo test galaxy/local_tools/reactome

$ planemo lint galaxy/local_tools/reactome-gsa
$ planemo test galaxy/local_tools/reactome-gsa
```

When installing the tools in a Galaxy instance, the Galaxy admin should ensure that the
tool is set to `HTML Rendered` in Admin -> Manage Allowlist.