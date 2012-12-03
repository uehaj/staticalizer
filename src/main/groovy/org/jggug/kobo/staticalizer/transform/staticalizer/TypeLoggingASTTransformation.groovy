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
package org.jggug.kobo.staticalizer.transform

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.transform.*
import org.codehaus.groovy.syntax.SyntaxException;

/**
 * @author UEHARA Junji(uehaj@jggug.org)
 */
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
public class TypeLoggingASTTransformation extends ClassCodeExpressionTransformer implements ASTTransformation {

  SourceUnit sourceUnit = sourceUnit

  @Override
  SourceUnit getSourceUnit() {
    return sourceUnit;
  }

  @Override
  public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
    this.sourceUnit = sourceUnit

    AnnotatedNode node = (AnnotatedNode) nodes[1]
    ClassNode withTypeLoggingClassNode = new ClassNode(WithTypeLogging)
    if (node instanceof ClassNode) {
      // find all methods annotated with @WithTypeLogging
      node.getMethods().each { MethodNode method ->
        println "here"+method
        visitMethod(method);
      }
    } else if (node instanceof MethodNode) {
      visitMethod(node);
    } else {
      source.addError(new SyntaxException("Unsupported node type", node.getLineNumber(), node.getColumnNumber()));
    }
  }

  @Override
  Expression transform(Expression exp) {
    if (exp == null) return null
    if (exp.class == MethodCallExpression) {
      // transformの呼び出しを単に分配する(なぜか自動的には分配されないため)
      def object = transform(exp.getObjectExpression())
      def method = transform(exp.getMethod())
      def args = transform(exp.getArguments())
      def result = new MethodCallExpression(object, method, args)
      return result
    }
    else if (exp.class == ClosureExpression) {
      visitClosureExpression(exp)
      // visitの呼び出しを単に分配する(なぜか自動的には分配されないため)
      Statement code = exp.getCode()
      if (code != null) code.visit(this)
      return exp
    }
    else if (exp.class == ConstructorCallExpression) {
      // transformExpressionに分配する
      return exp.transformExpression(this)
    }
    return exp.transformExpression(this)
  }
  
  @Override
  void visitClosureExpression(ClosureExpression closureNode) {
    insertClosureParameterLoggingAst(closureNode)
    closureNode.getCode().visit(this);
  }

  MethodNode savedMethodForHandleReturn;

  @Override
  void visitMethod(MethodNode method) {
    savedMethodForHandleReturn = method
    if (method.parameters.find { it.type.name == "java.lang.Object" } != null) {
      insertMethodParameterLoggingAst(method)
    }
    super.visitMethod(method)
  }

  private void insertMethodParameterLoggingAst(MethodNode method) {
    /*
      def method(param) {
        TypeLogger.logMethodArgs(...) <== insert this
      }
     */
    if (method.parameters.size() > 0) {
      method.getCode().getStatements().add(0, createMethodParameterTypeLoggingAst(method))
    }
  }
  
  private void insertClosureParameterLoggingAst(ClosureExpression closure) {
    /*
      ... { ... ->
        TypeLogger.logClosureArgs(...) <== insert this
      }
     */
    closure.getCode().getStatements().add(0, createClosureParameterTypeLoggingAst(closure))
  }

  private Statement createMethodParameterTypeLoggingAst(MethodNode method) {
    /* TypeLogger.logMethodArgs(...) <== generate this */
    return new ExpressionStatement(
      new StaticMethodCallExpression(
        new ClassNode(org.jggug.kobo.staticalizer.TypeLogger),
        "logMethodArgs",
        new ArgumentListExpression(
          new ConstantExpression(sourceUnit.name),
          new ConstantExpression(method.lineNumber),
          new ConstantExpression(method.columnNumber),
          new ConstantExpression(method.name),
          createMethodParameterTypeInfoList(method)
        )
      )
    )
  }

  private Statement createClosureParameterTypeLoggingAst(ClosureExpression closure) {
    /* TypeLogger.logClosureArgs(...) <== generate this */
    return new ExpressionStatement(
      new StaticMethodCallExpression(
        new ClassNode(org.jggug.kobo.staticalizer.TypeLogger),
        "logClosureArgs",
        new ArgumentListExpression(
          new ConstantExpression(sourceUnit.name),
          new ConstantExpression(closure.lineNumber),
          new ConstantExpression(closure.columnNumber),
          createClosureParameterTypeInfoList(closure)
        )
      )
    )
  }

  private ListExpression createMethodParameterTypeInfoList(MethodNode method) {
    /* TypeLogger.logMethodArgs(
         sourceFileName, lineNumber, columnNumber, methodName,
           [ ..., ..., ..., ] <== generate this
       ) */
    def result = new ListExpression()
    method.parameters.each { param ->
      result.addExpression(
        createParameterTypeInfo(param.name)
      )
    }
    result
  }
  
  private ListExpression createClosureParameterTypeInfoList(ClosureExpression closureNode) {
    /* TypeLogger.logClosureArgs(
         sourceFileName, lineNumber, columnNumber,
           [ ..., ..., ..., ] <== generate this
       ) */
    def result = new ListExpression()
    if (closureNode.parameterSpecified) {
      closureNode.parameters.each { param ->
        result.addExpression(
          createParameterTypeInfo(param.name)
        )
      }
    }
    else {
      // handle implicit closure parameter 'it'
      result.addExpression(
        createParameterTypeInfo("it")
      )
    }
    result
  }
  
  private ListExpression createParameterTypeInfo(String paramName) {
    /* TypeLogger.logArgs(sourceFileName, lineNumber, columnNumber, methodName,
         [
           [ ${paramName}.getClass().getName(), "${paramName}" ], <== generate this
             ...,
             ...,
         ]
       ) */
    def result = new ListExpression()
    result.addExpression(
      new MethodCallExpression(
        new MethodCallExpression(
          new VariableExpression(paramName),
          new ConstantExpression("getClass"),
          new ArgumentListExpression()
        ),
        new ConstantExpression("getName"),
        new ArgumentListExpression()
      )
    )
    result.addExpression(new ConstantExpression(paramName))
    result
  }

  @Override
  void visitReturnStatement(ReturnStatement statement) {
    if (savedMethodForHandleReturn.returnType.name == "java.lang.Object") {
      statement.setExpression(createReturnTypeLoggingAst(statement.getExpression()));
    }
    super.visitReturnStatement(statement);
  }

  Expression createReturnTypeLoggingAst(Expression expression) {
    /* TypeLogger.logReturn(...) <== generate this */
    return new StaticMethodCallExpression(
      new ClassNode(org.jggug.kobo.staticalizer.TypeLogger),
      "logReturn",
      new ArgumentListExpression(
        new ConstantExpression(sourceUnit.name),
        new ConstantExpression(savedMethodForHandleReturn.lineNumber),
        new ConstantExpression(savedMethodForHandleReturn.columnNumber),
        new ConstantExpression(savedMethodForHandleReturn.name),
        expression
      )
    )
  }
}
