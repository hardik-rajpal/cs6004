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
		ifelse();
		Node z = foo2();
		poly(new Base(),z);
	}
	public static void poly(Base b,Node z) {
		b.method1(z);
	}
	public static Node foo2(){
		Node x = new Node();
		return x;
	}
	public static Node ifelse(){
		Node x = new Node();
		x.f = new Node();
		Node y = new Node();
		Node z;
		if(x==null){
			z = y;
		}
		else{
			z = x;
		}
		return x;
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
/*
My output:
Base:method1 
Derived:method1 
Test:bar 
Test:foo 57:58 58:58 59:61 
Test:foo2 
Test:ifelse 36:39 
Test:main 14:24 15:24 24:24 30:24 34:22 35:22 56:20 66:20 9:24 
Test:makeNode 
Test:multiLineGC 47:51 
Test:poly 
*/