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

/**
 * @author UEHARA Junji(uehaj@jggug.org)
 */
class TypeLogger {
  
  static private final String PATCH_FILENAME = "staticalizer.patch"
  
  static private boolean initialized = false
  
  static private TypeLogRegistry registry = new TypeLogRegistry()
  
  static private shutdown() {
    new File(PATCH_FILENAME).withWriter { writer ->
      def emitter = new PatchEmitter(writer)
      emitter.emitDiff(registry.typeLogMap)
    }
  }
  
  static private initialize() {
    if (!initialized) {
      initialized = true
      Runtime.getRuntime().addShutdownHook(new Thread({shutdown()}))
    }
  }
  
  static Object logMethodArgs(String sourceFileName, int sourceLineNum, int sourceColumnNum, String methodName, List args) {
    initialize()
    registry.addMethodArgsTypeLog(sourceFileName, sourceLineNum, sourceColumnNum, methodName, args)
  }

  static Object logClosureArgs(String sourceFileName, int sourceLineNum, int sourceColumnNum, List args) {
    initialize()
    registry.addClosureArgsTypeLog(sourceFileName, sourceLineNum, sourceColumnNum, args)
  }
  
  static Object logReturn(String sourceFileName, int sourceLineNum, int sourceColumnNum, String methodName, Object returnValue) {
    String returnType = returnValue.getClass().getName()
    initialize()
    registry.addReturnTypeLog(sourceFileName, sourceLineNum, sourceColumnNum, methodName, returnType)
    return returnValue
  }

}
