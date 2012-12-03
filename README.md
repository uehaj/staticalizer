Staticalizer
======================

Staticalizer is a tool to support static groovy.

Static groovy is easy to use and powerful way of to get the benefit of static typing.
(Just put @CompileStatic!)

But if you have tons of dynamic groovy code, it is not easy to modify all of those code to be static. Because:

- It is not obvious the type of method parameter. Some method parameter might have different type with each invocation. In those case, you have to specify Loweest Upper Bound type among those types.
- You have to trace the call chain trace if the value is supplied by other method.

 
Install
-------

Download source or binary distribution of staticalizer from (https://github.com/uehaj/staticalizer/downloads "here"),
and extract it anywhre. We call the directory 'STATICALIZER_HOME' (don't neccessary to set it as environment variable).

And add 'STATICALIZER_HOME/bin' directory to your PATH environment variable.

Getting started
------------------

Staticalizer command line is compatible to groovy command.
So you can spacify any groovy options if you want.

    $ cat hello.groovy
    def foo(n) {
     return n*n
    }
    println foo(2)
    println foo(10.0d)
    
    $ staticallizer hello.groovy
    4
    100.0
    Patch file 'staticalizer.patch' generated. Apply the patch with command line:
     % (cd /; patch -b -p0) < staticalizer.patch

Then 'staticalizer.patch' file is generated at current directory.
Verify it.

    % cat staticalizer.patch
    --- /work/staticalizer/hello.groovy 2012-48-04 07:48:40.000000000 +0900
    +++ /work/staticalizer/hello.groovy.changed 2012-48-04 07:48:40.000000000 +0900
    @@ -1,0 +1,1 @@
    +// TODO: Change method argument type: foo(java.lang.Number n)
    @@ -1,0 +2,1 @@
    +// TODO: Change return type: java.lang.Number foo(...)

If it is ok, do the command line displayed above.

    $ (cd /; patch -b -p0) < staticalizer.patch
    patching file /work/staticalizer/hello.groovy
 
hello.groovy is modified to:

    $ cat hello.groovy
    def foo(n) {
    // TODO: Change method argument type: foo(java.lang.Number n)
    // TODO: Change return type: java.lang.Number foo(...)
     return n*n
    }
    println foo(2)
    println foo(10.0d) 

Please modify declaration of method foo by hand.
    
Annotations
------------------

Staticalize provides following local AST transformation annotations:

    staticalizer.transform.WithTypeLogging

You can use this annotation directly instead of using STATICALIZER_HOME/bin/staticalizer script.
You can specify this annotations to method:

    import org.jggug.kobo.staticalizer.transform.WithTypeLogging
    class X {
      @WithTypeLogging
      int foo(i) {
        return 0
      }
    }

or specify it on class.

    import org.jggug.kobo.staticalizer.transform.WithTypeLogging
    @WithTypeLogging
    class X {
      int foo(i) {
        return 0
      }
    }

This annotation makes the specified method to logging types of method or closure arguments and type of return value of method internally.
When the program exit, those logged information are writen to 'staticalizer.patch' file.

Behind the hood, STATICALIZER_HOME/bin/staticalizer script just apply this AST transformation on the script to run.

To use this annocations, you have to put the jar file of
STATICALIZER_HOME/lib/staticalizer-x.y.jar into your CLASSPATH

    $ export CLASSPATH=STATICALIZER_HOME/lib/staticalizer-0.1.jar
    groovy test.groovy

or speify it on -cp option. e.g.

    groovy -cp STATICALIZER_HOME/lib/staticalizer-x.y.jar test.groovy

You can also copy the jar into .groovy/lib directry.
 
License
----------

Licensed under the [Apache License, Version 2.0][Apache]
 
[Apache]: http://www.apache.org/licenses/LICENSE-2.0

