import groovy.util.GroovyTestCase

class TypeLoggingTest extends GroovyTestCase {
  def groovyCommand = ["groovy", "-cp", "build/libs/staticalizer-0.1.jar", "-e"]

  def lsep = System.getProperty('line.separator')

  def lastLines(text, count=1) {
    text.split(lsep)[-count..-1].join(lsep)
  }
  
  void testArg() {
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

    assert lastLines(new File("staticalizer.patch").text, 1) == '+// TODO: Change argument type: foo(java.lang.Integer i)'
  }

  void testReturn() {
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
    assert lastLines(new File("staticalizer.patch").text, 1) == '+// TODO: Change return type: java.lang.Integer bar(...)'
  }
  
  void testClosureWithImplicitIt() {
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
    assert lastLines(new File("staticalizer.patch").text, 1) == '+// TODO: Change closure argument type: { java.lang.Integer it -> .. }'
  }
  
  void testClosureWithOneArg() {
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
    assert lastLines(new File("staticalizer.patch").text, 1) == '+// TODO: Change closure argument type: { java.lang.Integer a -> .. }'
  }
}
