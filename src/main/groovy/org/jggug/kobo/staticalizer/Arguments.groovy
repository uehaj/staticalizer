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
