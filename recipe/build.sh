#!/bin/bash
set -euo pipefail

mkdir -p $PREFIX/lib/reactome
cp $SRC_DIR/reactome-cli-local-SNAPSHOT-jar-with-dependencies.jar $PREFIX/lib/reactome/reactome.jar

cat > $PREFIX/bin/reactome <<EOF
#!/bin/bash
exec java -jar \$PREFIX/lib/reactome/reactome.jar "\$@"
EOF

chmod +x $PREFIX/bin/reactome

