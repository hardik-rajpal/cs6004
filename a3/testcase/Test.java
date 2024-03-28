class Node {
	Node f;
	Node g;
	Node() {}
}
class Base{
	Node f;
	public void method1(Node x){
		x.f = new Node();
	}
}
class Derived extends Base{
	public void method1(Node x){
		x.g = new Node();
		x.g.f = new Node();
	}
}
public class Test {
	public static void main(String[] args) {
		foo();
		multiLineGC();
	}
	public static Node makeNode(){
		Node a = new Node(); //O1
		return a;
	}
	public static Node multiLineGC(){
		makeNode();
		Node x2 = makeNode();
		return x2;
	}
	public static Node foo(){
		Node x = new Node();
		Node y = new Node();
		y.f = new Node();
		y = new Node();
		bar(x, y);
		Node z = y.f;
		Node a = x.f;
		return x;
	}
	public static void bar(Node p1, Node p2){
		Node v = new Node();
		p1.f = v;	
	}
}
