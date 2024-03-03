
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
		while(p.f==global){
			global = global.f;
		}
		Node x = new Node();
		global.f = new Node();
		return x;
	}
}
