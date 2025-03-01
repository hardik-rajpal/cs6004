Classfile /C:/CODE/atgit/cs6004/test/Test.class
  Last modified May 1, 2024; size 467 bytes
  MD5 checksum c1b94e8a84ea7c302b1600519f44e2f6
  Compiled from "Test.java"
public class Test
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #9.#23         // java/lang/Object."<init>":()V
   #2 = Class              #24            // Test$Derived
   #3 = Methodref          #2.#23         // Test$Derived."<init>":()V
   #4 = Class              #25            // Test$Base
   #5 = Methodref          #4.#23         // Test$Base."<init>":()V
   #6 = Methodref          #4.#26         // Test$Base.callerNotOverridden:()V
   #7 = Methodref          #2.#26         // Test$Derived.callerNotOverridden:()V
   #8 = Class              #27            // Test
   #9 = Class              #28            // java/lang/Object
  #10 = Class              #29            // Test$Derived2
  #11 = Utf8               Derived2
  #12 = Utf8               InnerClasses
  #13 = Utf8               Derived
  #14 = Utf8               Base
  #15 = Utf8               <init>
  #16 = Utf8               ()V
  #17 = Utf8               Code
  #18 = Utf8               LineNumberTable
  #19 = Utf8               main
  #20 = Utf8               ([Ljava/lang/String;)V
  #21 = Utf8               SourceFile
  #22 = Utf8               Test.java
  #23 = NameAndType        #15:#16        // "<init>":()V
  #24 = Utf8               Test$Derived
  #25 = Utf8               Test$Base
  #26 = NameAndType        #30:#16        // callerNotOverridden:()V
  #27 = Utf8               Test
  #28 = Utf8               java/lang/Object
  #29 = Utf8               Test$Derived2
  #30 = Utf8               callerNotOverridden
{
  public Test();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 2: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=3, args_size=1
         0: new           #2                  // class Test$Derived
         3: dup
         4: invokespecial #3                  // Method Test$Derived."<init>":()V
         7: astore_1
         8: new           #4                  // class Test$Base
        11: dup
        12: invokespecial #5                  // Method Test$Base."<init>":()V
        15: astore_2
        16: aload_2
        17: invokevirtual #6                  // Method Test$Base.callerNotOverridden:()V
        20: aload_1
        21: invokevirtual #7                  // Method Test$Derived.callerNotOverridden:()V
        24: return
      LineNumberTable:
        line 26: 0
        line 27: 8
        line 28: 16
        line 29: 20
        line 30: 24
}
SourceFile: "Test.java"
InnerClasses:
     public static #11= #10 of #8; //Derived2=class Test$Derived2 of class Test
     public static #13= #2 of #8; //Derived=class Test$Derived of class Test
     public static #14= #4 of #8; //Base=class Test$Base of class Test
