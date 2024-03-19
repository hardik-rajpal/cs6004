class Node {
	Node f;
	Node g;
	Node() {}
}
public class Test {
	public static void main(String[] args) {
		foo(args);
	}
	public static Node foo(String [] args){
		Node x = new Node();
		Node y = new Node();
		y.f = new Node();
		y = new Node();
		bar(x, y);
		bar(y, x);
		Node z = y.f;
		Node a = x.f;
		return x;
	}
	public static void bar(Node p1, Node p2){
		Node v = new Node();
		p1.f = v;	
	}
}
