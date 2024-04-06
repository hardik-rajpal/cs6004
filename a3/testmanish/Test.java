class Node {
	Node f;
	Node g;
	Node() {}
	Node(Node x, Node y){}

	void goo(){}
}

public class Test {
	public static Node global;
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

		// x.g = f3(x);
	}

	public static Node baz(){
		Node x = new Node();
		x.f = new Node();
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
		f3(x);
		return x;
	}

	public static Node f3(Node p){
		Node x = new Node();
		Node y = new Node(x, p);

		return y;
	}
}

/* What I get
Node:goo
Test:bar 42:42 49:43 50:43 63:45
Test:baz 51:52 53:53 58:53
Test:f1
Test:f2
Test:foo 19:33 20:21 21:21 25:30 28:34 31:34 39:33
Test:main 22:16
*/
