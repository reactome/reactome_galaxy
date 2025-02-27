# Reactome-CLI Conda package

Cross-platform conda package with dependency on Java.

Building the package requires conda & the conda-build tool. These
can be installed locally, or via a docker container. In the
root of this repository, run:

```bash
# create the jar file to be included in the package
$ mvn package

# build package via docker
$ docker run --rm -v "$(pwd)":/workdir -w /workdir condaforge/mambaforge \
  /bin/bash -c "conda install -y conda-build && conda build --output-folder /workdir/conda-bld conda"
```

If successful, the package will be under the `conda-bld` directory.