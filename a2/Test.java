
class Node {
	Node f;
	Node g;
}

public class Test {
	public static Node global;
	public static void main(String[] args) {
		foo(new Node(), new Node());
	}
	public static Node foo(Node w, Node s){
		
		Node x,y;
		x = new Node();
		y = new Node();
		x.f = y;
		y.f = w;
		x.f = s.f;//shouldn't generate pt, but should kill.
		while(y==x){
			x = x.f;
		}
		return y.f;
	}
}
