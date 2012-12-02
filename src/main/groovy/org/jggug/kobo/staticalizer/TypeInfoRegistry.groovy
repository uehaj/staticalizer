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
