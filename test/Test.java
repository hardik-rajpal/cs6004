
public class Test{
    public static class Base{
        void overridden(){
            System.out.println("Base function to be overridden");
        }
        static void  overshadowed(){
            System.out.println("Base function to be overshadowed.");
        }
    }
    public static class Derived extends Base{
        void overridden(){
            System.out.println("Derived function overrides");
        }
        static void overshadowed(){
            System.out.println("Derived function overshadows");
        }
    }
    public static class Derived2 extends Base{

    }
    public static void main (String[] args){
        Derived2 d2 = new Derived2();
        Derived d = (Derived)(d2);
    }
}