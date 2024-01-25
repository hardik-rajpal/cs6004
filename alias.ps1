function sootup { java -cp soot.jar soot.Main -pp -cp $args }
# usage: sootup <dir containing .class file> -f <jimple/etc options> <filename w/o .class>