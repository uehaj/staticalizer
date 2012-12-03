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

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.tools.WideningCategories

/**
 * @author <a href="mailto:uehaj@jggug.org">UEHARA Junji</a>
 */
class TypeLogRegistry {
  
  HashSet<String> keys = [] as HashSet
  
  Map<MethodOrClosureDecl, List<String>> paramTypesMap = [:]
  
  Map<MethodOrClosureDecl, List<String>> paramNamesMap = [:]
  
  Map<MethodOrClosureDecl, String> returnTypeMap = [:]

  static String lowestUpperBound(String class1, String class2) {
    ClassNode result = WideningCategories.lowestUpperBound(new ClassNode(Class.forName(class1)),
                                                           new ClassNode(Class.forName(class2)))
    // TODO: support generics type
    return result.getName()
  }

  private void registerParamTypes(MethodOrClosureDecl decl, List<String> argTypes) {
    keys.add(decl)
    def paramTypes = paramTypesMap.get(decl)
    if (paramTypes == null) {
      paramTypesMap.put(decl, argTypes)
    }
    else {
      assert paramTypes.size() == argTypes.size()
      for (int i=0; i<paramTypes.size(); i++) {
        paramTypes[i] = lowestUpperBound(paramTypes[i], argTypes[i])
      }
    }
  }
  
  private void registerParamNames(MethodOrClosureDecl decl, List<String> argNames) {
    keys.add(decl)
    def paramNames = paramNamesMap.get(decl)
    if (paramNames == null) {
      paramNamesMap.put(decl, argNames)
    }
    else {
      assert paramNames == argNames
    }
  }
  
  private void registerReturnType(MethodOrClosureDecl decl, String type) {
    keys.add(decl)
    def returnType = returnTypeMap.get(decl)
    if (returnType == null) {
      returnTypeMap.put(decl, type)
    }
    else {
      returnType = lowestUpperBound(returnType, type)
      returnTypeMap.put(decl, returnType)
    }
  }
  
  void addMethodArgsTypeLog(String sourceFileName, int lineNumber, int columnNumber, String methodName, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, columnNumber, methodName, "M")
    registerParamTypes(decl, args.collect { it[0] })
    registerParamNames(decl, args.collect { it[1] })
  }

  void addClosureArgsTypeLog(String sourceFileName, int lineNumber, int columnNumber, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, columnNumber, null, "C")
    registerParamTypes(decl, args.collect { it[0] })
    registerParamNames(decl, args.collect { it[1] })
  }

  void addReturnTypeLog(String sourceFileName, int lineNumber, int columnNumber, String methodName, String returnType) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, columnNumber, methodName, "R")
    registerReturnType(decl, returnType)
  }

  String params(MethodOrClosureDecl decl) {
    def paramTypes = paramTypesMap[decl]
    def paramNames = paramNamesMap[decl]
    assert paramTypes.size() == paramNames.size()
    List tmp = []
    for (int i=0; i<paramTypes.size(); i++) {
      tmp += paramTypes[i] + ' ' + paramNames[i]
    }
    return tmp.join(',')
  }

  String returnType(MethodOrClosureDecl decl) {
    return returnTypeMap[decl]
  }
}
