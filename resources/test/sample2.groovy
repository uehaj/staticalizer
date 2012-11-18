import staticalizer.transform.WithTypeLogging
@WithTypeLogging
def greet(a,b,c,d) {
//greet(java.lang.Integer a,java.math.BigDecimal b,java.lang.String c,java.util.ArrayList d)
//greet(java.math.BigDecimal a,java.math.BigDecimal b,java.lang.Integer c,java.util.ArrayList d)
  println "Hello World"
}

@WithTypeLogging
def greet2(a,b,c,d) {
//greet2(java.math.BigDecimal a,java.math.BigDecimal b,java.lang.Integer c,java.util.ArrayList d)
  println "Hello World"
}

greet(3, 3.3, "abc", [])
greet2(3.3, 3.3, 0, [])
greet(3.3, 3.3, 0, [])
greet(3.3, 3.3, 0, [])

