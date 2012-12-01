package staticalizer

import groovy.transform.*
import java.text.SimpleDateFormat

@Canonical
class MethodCall implements Comparable {
  String sourceFileName
  int lineNumber
  String methodName
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
  private static final String CHANGED_FILENAME_POSTFIX = ".command"
  
  Map<MethodCall, Set<List>> typeInfoMap = new HashMap().withDefault {new HashSet()}

  void addArgsTypeInfo(String sourceFileName, int lineNumber, String methodName, List<List<String>> args) {
    def methodCall = new MethodCall(sourceFileName, lineNumber, methodName)
    typeInfoMap.get(methodCall).add(new Arguments(args))
  }

  void addReturnTypeInfo(String sourceFileName, int lineNumber, String methodName, String returnType) {
    def methodCall = new MethodCall(sourceFileName, lineNumber, methodName)
    typeInfoMap.get(methodCall).add(returnType)
  }

  String composeArgs(Arguments args) {
    args.arguments.collect{it[0]+" "+it[1]}.join(",")
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

  void emit(Writer output, String str) {
    output.write(str+"\n")
  }

  void emitDiff(Writer output) {
    String fileName = null
    int ofs

    typeInfoMap.keySet().sort().each { methodCall ->
      if (fileName != methodCall.sourceFileName) {
        fileName = methodCall.sourceFileName
        def changedFileName = fileName + CHANGED_FILENAME_POSTFIX
        def time = fileTime(fileName)
        emit(output, "--- "+fileName+" "+time)
        emit(output, "+++ "+changedFileName+" "+time)
        ofs = 0
      }
      methodCall.with {
        def diffs = typeInfoMap[methodCall]
        emit(output, "@@ -${lineNumber+1},0 +${lineNumber+1+ofs},${diffs.size()} @@")
        ofs += diffs.size()
        diffs.each {
          if (it instanceof Arguments) {
            emit(output, "+// TODO: Change argument type: "+methodName+"("+composeArgs(it)+")")
          }
          else if (it instanceof String) {
            emit(output, "+// TODO: Change return type: "+it+" "+methodName+"(...)")
          }
        }
      }
    }
    println """Patch file '${TypeLogger.PATCH_FILENAME}' generated, verify the content of it and do following command:
 % (cd /; patch -b -p0) < ${TypeLogger.PATCH_FILENAME}"""
  }
}

class TypeLogger {
  private static final String PATCH_FILENAME = "staticalizer.patch"
  static private boolean initialized = false
  static private TypeInfoRegistry repo = new TypeInfoRegistry()
  static private shutdown() {
    new File(PATCH_FILENAME).withWriter { repo.emitDiff(it) }
  }
  static private initialize() {
    initialized = true
    Runtime.getRuntime().addShutdownHook(new Thread({shutdown()}))
  }
  static Object logArgs(String sourceFileName, int sourceLineNum, String methodName, List args) {
    println "$sourceFileName:$sourceLineNum:$methodName($args)"
    if (!initialized) {
      initialize()
    }
    repo.addArgsTypeInfo(sourceFileName, sourceLineNum, methodName, args)
  }
  static Object logReturn(String sourceFileName, int sourceLineNum, String methodName, Object returnValue) {
    String returnType = returnValue.getClass().getName()
    println "$sourceFileName:$sourceLineNum:$methodName() returns $returnType"
    if (!initialized) {
      initialize()
    }
    repo.addReturnTypeInfo(sourceFileName, sourceLineNum, methodName, returnType)
    return returnValue
  }
}
