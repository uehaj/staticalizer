import staticalizer.transform.WithTypeLogging
@WithTypeLogging
def greet(a,b,c,d) {
  println "Hello World"
}

@WithTypeLogging
def greet2(a,b,c,d) {
  println "Hello World"
}

greet(3, 3.3, "abc", [])
greet2(3.3, 3.3, 0, [])
greet(3.3, 3.3, 0, [])
greet(3.3, 3.3, 0, [])

