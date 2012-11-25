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
  SourceUnit getSourceUnit() {
    return sourceUnit;
  }

  public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
    this.sourceUnit = sourceUnit
    def methodNode = nodes[1]
    insertParameterLoggingAst(methodNode)
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

  private void insertParameterLoggingAst(MethodNode method) {
    /*
      def method(param) {
        ... <==
      }
     */
    method.getCode().getStatements().add(0, createParameterTypeLoggingAst(method))
  }

  private Statement createParameterTypeLoggingAst(MethodNode method) {
    /* TypeLogger.logArgs(...) <== */
    return new ExpressionStatement(
      new StaticMethodCallExpression(
        new ClassNode(staticalizer.TypeLogger),
        "logArgs",
        createLoggingArgs(method)
      )
    )
  }

  private ArgumentListExpression createLoggingArgs(MethodNode method) {
    /* TypeLogger.logArgs(
         sourceFileName, lineNumber, methodName, ...  <== 
       ) */
    new ArgumentListExpression(
      new ConstantExpression(sourceUnit.name),
      new ConstantExpression(method.lineNumber),
      new ConstantExpression(method.name),
      createParameterTypeInfoList(method)
    )
  }
  
  private ListExpression createParameterTypeInfoList(MethodNode method) {
    /* TypeLogger.logArgs(sourceFileName, lineNumber, methodName,
         [ ..., ..., ..., ] <==
       ) */
    def result = new ListExpression()
    method.parameters.each { param ->
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

  Expression transform(Expression exp) {
    return super.transform(exp)
  }
  
  MethodNode method;

  @Override
  void visitMethod(MethodNode methodNode) {
    println "visitMethod($methodNode)"
    method = methodNode
    if (methodNode.returnType.name == "java.lang.Object") {
      super.visitMethod(methodNode)
    }
  }

  @Override
  void visitReturnStatement(ReturnStatement statement) {
    statement.setExpression(createReturnTypeLoggingAst(statement.getExpression()));
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
