package staticalizer

import groovy.transform.*
import java.text.SimpleDateFormat

@Canonical
class MethodOrClosureDecl implements Comparable {
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
  private static final String CHANGED_FILENAME_POSTFIX = ".changed"
  private static final String CLOSURE_MARKER = "<closure>"
  
  Map<MethodOrClosureDecl, Set<List>> typeInfoMap = new HashMap().withDefault {new HashSet()}

  void addMethodArgsTypeInfo(String sourceFileName, int lineNumber, String methodName, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, methodName)
    typeInfoMap.get(decl).add(new Arguments(args))
  }

  void addClosureArgsTypeInfo(String sourceFileName, int lineNumber, List<List<String>> args) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, CLOSURE_MARKER)
    typeInfoMap.get(decl).add(new Arguments(args))
  }

  void addReturnTypeInfo(String sourceFileName, int lineNumber, String methodName, String returnType) {
    def decl = new MethodOrClosureDecl(sourceFileName, lineNumber, methodName)
    typeInfoMap.get(decl).add(returnType)
  }

  String composeArgs(Arguments args) {
    args.arguments.collect{it[0]+" "+it[1]}.join(",")
  }

  String fileTime(String fileName) {
    File file = new File(fileName)
    def lastModified = file.lastModified()
    Date date = new Date(lastModified)
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

    typeInfoMap.keySet().sort().each { decl ->
      if (fileName != decl.sourceFileName) {
        fileName = decl.sourceFileName
        def changedFileName = fileName + CHANGED_FILENAME_POSTFIX
        def time = fileTime(fileName)
        emit(output, "--- "+fileName+" "+time)
        emit(output, "+++ "+changedFileName+" "+time)
        ofs = 0
      }

      def diffs = typeInfoMap[decl]
      emit(output, "@@ -${decl.lineNumber+1},0 +${decl.lineNumber+1+ofs},${diffs.size()} @@")
      ofs += diffs.size()
      diffs.each {
        if (it instanceof Arguments) {
          if (decl.methodName == CLOSURE_MARKER) {
            emit(output, "+// TODO: Change closure argument type: { "+composeArgs(it)+" -> .. }")
          }
          else {
            emit(output, "+// TODO: Change argument type: "+decl.methodName+"("+composeArgs(it)+")")
          }
        }
        else if (it instanceof String) {
          emit(output, "+// TODO: Change return type: "+it+" "+decl.methodName+"(...)")
        }
      }
    }
    println """Patch file '${TypeLogger.PATCH_FILENAME}' generated. Apply the patch by:
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
  static Object logMethodArgs(String sourceFileName, int sourceLineNum, String methodName, List args) {
    if (!initialized) {
      initialize()
    }
    repo.addMethodArgsTypeInfo(sourceFileName, sourceLineNum, methodName, args)
  }
  static Object logClosureArgs(String sourceFileName, int sourceLineNum, List args) {
    if (!initialized) {
      initialize()
    }
    repo.addClosureArgsTypeInfo(sourceFileName, sourceLineNum, args)
  }
  static Object logReturn(String sourceFileName, int sourceLineNum, String methodName, Object returnValue) {
    String returnType = returnValue.getClass().getName()
    if (!initialized) {
      initialize()
    }
    repo.addReturnTypeInfo(sourceFileName, sourceLineNum, methodName, returnType)
    return returnValue
  }
}
