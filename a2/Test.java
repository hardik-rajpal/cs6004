
class Node {
	Node f;
	Node g;
}

public class Test {
	public static Node global;
	public static void main(String[] args) {
		foo();
	}
	public static Node foo(){
		
		Node x,y;
		x = new Node();
		y = new Node();
		while(y==x){
			x = x.f;
		}
		return y.f;
	}
}
