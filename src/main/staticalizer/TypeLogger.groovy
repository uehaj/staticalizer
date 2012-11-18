package staticalizer

import groovy.transform.*
import java.text.SimpleDateFormat

@TupleConstructor
@ToString
class MethodCall implements Comparable {
  String sourceFileName
  int lineNumber
  String methodName
  boolean equals(rhs) {
    compareTo(rhs) == 0
  }
  int compareTo(rhs) {
    if (rhs == null) { throw new NullPointerException() }
    if (sourceFileName == rhs.sourceFileName) {
      if (lineNumber == rhs.lineNumber) {
        return methodName <=> rhs.methodName
      }
      else return lineNumber <=> rhs.lineNumber
    }
    else return sourceFileName <=> rhs.sourceFileName
  }
  int hashCode() {
    return sourceFileName.hashCode() + methodName.hashCode() + lineNumber
  }
}

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

class TypeInfoRegistry {
  Map<MethodCall, Set<List>> typeInfoMap = new HashMap().withDefault {new HashSet()}

  void addTypeInfo(String sourceFileName, int lineNumber, String methodName, List<List<String>> args) {
    def methodCall = new MethodCall(sourceFileName, lineNumber, methodName)
    typeInfoMap.get(methodCall).add(new Arguments(args))
  }

  String emitArgs(Arguments args) {
    args.arguments.collect{it[0]+" "+it[1]}.join(",")
  }

  void emit(Writer output, String str) {
    output.write(str+"\n")
  }

  String fileTime(String fileName) {
    File file = new File(fileName)
    def lastModified = file.lastModified()
    Date date = new Date(lastModified)
    //    String pat = "EEE MMM dd HH:mm:ss yyyy";
    String pat = "yyyy-mm-dd HH:mm:ss.SSSSSSSSS Z";
    SimpleDateFormat sdf = new SimpleDateFormat(pat, Locale.US);
    return sdf.format(date)
  }

  void emitDiff(Writer output) {
    String fileName = null
    int ofs

    typeInfoMap.keySet().sort().each { methodCall ->
      if (fileName != methodCall.sourceFileName) {
        fileName = methodCall.sourceFileName
        emit(output, "--- "+fileName+" 2012-11-17 21:05:08.000000000 +0900")
        emit(output, "+++ "+fileName+".staticgroovy"+" 2012-11-17 21:05:08.000000000 +0900")
        ofs = 0
      }
      methodCall.with {
        def diffs = typeInfoMap[methodCall]
        emit(output, "@@ -${lineNumber+1},0 +${lineNumber+1+ofs},${diffs.size()} @@")
        ofs += diffs.size()
        diffs.each {
          emit(output, "+//"+methodName+"("+emitArgs(it)+")")
        }
      }
    }
  }
}

class TypeLogger {
  static private boolean initialized = false
  static private TypeInfoRegistry repo = new TypeInfoRegistry()
  static private initialize() {
    initialized = true
    Runtime.getRuntime().addShutdownHook(new Thread({
                                                      new File("staticalizer.patch").withWriter { writer ->
                                                        repo.emitDiff(writer)
                                                      }
                                                    }))
  }
  static void log(String sourceFileName, int sourceLineNum, String methodName, List args) {
    // println "$sourceFileName:$sourceLineNum:$methodName($args)"
    if (!initialized) {
      initialize()
    }
    repo.addTypeInfo(sourceFileName, sourceLineNum, methodName, args)
  }
}


