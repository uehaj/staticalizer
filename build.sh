#!/bin/sh

if [ -d ./build ]; then
  rm -r build
fi
mkdir build
mkdir build/classes
mkdir build/libs

groovyc -d build/classes src/main/groovy/org/jggug/kobo/staticalizer/*.groovy src/main/groovy/org/jggug/kobo/staticalizer/transform/TypeLoggingASTTransformation.groovy src/main/groovy/org/jggug/kobo/staticalizer/transform/WithTypeLogging.groovy

jar cf build/libs/staticalizer-0.1.jar -C build/classes .

