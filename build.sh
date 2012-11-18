#!/bin/sh

if [ -d ./build ]; then
  rm -r build
fi
mkdir build
mkdir build/classes

groovyc -d build/classes src/main/staticalizer/TypeLogger.groovy src/main/staticalizer/transform/TypeLoggingASTTransformation.groovy src/main/staticalizer/transform/WithTypeLogging.groovy

jar cf staticalizer.jar -C build/classes .

