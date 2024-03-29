class Base {
	void goo(){}
}

class Node /* extends Base */ {
	Node f;
	Node g;
	Node() {}

	void goo(){}
}

public class Test {
	public static void main(String[] args) {
		Node ret = foo();
	}
	public static Node foo(){
		Node x = new Node();
		Node y = new Node();
		y.f = new Node();
		y = new Node();
		bar(x, y);

		Node z = new Node();
		if (y != null){
			Node a = z.f;
			z = new Node();
		} else {
			Node b = z.f;
			z = new Node();
		}
		Node a = x.f;
		z.goo();
		return y;
	}

	public static void bar(Node p1, Node p2){
		Node v = new Node();
		p1.f = v;	
		Node x = baz();
		x.f = new Node();			
		baz();						// ignoring ret val

		x = f2(p2);					// catching ret val
		x.f = f2(p2);
	}

	public static Node baz(){
		Node x = new Node();
		Node y = new Node();
		y = f1(y);
		y.f = new Node();
		return x;
	}

	public static Node f1(Node p){
		Node x = new Node();
		return x;
	}

	public static Node f2(Node p){
		Node x = new Node();
		return x;
	}
}

/* What I get
Test:baz 50:54 51:52 53:53 58:53
Test:bar 42:42 63:46
Test:foo 19:33 20:21 21:21 25:30 28:34 31:34 39:33
Test:main 22:16
*/
