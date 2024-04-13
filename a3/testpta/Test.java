class Node{
    Node f;
}
public class Test {
    public static void main(String[] args) {
        Node a = new Node(); //O1
        a.f = new Node(); //O2
        a.f.f = new Node(); // O3
        a.f.f.f = new Node(); //O4
        a.f.f.f.f = new Node(); //O5
        a.f.f.f.f.f = new Node(); //O6
        Node b = foo(a);
        a = new Node(); //O7
        // Lots of other code
    }

    static Node foo(Node x) {
        while(x.f==x.f.f) {
            x = x.f;
        }
        return x;
    }
}