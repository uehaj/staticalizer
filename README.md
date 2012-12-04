Staticalizer
======================

Staticalizer is a tool to support using static groovy.

Groovy 2.0's static groovy feature is easy to use and powerful way to get the benefit of static typing.
(Just put @CompileStatic!)

But if you have tons of dynamic typed groovy code, it is not easy to modify all of those code to be static.
Because:

- The type of method parameter is not obvious. Some method parameter might be different type with each invocation. In those case, you have to specify Loweest Upper Bound type among those types.
- You have to trace the method calls chain if a value is supplied by other method.

Basically Groovy's suport of type inference is not work with inter-method information.
So omitted type of method parameters and the type of value which other method returns are can't be inffered.
To put it the other way around, if method parameter types and method return type is be decided, type infference get a chance to work well.

By using staticalizer, you can feedback the information from runtime actual type of method/closure parameters and method return type information to source code. (Partially by hand.)

Install & Settings
---------------------

Download binary distribution of staticalizer jar (staticalizer-0.1-bin.jar) from [here](https://github.com/uehaj/staticalizer/downloads), and extract it anywhre. We call the directory 'STATICALIZER_HOME'. it is not neccessary to set that to environment variable.

And add 'STATICALIZER_HOME/bin' directory to your PATH environment variable.
For example, if you extract staticalizer to /tool/staticalizer, set:

    export PATH=/tool/staticalizer/bin:${PATH}

In addition, if you want to use annocation form, you have to put the jar file of
STATICALIZER_HOME/lib/staticalizer-x.y.jar into your CLASSPATH.

    $ export CLASSPATH=STATICALIZER_HOME/lib/staticalizer-0.1.jar

Getting started
------------------

Let's start from using staticalizer command.
The usage of staticalizer command is basically the same as usage of groovy command.
You can spacify any groovy options if you want.

    $ cat hello.groovy
    def foo(n) {
      Closure c = {a,b -> a+b}
      return c(n, n)
    }
    println foo(3)
    println foo(3.5)
    
    $ staticallizer hello.groovy
    6
    7.0
    Patch file 'staticalizer.patch' generated. Apply the patch with command line:
     % (cd /; patch -b -p0) < staticalizer.patch

Then 'staticalizer.patch' file would be generated on the current directory.
Verify it.

    % cat staticalizer.patch
    --- /work/hello.groovy 2012-01-04 12:01:17.000000000 +0900
    +++ /work/hello.groovy.changed 2012-01-04 12:01:17.000000000 +0900
    @@ -1,0 +1,1 @@
    +// TODO: Change method argument type: foo(java.lang.Number n)
    @@ -1,0 +2,1 @@
    +// TODO: Change return type: java.lang.Number foo(...)
    @@ -2,0 +4,1 @@
    +// TODO: Change closure argument type: { java.lang.Number a,java.lang.Number b -> .. }

If it is ok, do the command line displayed above.

    $ (cd /; patch -b -p0) < staticalizer.patch
    patching file /work/hello.groovy
 
Now, hello.groovy is modified.

    $ cat hello.groovy
    def foo(n) {
    // TODO: Change method argument type: foo(java.lang.Number n)
    // TODO: Change return type: java.lang.Number foo(...)
      Closure c = {a,b -> a+b}
    // TODO: Change closure argument type: { java.lang.Number a,java.lang.Number b -> .. }
      return c(n, n)
    }
    println foo(3)
    println foo(3.5)

Please modify hello.groovy by hand under those orders in the comment.
Like:

    Number foo(Number n) {
      Closure c = {Number a, Number b -> a+b}
      return c(n, n)
    }

Of course you would like to spefify @CompileStatic or @TypeChecked annotations.

    @CompileStatic
    Number foo(Number n) {
      return n*n
    }

Caution: If any methods or closure is not executed at all, staticalizer.patch will not be generated at all and not be modified.
    
AST Transformation Annotation
------------------------------------

Staticalizer also provides following local AST transformation annotation:

    staticalizer.transform.WithTypeLogging

You can use this annotation directly instead of using STATICALIZER_HOME/bin/staticalizer script.
This annotation can be specified with method or constructor definitions:

    import org.jggug.kobo.staticalizer.transform.WithTypeLogging
    class X {
      @WithTypeLogging
      int foo(i) {
        return 0
      }
    }

Or you can specify it on class definition.

    import org.jggug.kobo.staticalizer.transform.WithTypeLogging
    @WithTypeLogging
    class X {
      int foo(i) {
        return 0
      }
    }

In this case, all of the methods in the class is under effect of WithTypeLogging.

This annotation makes the specified method to record actual types of method/closure arguments and type of return value of method internally.
When the program exit, those recorded information are writen to 'staticalizer.patch' file.

Behind the hood, STATICALIZER_HOME/bin/staticalizer script just applies this AST transformation on the script to run.

When you use this annotation, you have to modify your CLASSPATH environment variable includes jar file 'STATICALIZER_HOME/lib/staticalizer-x.y.jar'.

    $ export CLASSPATH=STATICALIZER_HOME/lib/staticalizer-0.1.jar
    groovy test.groovy

or speify it on -cp option. e.g.

    groovy -cp STATICALIZER_HOME/lib/staticalizer-x.y.jar test.groovy

or you can copy the jar into ~/.groovy/lib/ directry.

Environment
-------------

Staticalize itself is pure java program. But the wrapper scripts staticalizer/staticalizer.bat are supplied in bourne shell script and Window batch file. Many unix like environment and windows can run it.

To apply the patch file generated by staticalizer, patch command or some tools which can handle unified-diff-form patch file have to be available.

License
----------

This software is licensed under the [Apache License, Version 2.0][Apache].
 
[Apache]: http://www.apache.org/licenses/LICENSE-2.0

