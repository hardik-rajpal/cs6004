
class Node {
	Node f;
	Node g;
}

public class Test {
	public static Node global;
	public int x;
	public static void main(String[] args) {
		foo(new Node());
	}
	public static void bar(Node y){
		y = global.f;
	}
	public static Node foo(Node p){
		Node x = new Node();
		x.f = new Node();
		global = new Node();
		global.f = new Node();
		Node y = new Node();
		bar(y);
		return x.f;
	}
}
