#!/usr/bin/env bash
# Compila el simulador y genera estadio.jar (requiere JDK 17 o más nuevo).
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes

find src -name '*.java' > build/fuentes.txt
javac --release 17 -encoding UTF-8 -Xlint:all,-serial,-this-escape -d build/classes @build/fuentes.txt

# Recursos (sonidos, imágenes) si ya existen
if [ -d resources ]; then
    cp -r resources/. build/classes/
fi

jar --create --file estadio.jar --main-class estadio.Main -C build/classes .
echo "Listo: estadio.jar"
