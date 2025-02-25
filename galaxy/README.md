# Galaxy Development Configuration

Use docker to run a local instance of Galaxy with the Reactome plugin for
dev/testing as follows:

Create an empty galaxy storage folder (this will be preserved and reused):

```bash
$ mdkir galaxy_storage
```

Build and copy the galaxy jar to the `local_tools` folder:

```bash
$ mvn package
$ cp target/reactome-cli-local-SNAPSHOT-jar-with-dependencies.jar local_tools/reactome/reactome.jar
```

Start the container (this may take a while):

```bash
docker run -d \
  -v galaxy_storage/:/export/ \
  -v local_tools/:/local_tools \
  --privileged=true \
  -p 8080:80 -p 8081:21 -p 8022:22 \
  -e GALAXY_CONFIG_TOOL_CONFIG_FILE=/etc/galaxy/tool_conf.xml,/local_tools/local_tools.xml \
  --name galaxy \
  quay.io/bgruening/galaxy
```

Follow logs with `docker logs -f galaxy`

Once ready, navigate to `http://localhost:8080` for the Galaxy UI, the plugin should be available in the tool panel.

Note this assumes that java available in the galaxy container image, via the path in the `command` field
of `local_tools/reactome/reactome.xml`, adjust as needed.
