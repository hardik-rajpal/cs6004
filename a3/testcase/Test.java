class Node {
	Node f;
	Node g;
	Node() {}
}
public class Test {
	public static void main(String[] args) {
		Node a = new Node();
		a.f = foo(args);
		Node b = a.f;
	}
	public static Node foo(String [] args){
		Node x = new Node();
		Node y = new Node();
		y.f = new Node();
		y = new Node();
		bar(x, y);
		Node z = y.f;
		bar(y, x);
		Node a = x.f;
		return x;
	}
	public static void bar(Node p1, Node p2){
		Node v = new Node();
		p1.f = v;	
	}
}
