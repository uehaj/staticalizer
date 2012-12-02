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

class TypeLoggingTest extends GroovyTestCase {
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
              class X {
                @WithTypeLogging
                int foo(i) {
                  return 0
                }
              }
              (new X()).foo(3)
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change method argument type: foo(java.lang.Integer i)'
  }

  void testMethodArgs() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class X {
                @WithTypeLogging
                int foo(i, j, k, s) {
                  return 0
                }
              }
              (new X()).foo(3, 3.3, 3.3d, "3.3")
        """])

    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change method argument type: foo(java.lang.Integer i,java.math.BigDecimal j,java.lang.Double k,java.lang.String s)'
  }

  void testMethodPartallyTypeSpecifiedArgs() {
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class X {
                @WithTypeLogging
                int foo(int i, j, double k, s) {
                  return 0
                }
              }
              (new X()).foo(3, 3.3, 3.3d, "3.3")
        """])

    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change method argument type: foo(java.lang.Integer i,java.math.BigDecimal j,java.lang.Double k,java.lang.String s)'
  }
  
  void testClosureWithImplicitIt() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Z {
                @WithTypeLogging
                def baz() {
                  (1..10).each { println it }
                }
              }
              (new Z()).baz()
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change closure argument type: { java.lang.Integer it -> .. }'
  }
  
  void testClosureWithOneArg() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Z {
                @WithTypeLogging
                def baz() {
                  (1..10).each { a -> println a }
                }
              }
              (new Z()).baz()
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
                def baz() {
                  [k1:1, k2:2].each { a, b -> println("\$a:\$b") }
                }
              }
              (new Z()).baz()
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change closure argument type: { java.lang.String a,java.lang.Integer b -> .. }'
  }

  void testClosurePartiallyTypeCpecifiedArgs() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Z {
                @WithTypeLogging
                def baz() {
                  [k1:1, k2:2].each { String a, b -> println("\$a:\$b") }
                }
              }
              (new Z()).baz()
        """])

    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change closure argument type: { java.lang.String a,java.lang.Integer b -> .. }'
  }
  
  void testReturn() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Y {
                @WithTypeLogging
                def bar() {
                  return 3
                }
              }
              (new Y()).bar()
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change return type: java.lang.Integer bar(...)'
  }

  void testReturnTypeSpecified() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Y {
                @WithTypeLogging
                int bar() {
                  return 3
                }
              }
              def y = new Y()
              y.bar()
        """])
    
    assert !patchFile.exists()
  }

  void testReturnOfTwoMethods() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Y {
                @WithTypeLogging
                def foo() {
                  return 3
                }
                @WithTypeLogging
                def bar() {
                  return 3.3
                }
              }
              def y = new Y()
              y.foo()
              y.bar()
        """])
    
    def result = lastLines(patchFile.text, 4)
    assert result.size() == 4
    assert result[1] == '+// TODO: Change return type: java.lang.Integer foo(...)'
    assert result[3] == '+// TODO: Change return type: java.math.BigDecimal bar(...)'
  }

  void testReturnTypeSpecifiedAndNotSpecified() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class Y {
                @WithTypeLogging
                def foo() {
                  return 3
                }
                @WithTypeLogging
                int bar() {
                  return 3
                }
              }
              def y = new Y()
              y.foo()
              y.bar()
        """])
    
    def result = lastLines(patchFile.text, 1)
    assert result.size() == 1
    assert result[0] == '+// TODO: Change return type: java.lang.Integer foo(...)'
  }
  
  void testMethodSameLine() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import org.jggug.kobo.staticalizer.transform.WithTypeLogging
              class X {
                @WithTypeLogging def foo(i) {0}; @WithTypeLogging def bar(i) {0}
              }
              X x = new X()
              x.foo(3)
              x.bar(4)
        """])

    def result = lastLines(patchFile.text, 4)
    assert result.size() == 4
    assert result[1] == '+// TODO: Change method argument type: bar(java.lang.Integer i)'
    assert result[3] == '+// TODO: Change method argument type: foo(java.lang.Integer i)'
  }
  
}
