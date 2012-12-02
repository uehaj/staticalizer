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

  static String composeArgs(Arguments args) {
    args.arguments.collect{it[0]+" "+it[1]}.join(",")
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

  void emitDiffs(Set<List> diffs, MethodOrClosureDecl decl, int ofs) {
    emit("@@ -${decl.lineNumber+1},0 +${decl.lineNumber+1+ofs},${diffs.size()} @@")
    diffs.each {
      if (it instanceof Arguments) {
        if (decl.methodName == TypeLogRegistry.CLOSURE_MARKER) {
          emit("+// TODO: Change closure argument type: { "+composeArgs(it)+" -> .. }")
        }
        else {
          emit("+// TODO: Change argument type: "+decl.methodName+"("+composeArgs(it)+")")
        }
      }
      else if (it instanceof String) {
        emit("+// TODO: Change return type: "+it+" "+decl.methodName+"(...)")
      }
    }
  }

  void emitDiff(Map<MethodOrClosureDecl, Set<List>> typeLogMap) {
    String fileName = null
    int ofs = 0

    typeLogMap.keySet().sort().each { MethodOrClosureDecl decl ->
      if (fileName != decl.sourceFileName) {
        fileName = decl.sourceFileName
        emitHeader(fileName)
        ofs = 0
      }

      def diffs = typeLogMap[decl]
      emitDiffs(diffs, decl, ofs)
      ofs += diffs.size()
    }
    println """Patch file '${TypeLogger.PATCH_FILENAME}' generated. Apply the patch by:
 % (cd /; patch -b -p0) < ${TypeLogger.PATCH_FILENAME}"""
  }
}  
