class Node9 {
    Node9 f;
}

public class Test9 {
    public static void main(String[] args) {
        bar2(); // L1
        bar1(); // L2
    }
    static Node9 foo() {
        Node9 a = new Node9(); // O11
        return a;
    }
    static void bar1() {
        foo(); // L3
    }
    static void bar2() {
        foo(); // L4
    }
}