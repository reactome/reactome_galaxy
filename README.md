# reactome_galaxy

Command-line utility and for running the Reactome analysis service and
integration into Galaxy platform.

To get started:

```bash
$ mvn package
$ java -jar target/reactome-cli-local-SNAPSHOT-jar-with-dependencies.jar --reactome_url http://reactome.org \
    --protein_file_path src/test/resources/uniprot.txt
```


