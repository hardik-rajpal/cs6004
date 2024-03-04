
class Node {
	Node f;
	Node g;
}
class Man{
	int h;
}
public class Test {
	public static Node[] global;
	public Node x;
	public Man y;
	public void main(String[] args) {
		foo(new Node());
	}
	public static Node bar(Node y){
		y = global[0].f;
		return global[0];
	}
	public Node foo(Node p){
		Man m = new Man();
		global[0].f.f.f.f.g.f.g = new Node();
		m.h = y.h+1;
		y = m;	
		return new Node();
	}
}
