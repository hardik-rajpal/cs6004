//inheritence, possibly null reference.
//not all members live.
public class tc4 {
    public static class Animal{
        int teethCount;
    }
    public static class Dog extends Animal{
        int tailLength;
        Animal prey;
    }
    public static class Labrador extends Dog{
        int bloodPurity;
    }
    public static void main() {
        Animal pet = new Labrador();
        
        pet.teethCount = 32;
        ((Dog)pet).tailLength = 100;
        ((Labrador)pet).bloodPurity = 99;


        if(((Labrador)pet).bloodPurity>90){
            ((Dog)pet).prey = new Dog();
            //dog eat dog.
        }
        System.out.println("Woof, grin with: "+pet.teethCount+ " teeth");
    }
}