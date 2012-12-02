package staticalizer

import groovy.util.GroovyTestCase

class TypeLoggingTest extends GroovyTestCase {
  def groovyCommand = ["groovy", "-cp", "build/libs/staticalizer-0.1.jar", "-e"]

  def lsep = System.getProperty('line.separator')

  def lastLines(text, count=1) {
    text.split(lsep)[-count..-1]
  }

  static private final File patchFile = new File(TypeLogger.PATCH_FILENAME)
  
  void testMethodArg() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import staticalizer.transform.WithTypeLogging
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
    assert result[0] == '+// TODO: Change argument type: foo(java.lang.Integer i)'
  }

  void testMethodArgs() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import staticalizer.transform.WithTypeLogging
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
    assert result[0] == '+// TODO: Change argument type: foo(java.lang.Integer i,java.math.BigDecimal j,java.lang.Double k,java.lang.String s)'
  }

  void testMethodPartallyTypeSpecifiedArgs() {
    TestUtils.executeCommandOk([*groovyCommand, """
              import staticalizer.transform.WithTypeLogging
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
    assert result[0] == '+// TODO: Change argument type: foo(java.lang.Integer i,java.math.BigDecimal j,java.lang.Double k,java.lang.String s)'
  }
  
  void testClosureWithImplicitIt() {
    patchFile.delete()
    TestUtils.executeCommandOk([*groovyCommand, """
              import staticalizer.transform.WithTypeLogging
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
              import staticalizer.transform.WithTypeLogging
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
              import staticalizer.transform.WithTypeLogging
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
              import staticalizer.transform.WithTypeLogging
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
              import staticalizer.transform.WithTypeLogging
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
              import staticalizer.transform.WithTypeLogging
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
              import staticalizer.transform.WithTypeLogging
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
              import staticalizer.transform.WithTypeLogging
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
}
