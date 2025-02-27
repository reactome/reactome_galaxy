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

Note that when test-installing the package from a local file, dependency resolution for
openjdk is not performed. The -use-local flag is used to bypass this. For example,
using the same docker container:

```bash
# run container interactively
$ docker run -it --rm -v "$(pwd)":/workdir -w /workdir condaforge/mambaforge /bin/bash

# index the package folder
$ conda install -y conda-build
$ conda index /workdir/conda-bld

# create a test env and install the package
$ conda create -n testenv
$ conda activate testenv
$ conda install -c file:///workdir/conda-bld/ reactome-cli

# test the package
$ reactome --help
```