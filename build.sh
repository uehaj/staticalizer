#!/bin/sh

if [ -d ./build ]; then
  rm -r build
fi
mkdir build
mkdir build/classes
mkdir build/lib

groovyc -d build/classes src/main/groovy/staticalizer/TypeLogger.groovy src/main/groovy/staticalizer/transform/TypeLoggingASTTransformation.groovy src/main/groovy/staticalizer/transform/WithTypeLogging.groovy

jar cf build/libs/staticalizer-0.1.jar -C build/classes .

