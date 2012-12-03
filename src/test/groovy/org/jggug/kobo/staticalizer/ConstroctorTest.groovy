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
import groovy.util.GroovyTestCase

class ConstructorTest extends GroovyTestCase {
  def groovyCommand = ["groovy", "-cp", "build/libs/staticalizer-0.1.jar", "-e"]

  static lsep = System.getProperty('line.separator')

  static lastLines(text, count=1) {
    text.split(lsep)[-count..-1]
  }
  
  static private final File patchFile = new File(TypeLogger.PATCH_FILENAME)
  
  void testMethodArg() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              @WithTypeLogging
              class X {
                X(i) {
                }
              }
              new X(3)
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change constructor argument type: <init>(java.lang.Integer i)'
  }

  void testMethodArgs() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class X {
                @WithTypeLogging
                X(i, j, k, s) {
                }
              }
              new X(3, 3.3, 3.3d, "3.3")
        """])

    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change constructor argument type: <init>(java.lang.Integer i,java.math.BigDecimal j,java.lang.Double k,java.lang.String s)'
  }

  void testMethodPartallyTypeSpecifiedArgs() {
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              @WithTypeLogging
              class X {
                X(int i, j, double k, s) {
                }
              }
              new X(3, 3.3, 3.3d, "3.3")
        """])

    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change constructor argument type: <init>(java.lang.Integer i,java.math.BigDecimal j,java.lang.Double k,java.lang.String s)'
  }
  
  void testClosureWithImplicitIt() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Z {
                @WithTypeLogging
                Z() {
                  (1..10).each { println it }
                }
              }
              new Z()
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change closure argument type: { java.lang.Integer it -> .. }'
  }
  
  void testClosureWithOneArg() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              @WithTypeLogging
              class Z {
                Z() {
                  (1..10).each { a -> println a }
                }
              }
              new Z()
        """])

    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change closure argument type: { java.lang.Integer a -> .. }'
  }

  void testClosureArgs() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Z {
                @WithTypeLogging
                Z() {
                  [k1:1, k2:2].each { a, b -> println("\$a:\$b") }
                }
              }
              new Z()
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change closure argument type: { java.lang.String a,java.lang.Integer b -> .. }'
  }

  void testClosurePartiallyTypeCpecifiedArgs() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              @WithTypeLogging
              class Z {
                Z() {
                  [k1:1, k2:2].each { String a, b -> println("\$a:\$b") }
                }
              }
              new Z()
        """])

    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change closure argument type: { java.lang.String a,java.lang.Integer b -> .. }'
  }
  
  void testMethodSameLine() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              @WithTypeLogging class X { X(i) {}; X(k,l) {} }
              new X(1)
              new X(1,2)
        """])

    def result = lastLines(patchFile.text, 4)
    assert result.size() == 4
    assert result[1] == '+// TODO: Change constructor argument type: <init>(java.lang.Integer i)'
    assert result[3] == '+// TODO: Change constructor argument type: <init>(java.lang.Integer k,java.lang.Integer l)'
  }
  
}
