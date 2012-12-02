package staticalizer.transform

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.control.*
import org.codehaus.groovy.transform.*

@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
public class TypeLoggingASTTransformation extends ClassCodeExpressionTransformer implements ASTTransformation {
  //public class TypeLoggingASTTransformation extends ClassCodeVisitorSupport implements ASTTransformation {

  SourceUnit sourceUnit = sourceUnit

  @Override
  SourceUnit getSourceUnit() {
    return sourceUnit;
  }

  @Override
  public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
    this.sourceUnit = sourceUnit
    def methodNode = nodes[1]
    visitMethod((MethodNode) methodNode);
    // followings are for global ast transformation
    // List methods = sourceUnit.getAST()?.getMethods()
    // // find all methods annotated with @WithTypeLogging
    // methods.findAll { MethodNode method ->
    //   method.getAnnotations(new ClassNode(WithTypeLogging))
    // }.each { MethodNode method ->
    //   insertParameterLoggingAst(method)
    // }
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
    println "[2]"
      return result
    }
    else if (exp.class == ClosureExpression) {
      visitClosureExpression(exp)
      println "[3]"
      // visitの呼び出しを単に分配する(なぜか自動的には分配されないため)
      Statement code = exp.getCode()
      if (code != null) code.visit(this)
    println "[4]"
      return exp
    }
    else if (exp.class == ConstructorCallExpression) {
    println "[5]"
      // transformExpressionに分配する
      return exp.transformExpression(this)
    }
    println "[6]"
    return exp.transformExpression(this)
  }
  
  @Override
  void visitClosureExpression(ClosureExpression closureNode) {
    println "visitClosureExpression($closureNode)"
    insertClosureParameterLoggingAst(closureNode)
    closureNode.getCode().visit(this);
  }

  MethodNode savedMethodForHandleReturn;

  @Override
  void visitMethod(MethodNode method) {
    println "visitMethod($method)"
    savedMethodForHandleReturn = method
    insertMethodParameterLoggingAst(method)
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
        new ClassNode(staticalizer.TypeLogger),
        "logMethodArgs",
        new ArgumentListExpression(
          new ConstantExpression(sourceUnit.name),
          new ConstantExpression(method.lineNumber),
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
        new ClassNode(staticalizer.TypeLogger),
        "logClosureArgs",
        new ArgumentListExpression(
          new ConstantExpression(sourceUnit.name),
          new ConstantExpression(closure.lineNumber),
          createClosureParameterTypeInfoList(closure)
        )
      )
    )
  }

  private ListExpression createMethodParameterTypeInfoList(MethodNode method) {
    /* TypeLogger.logMethodArgs(
         sourceFileName, lineNumber, methodName,
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
         sourceFileName, lineNumber,
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
    /* TypeLogger.logArgs(sourceFileName, lineNumber, methodName,
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
      new ClassNode(staticalizer.TypeLogger),
      "logReturn",
      createLoggingReturn(expression)
    )
  }
  
  Expression createLoggingReturn(Expression expression) {
    /* TypeLogger.logReturn(
         sourceFileName, lineNumber, methodName, expression <== generate this
       )
    */
    new ArgumentListExpression(
      new ConstantExpression(sourceUnit.name),
      new ConstantExpression(savedMethodForHandleReturn.lineNumber),
      new ConstantExpression(savedMethodForHandleReturn.name),
      expression
    )
  }
}
