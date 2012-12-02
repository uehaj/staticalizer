/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jggug.kobo.staticalizer

import groovy.transform.*

@Canonical
class MethodOrClosureDecl implements Comparable {
  String sourceFileName
  int lineNumber
  String methodName
  int compareTo(rhs) {
    if (rhs == null) { throw new NullPointerException() }
    if (sourceFileName == rhs.sourceFileName) {
      if (lineNumber == rhs.lineNumber) {
        return methodName <=> rhs.methodName
      }
      else return lineNumber <=> rhs.lineNumber
    }
    else return sourceFileName <=> rhs.sourceFileName
  }
}

@TupleConstructor
class Arguments {
  List<List<String>> arguments

  boolean equals(rhs) {
    if (rhs == null) {
      return false
    }
    if (arguments.size() != rhs.arguments.size()) {
      return false
    }
    for (int i=0; i<arguments.size(); i++) {
      if (arguments[i] == null && rhs.arguments[i] == null) {
        continue
      }
      if (arguments[i].size() != rhs.arguments[i].size()) {
        return false
      }
      if (arguments[i][0] != rhs.arguments[i][0]) {
        return false
      }
    }
    return true
  }
  
  int hashCode() {
    int result = 0
    for (int i=0; i<arguments.size(); i++) {
      result += arguments[i][0].hashCode() + arguments[i][1].hashCode()
    }
    return result
  }
}

class TypeInfoRegistry {
  private static final String CLOSURE_MARKER = "<closure>"
  
  final Map<MethodOrClosureDecl, Set<List>> typeInfoMap = new HashMap().withDefault {new HashSet()}

  void addMethodArgsTypeInfo(String sourceFileName, int lineNumber, String methodName, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, methodName)
    typeInfoMap.get(decl).add(new Arguments(args))
  }

  void addClosureArgsTypeInfo(String sourceFileName, int lineNumber, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, CLOSURE_MARKER)
    typeInfoMap.get(decl).add(new Arguments(args))
  }

  void addReturnTypeInfo(String sourceFileName, int lineNumber, String methodName, String returnType) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, methodName)
    typeInfoMap.get(decl).add(returnType)
  }

}

class TypeLogger {
  
  static private final String PATCH_FILENAME = "staticalizer.patch"
  
  static private boolean initialized = false
  
  static private TypeInfoRegistry repo = new TypeInfoRegistry()
  
  static private shutdown() {
    new File(PATCH_FILENAME).withWriter { writer ->
      def emitter = new PatchEmitter(writer)
      emitter.emitDiff(repo.typeInfoMap)
    }
  }
  
  static private initialize() {
    if (!initialized) {
      initialized = true
      Runtime.getRuntime().addShutdownHook(new Thread({shutdown()}))
    }
  }
  
  static Object logMethodArgs(String sourceFileName, int sourceLineNum, String methodName, List args) {
    initialize()
    repo.addMethodArgsTypeInfo(sourceFileName, sourceLineNum, methodName, args)
  }

  static Object logClosureArgs(String sourceFileName, int sourceLineNum, List args) {
    initialize()
    repo.addClosureArgsTypeInfo(sourceFileName, sourceLineNum, args)
  }
  
  static Object logReturn(String sourceFileName, int sourceLineNum, String methodName, Object returnValue) {
    String returnType = returnValue.getClass().getName()
    initialize()
    repo.addReturnTypeInfo(sourceFileName, sourceLineNum, methodName, returnType)
    return returnValue
  }

}
