class Node {
	Node f;
	Node g;
	Node() {}
}
class Base{
	Node f;
	public void method1(Node x){

	}
}
class Derived extends Base{
	public void method1(Node x){

	}
}
public class Test {
	public static Node global;
	public static void main(String[] args) {
		foo();
	}
	public static Node ifElse(){
		Node x = new Node();
		if(x.f==x.g){
			x = new Node();
		}
		else{
			if(x.f!=x.g){
				x = new Node();
			}
		}
		x.f = new Node();
		return x.f;
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
