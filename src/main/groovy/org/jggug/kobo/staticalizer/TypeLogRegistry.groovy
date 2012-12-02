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

class TypeLogRegistry {
  private static final String CLOSURE_MARKER = "<closure>"
  
  final Map<MethodOrClosureDecl, Set<List>> typeLogMap = new HashMap().withDefault {new HashSet()}

  public Map<MethodOrClosureDecl, Set<List>> getTypeLogMap() {
    return typeLogMap
  }

  void addMethodArgsTypeLog(String sourceFileName, int lineNumber, String methodName, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, methodName)
    typeLogMap.get(decl).add(new Arguments(args))
  }

  void addClosureArgsTypeLog(String sourceFileName, int lineNumber, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, CLOSURE_MARKER)
    typeLogMap.get(decl).add(new Arguments(args))
  }

  void addReturnTypeLog(String sourceFileName, int lineNumber, String methodName, String returnType) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, methodName)
    typeLogMap.get(decl).add(returnType)
  }

}
