package testcases;

public class T2 {
    // [[pure]]
    static int foo(int a, int b){
        int c = a;
        c = c+1;
        if(c==b){
            c = c+1;
        }
        return a+b;
    }
    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        int c = 3;
        a = a+1;
        b = b*2;
        c = c+1*c-2;
        a = b+c;
        c = a+b;
        System.out.println(c+a+b);
    }
}
