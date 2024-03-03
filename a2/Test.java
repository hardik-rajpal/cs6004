
class Node {
	Node f;
	Node g;
}

public class Test {
	public static Node global;
	public Node x;
	public static void main(String[] args) {
		foo(new Node());
	}
	public static void bar(Node y){
		y = global.f;
	}
	public static Node foo(Node p){
		Node x = new Node();//escapes
		global.f = new Node();//escapes.
		global = new Node();//escapes
		global.f = new Node();//escapes.
		return x;
	}
}
