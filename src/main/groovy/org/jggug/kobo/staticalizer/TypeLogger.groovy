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
