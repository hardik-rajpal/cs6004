class Node {
	Node f;
	Node g;

	Node(){
		f = null;
		g = null;
	}

	Node(Node x){
		g = null;
		g = null;
	}

	public void m1(Node x){}
	public Node m2(Node f){
		return f.g.f;
	}
}

public class TestManish {
	public static Node global;
	public static void main(String[] args) {
		foo();
	}

	public static Node foo(){	// 29 30 31 32
		Node x = new Node();
		x.f = new Node();
		x.f.g = new Node(); 
		Node y = new Node(); 
		Node z = new Node();
		y.f = z;
		bar(x.f, y);
		return y.f;
	}

	public static Node bar(Node p1, Node p2){	// 40
		Node w = new Node();
		w.f = new Node(); 
		p2.f = w.f;		

		return p1;
	}

	public Node f1(){			// 47 49
		global = new Node();
		Node x = global.f;
		global.f = new Node();
		x.g = global.f;

		return x.g;
	}

	public Node f2(Node p){		// 57
		Node x = f1();
		x.f = new Node();

		return p;
	}

	public void f3(){			// 64
		Node x = new Node();
		Node y = new Node();

		x.g = y;
		y.f = x.g.f;

		field.f = x.g;
	}

	public void f4(){		
        Node x = new Node();
        
        while (x != null){
            x = x.f;
        }

        Node y = x.f;
        x.g = x.f;
        y.g = new Node();
        x.g = f2(x.g);

        x = x.g;

		global = x.g;
	}

	public void f5(Node p){		// 93 96
		Node x = new Node();
		Node y = x;

		y.f = new Node();

		if (x.f != null){
			y.g = new Node();
		} else if (x.g != null){
			x.g = y.g;
		} else {
			x.g = y.f;
		}

		p.g = x.g;
	}

	public Node f6(){			// 107 108 112 113 114 115 121 122 126 129
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

	public static Node[] arrTestManish(){		// 137 138 139
		Node[] global = new Node[5];
		global[1] = new Node();
		global[1].f = new Node();
		return global;
	}

	public Node[] arrParam(Node[] p){	// 146 147
		Node arr[] = new Node[5];

		arr[0] = new Node();
		arr[1].f = new Node();
	
		p[0] = arr[2];
		return p;
	}

	protected Node f7(){				// 155 156
		Node x = new Node();
		x.f = new Node();
		x.f.g = new Node();

		Node y = new Node();
		Node z = new Node();

		if (true){
			y.f = x.f;
		} else {
			y.f = z;
		}

		f7();
		return y.f;
	}

	protected void f8(Node p){			// 172	
		Node x = new Node();

		field = x;
		f8(p);
	}

	public static Node f9(){			// 180 181 183
		Node x = new Node();
		x.f = new Node();
		x.f.g = new Node(); 
		Node y = new Node();
		Node z = new Node();
		
		f10(x.f, y.f);
		
		y.f = z;

		x.f = new Node();

		return y.f;	
	}

	public static void f10(Node p1, Node p2){	// 196
		Node w = new Node();
		w.f = new Node(); 
		p2.f = w.f;
	}

	public static Node f11(){					// 201 202
		Node x = new Node();
		Node y = new Node();
		global.f = x;

		return y;
	}

	public static void f12(){					// 209 210 211 219
		Node x = new Node();
		x.f = new Node();
		x.f.g = new Node(); 
		Node y = new Node();
		Node z = new Node();
		
		global = x.f;
		
		y.f = z;

		x.f = new Node();

		global = x;
	}

	public Node f13(Node p){					// 225 233
		Node x = new Node();
		while (x != null){
			x = x.f;
		}

		if (p == x) 
			x = p.f;
		
		p.f = new Node();

		return x;
	}

	public Node f14(Node p){					// 245
		Node x = new Node(); 
		while (x != null){
			x = x.f;
		}

		x = p.f;
		p.f = new Node(); 

		return x;
	}

	public void f15(){							// 252 254 256
		Node x = new Node();
		Node y = new Node(x);

		Node z = new Node();
		y.f = z;
		y.m1(new Node());
	}

	public void methodCallTestManish(){ 				// 260 261 263 264 267
		Node x = new Node();
		Node y = new Node();
		x.m2(y);
		Node z = new Node();
		Node w = new Node();
		Node s = new Node();
		Node t = z.m2(w);
		t.f = new Node();
		s.f = z;
	}

	public Node field;
}

/*
TestManish:arrParam 146 147
TestManish:arrTestManish 137 138 139
TestManish:bar 40
TestManish:f1 47 49
TestManish:f10 196
TestManish:f11 201 202
TestManish:f12 209 210 211 219
TestManish:f13 225 233
TestManish:f14 245
TestManish:f15 252 254 256
TestManish:f2 57
TestManish:f3 64
TestManish:f5 93 96
TestManish:f6 107 108 112 113 114 115 121 122 126 129
TestManish:f7 155 156
TestManish:f8 172
TestManish:f9 180 181 183
TestManish:foo 29 30 31 32
TestManish:methodCallTestManish 260 261 263 264 267 
*/
