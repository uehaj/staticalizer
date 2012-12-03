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

import java.text.SimpleDateFormat
import org.codehaus.groovy.ast.ClassNode

/**
 * @author <a href="mailto:uehaj@jggug.org">UEHARA Junji</a>
 */
class PatchEmitter {
  
  private static final String CHANGED_FILENAME_POSTFIX = ".changed"

  private Writer output
  
  PatchEmitter(Writer output) {
    this.output = output
  }

  static String fileTime(String fileName) {
    File file = new File(fileName)
    def lastModified = file.lastModified()
    Date date = new Date(lastModified)
    String pat = "yyyy-mm-dd HH:mm:ss.SSSSSSSSS Z";
    SimpleDateFormat sdf = new SimpleDateFormat(pat, Locale.US);
    return sdf.format(date)
  }

  void emit(String str) {
    output.write(str+"\n")
  }

  void emitHeader(String fileName) {
    def changedFileName = fileName + CHANGED_FILENAME_POSTFIX
    def time = fileTime(fileName)
    emit("--- "+fileName+" "+time)
    emit("+++ "+changedFileName+" "+time)
  }

  void emitDiffs(TypeLogRegistry registry, MethodOrClosureDecl decl, int ofs) {
    emit("@@ -${decl.lineNumber+1},0 +${decl.lineNumber+1+ofs},1 @@")
    switch (decl.type) {
    case "C": // Closure argument type
      emit("+// TODO: Change closure argument type: { "+registry.params(decl)+" -> .. }")
      break;
    case "M": // Method argument type
      emit("+// TODO: Change method argument type: "+decl.methodName+"("+registry.params(decl)+")")
      break;
    case "R": // Returning type of method
      emit("+// TODO: Change return type: "+registry.returnType(decl)+" "+decl.methodName+"(...)")
      break;
    }
  }

  void emitDiff(TypeLogRegistry registry) {
    String fileName = null
    int ofs = 0

    registry.keys.sort().each { MethodOrClosureDecl decl ->
      if (fileName != decl.sourceFileName) {
        fileName = decl.sourceFileName
        emitHeader(fileName)
        ofs = 0
      }
      emitDiffs(registry, decl, ofs)
      ofs++
    }
    println """Patch file '${TypeLogger.PATCH_FILENAME}' generated. Apply the patch with command line:
 % (cd /; patch -b -p0) < ${TypeLogger.PATCH_FILENAME}"""
  }
}  
