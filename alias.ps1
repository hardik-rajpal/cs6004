function sootup { java -cp sootclasses-trunk-jar-with-dependencies.jar soot.Main -pp -cp $args }
# usage: sootup <dir containing .class file> -f <jimple/etc options> <filename w/o .class>