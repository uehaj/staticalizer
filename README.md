Staticalizer
======================

Staticalizer is a tool to support static groovy.
 
Install
-------

Download source or binary distribution of staticalizer from (https://github.com/uehaj/staticalizer/downloads "here"),
and extract it anywhre. We call the directory 'STATICALIZER_HOME' (don't neccessary to set it as environment variable).

Add STATICALIZER_HOME/bin to your PATH environment variable.

Getting started
------------------

 % staticalizer hello.groovy

Staticalizer command line is compatible to groovy command.
So you can spacify any groovy options if you want.

 $ cat hello.groovy
 def foo(n) {
  return n*n
 }
 $ staticallizer hello.groovy
 4
 100.0
 Patch file 'staticalizer.patch' generated. Apply the patch with command line:
  % (cd /; patch -b -p0) < staticalizer.patch

Then 'staticalizer.patch' file is generated at current directory.
Verity it.

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
  
Annotations
------------------

Staticalize provides following AST transformation annotations:

 staticalizer.transform.WithTypeLogging

You can use this annotation directly instead of use STATICALIZER_HOME/bin/staticalizer script.
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

this annotation makes record parameter types and return value types and when the program exit,
put it those information onto 'staticalizer.patch' file.

Behind the food, STATICALIZER_HOME/bin/staticalizer script just apply
this AST transformation on the script to run.
 
License
----------

Licensed under the [Apache License, Version 2.0][Apache]
 
[Apache]: http://www.apache.org/licenses/LICENSE-2.0

