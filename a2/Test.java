
class Node {
	Node f;
	Node g;
}

public class Test {
	public Node global;
	public void main(String[] args) {
		foo();
	}
	public Node foo(){
		Node x = new Node();
		x.f = new Node();
		Node y = new Node();
		y.f = new Node();
		//comment out line 17 and check again.
		y = new Node();
		y.g = new Node();
		y.f = new Node();
		Node z = new Node();
		Node w = bar(y,z);
		//comment out line 23 and check again.
		w = new Node();
		w.f = new Node();
		global.f.g.g.f.f = new Node();
		global = new Node();
		
		Node a[] = new Node[10];
		if(a[1]==y){
			a[0] = new Node();//escapes.
		}
		else{
			a[2] = new Node();//escapes
		}
		
		global = a[1];
		return x;
	}
	public static Node bar(Node p1, Node p2){
		Node w = new Node();
		w.f = new Node(); 
		p2.f = w.f;	
		return p1;	
	}
	public static Node[] arrtest(){
		Node[] global = new Node[5];
		global[1] = new Node();
		global[1].f = new Node();
		return global;
	}
}
