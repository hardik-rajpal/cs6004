function sootup { java -cp soot-4.4.1-jar-with-dependencies.jar soot.Main -pp -cp $args }
# usage: sootup <dir containing .class file> -f <jimple/etc options> <filename w/o .class>