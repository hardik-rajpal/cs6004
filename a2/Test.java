
class Node {
	Node f;
	Node g;
}

public class Test {
	public static Node global;
	public static void main(String[] args) {
		foo(new Node());
	}
	public static Node foo(Node p){
		// Node x[] = new Node[10];//doesn't call constructor.
		// //no mention of specialinvoke in jimple code.
		// for(int i=0;i<10;i++){
		// 	x[i] = new Node();
		// }
		// Node w[] = new Node[5];
		// Node y = x[2];
		// y.f = new Node();
		// x[1] = new Node();
		// x = w;
		// return x[0];
		Node w = new Node();
		Node y = new Node();
		y.f = w;
		w.f = w;
		return w;
	}
}
