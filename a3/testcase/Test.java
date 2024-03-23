class Node {
	Node f;
	Node g;
	int y;
	Node() {}
}
// class Base{
// 	Base x;
// 	void foo(Node x){
// 		x.f = new Node();
// 	}
// }
// class Derived extends Base{
// 	Derived y;
// 	void foo(Node x){
// 		x.g = new Node();
// 	}
// }
public class Test {
	public static Node global;
	public static void main(String[] args) {
		foo();
	}
	public static Node foo(){
		Node x = new Node();
		Node y = new Node();
		if(y.f==y.g){
			if(y.f==y.g){
				x = new Node();
			}
			else{
				x = new Node();
			}
		}
		return x;
	}
}
