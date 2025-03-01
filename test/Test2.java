public class Test2{
    static double getA(){
        return Math.random();
    }
    public static void main(String[] args) {
        int a,b,c;
        a = (int)Math.round(getA());
        if(a==1){
            b = 2;
        }
        else{
            b = 3;
        }
        if(b==1){
            a = 2;
        }
        else{
            a = 5;
        }
        c = 1;
        System.out.println(c);
    }
}