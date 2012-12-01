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
    insertParameterLoggingAst(closureNode)
    closureNode.getCode().visit(this);
  }

  MethodNode method;

  @Override
  void visitMethod(MethodNode methodNode) {
    println "visitMethod($methodNode)"
    method = methodNode
    insertParameterLoggingAst(methodNode)
    super.visitMethod(methodNode)
  }

  private void insertParameterLoggingAst(methodOrClosure) {
    /*
      def method(param) {
        ... <==
      }
     */
    println "------------$methodOrClosure---------"
    println "------------<<<${methodOrClosure.parameters}>>---------"
    if (!(methodOrClosure instanceof MethodNode) || methodOrClosure.parameters.size() > 0) {
      println "------------${methodOrClosure.parameters}---------"
      methodOrClosure.getCode().getStatements().add(0, createParameterTypeLoggingAst(methodOrClosure))
    }
  }

  private Statement createParameterTypeLoggingAst(methodOrClosure) {
    /* TypeLogger.logArgs(...) <== */
    return new ExpressionStatement(
      new StaticMethodCallExpression(
        new ClassNode(staticalizer.TypeLogger),
        "logArgs",
        createLoggingArgs(methodOrClosure)
      )
    )
  }

  private ArgumentListExpression createLoggingArgs(methodOrClosure) {
    /* TypeLogger.logArgs(
         sourceFileName, lineNumber, methodName, ...  <== 
       ) */
    new ArgumentListExpression(
      new ConstantExpression(sourceUnit.name),
      new ConstantExpression(methodOrClosure.lineNumber),
      new ConstantExpression(methodOrClosure instanceof MethodNode ? methodOrClosure.name : "<closure>"),
      createParameterTypeInfoList(methodOrClosure)
    )
  }

  private ListExpression createParameterTypeInfoList(methodOrClosure) {
    /* TypeLogger.logArgs(sourceFileName, lineNumber, methodName,
         [ ..., ..., ..., ] <==
       ) */
    def result = new ListExpression()
    methodOrClosure.parameters.each { param ->
      result.addExpression(
        createParameterTypeInfo(param)
      )
    }
    result
  }
  
  private ListExpression createParameterTypeInfo(Parameter param) {
    /* TypeLogger.logArgs(sourceFileName, lineNumber, methodName,
         [
           [ ${param.name}.getClass().getName(), "${param.name}" ],   <==
             ...,
             ...,
         ]
       ) */
    def result = new ListExpression()
    result.addExpression(
      new MethodCallExpression(
        new MethodCallExpression(
          new VariableExpression(param.name),
          new ConstantExpression("getClass"),
          new ArgumentListExpression()
        ),
        new ConstantExpression("getName"),
        new ArgumentListExpression()
      )
    )
    result.addExpression(new ConstantExpression(param.name))
    result
  }

  @Override
  void visitReturnStatement(ReturnStatement statement) {
    if (method.returnType.name == "java.lang.Object") {
      statement.setExpression(createReturnTypeLoggingAst(statement.getExpression()));
    }
    super.visitReturnStatement(statement);
  }

  Expression createReturnTypeLoggingAst(Expression expression) {
    /* TypeLogger.logReturn(...) <== */
    return new StaticMethodCallExpression(
      new ClassNode(staticalizer.TypeLogger),
      "logReturn",
      createLoggingReturn(expression)
    )
  }
  
  Expression createLoggingReturn(Expression expression) {
    /* TypeLogger.logReturn(
         sourceFileName, lineNumber, methodName, expression   <==
       )
    */
    new ArgumentListExpression(
      new ConstantExpression(sourceUnit.name),
      new ConstantExpression(method.lineNumber),
      new ConstantExpression(method.name),
      expression
    )
  }
}
