class Node {
	Node f;
	Node g;
	int y;
	Node() {}
}
public class Test {
	public static Node global;
	public static void main(String[] args) {
		foo();
	}
	public static int returnsInt(Node p){
		p.f = new Node();
		return p.y;
	}
	public static Node foo(){
		Node x = new Node();
		Node y = new Node();
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
