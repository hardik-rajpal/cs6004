import java.io.IOException;

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
		Integer a= new Integer(0);
		try{
		a	= System.in.read();
		}
		catch(IOException e){

		} 
		Node x = new Node();
		x.f = new Node();
		x.f.g = new Node(); 
		Node y = new Node(); 
		Node z = new Node();
		y.f = z;
		Integer b = a;
		if(a==b*b){
			bar(x.f, y);
		}
		return y.f;
	}
	public static void bar(Node p1, Node p2){
		Node w = new Node();
		w.f = new Node(); 
		p2.f = w.f;		
	}
}
