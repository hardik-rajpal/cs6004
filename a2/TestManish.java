class Node {
	Node f;
	Node g;
	Node foo2(Node f){
		return f.g.f;
	}
}

public class TestManish {
	public static Node global;

	public static void main(String[] args) {
		foo();
	}

	public static Node foo(){	// 14 15 16 17
		Node x = new Node();
		x.f = new Node();
		x.f.g = new Node(); 
		Node y = new Node(); 
		Node z = new Node();
		y.f = z;
		bar(x.f, y);
		return y.f;
	}

	public static Node bar(Node p1, Node p2){	// 25
		Node w = new Node();
		w.f = new Node(); 
		p2.f = w.f;		

		return p1;
	}

	public Node f1(){			// 32 34
		global = new Node();
		Node x = global.f;
		global.f = new Node();
		x.g = global.f;

		return x.g;
	}

	public Node f2(Node p){		// 42
		Node x = f1();
		x.f = new Node();

		return p;
	}

	public void f3(){			// 49
		Node x = new Node();
		Node y = new Node();

		x.g = y;
		y.f = x.g.f;

		field.f = x.g;
	}

	public void f4(){			// 66
        Node x = new Node();
        
        while (x != null){
            x = x.f;
        }
		//x->(O58,)
        Node y = x.f;
        x.g = x.f;
        y.g = new Node();
        x.g = f2(x.g);

        x = x.g;

		global = x.g;
	}

	public void f5(Node p){		// 78 81
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

	public Node f6(){			// 92 93 97 98 99 100 110 113
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

	public static Node[] arrtestManish(){		// 121 122 123
		Node[] global = new Node[5];
		global[1] = new Node();
		global[1].f = new Node();
		return global;
	}

	public Node[] arrParam(Node[] p){	// 130 131
		Node arr[] = new Node[5];

		arr[0] = new Node();
		arr[1].f = new Node();
	
		p[0] = arr[2];
		return p;
	}
	public void methodCallTest(){ //141 142 144 145 147 
		Node x = new Node();
		Node y = new Node();
		x.foo2(y);
		Node z = new Node();
		Node w = new Node();
		Node s = new Node();
		Node t = z.foo2(w);
		t.f = new Node();
		s.f = z;
	}
	
	public Node field;
}
