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

/**
 * @author <a href="mailto:uehaj@jggug.org">UEHARA Junji</a>
 */
@Canonical
class MethodOrClosureDecl implements Comparable {
  
  String sourceFileName
  
  int lineNumber
  
  int cloumnNumber
  
  String methodName

  String type
  
  int compareTo(rhs) {
    if (rhs == null) { throw new NullPointerException() }
    if (sourceFileName == rhs.sourceFileName) {
      if (lineNumber == rhs.lineNumber) {
        if (columnNumber == rhs.columnNumber) {
          // following comparison is useless because if
          // column number is the same, method name should be same too.
          if (methodName == rhs.methodName) {
            return type <=> rhs.type
          }
          else return methodName <=> rhs.methodName
        }
        else return columnNumber <=> rhs.columnNumber
      }
      else return lineNumber <=> rhs.lineNumber
    }
    else return sourceFileName <=> rhs.sourceFileName
  }
}
