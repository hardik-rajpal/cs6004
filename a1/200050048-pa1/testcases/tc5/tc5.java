//recursion with faked liveness.
public class tc5 {
    public static int recurse(int i){
        if(i<0){
            return 42;
        }
        return i+recurse(i-1);
    }
    public static void main() {
        int a = recurse(10);
        int b = recurse(11);
        System.out.println("Only reading a: "+a);
        if(b<10){
            a = b;
        }
    }
}