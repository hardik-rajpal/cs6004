package testcases;

public class T1 {
    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        int c = 3;
        a = a+1;//dead code
        b = b*2;//dead code
        if(a==b){
            a = a+1;//dead code
        }
        System.out.println(c);
    }
}
