import groovy.util.GroovyTestCase

class TypeLoggingTest extends GroovyTestCase {
  def args = ["groovy", "-cp", "build/libs/staticalizer-0.1.jar", "-e"]

  def lsep = System.getProperty('line.separator')

  def lastLines(text, count=1) {
    text.split(lsep)[-count..-1].join(lsep)
  }
  void testArg() {
    TestUtils.executeCommandOk([*args, """
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
  // void testReturn() {
  //   def res = "".evaluate("""
  //             import staticalizer.transform.WithTypeLogging
  //             class Y {
  //               @WithTypeLogging
  //               def bar() {
  //                 return 3
  //               }
  //             }
  //             (new Y()).bar()
  //       """)
  // }
  // void testClosure() {
  //   def res = new GroovyShell().evaluate("""
  //             import staticalizer.transform.WithTypeLogging
  //             class Z {
  //               @WithTypeLogging
  //               def baz() {
  //                 (1..10).each { println it }
  //               }
  //             }
  //             (new Z()).baz()
  //       """)
  // }
  
}
