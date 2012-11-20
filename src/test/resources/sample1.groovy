import staticalizer.transform.WithTypeLogging
@WithTypeLogging
def greet(a,b,c,d) {
  println "Hello World"
}

greet(3, 3.3, "abc", [])
greet(3.3, 3.3, 0, [:])
