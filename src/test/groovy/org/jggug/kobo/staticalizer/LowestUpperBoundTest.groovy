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

import groovy.util.GroovyTestCase

/**
 * @author <a href="mailto:uehaj@jggug.org">UEHARA Junji</a>
 */
class LowestUpperBoundTest extends GroovyTestCase {
  def groovyCommand = ["groovy", "-cp", "build/libs/staticalizer-0.1.jar", "-e"]

  static lsep = System.getProperty('line.separator')

  static lastLines(text, count=1) {
    text.split(lsep)[-count..-1]
  }

  static private final File patchFile = new File(TypeLogger.PATCH_FILENAME)

  void testReturnLUB() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Y {
                @WithTypeLogging
                def foo(int n) {
                  if (n % 2) {
                    return 3.3
                  }
                  else {
                    return 3
                  }
                }
              }
              def y = new Y()
              y.foo(1)
              y.foo(2)
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change return type: java.lang.Number foo(...)'
  }
}
