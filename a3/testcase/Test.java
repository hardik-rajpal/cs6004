class Node {
	Node f;
	Node g;
	int y;
	Node() {}
}
class Base{
	Base x;
	void foo(Node x){
		x.f = new Node();
	}
}
class Derived extends Base{
	Derived y;
	void foo(Node x){
		x.g = new Node();
	}
}
public class Test {
	public static Node global;
	public static void main(String[] args) {
		foo();
	}
	public static void bar(Base b, Node x){
		b.foo(x);
	}
	public static Node foo(){
		Base b;
		Node x = new Node();
		if(x.f==x.f.f){
			b = new Derived();
			bar(b, x);
		}
		// else{
		// 	b = new Base();
		// 	bar(b, x);
		// }
		return x;
	}
}
