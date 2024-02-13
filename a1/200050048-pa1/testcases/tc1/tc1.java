// if-else testcase with one dead variable and one line of dead code.
// Only primitive vars=> no null check necessary.
public class tc1{
    public tc1(){
        System.out.println("Contructor");
    }
    public static void main(String args[]){
        int x,y,z,w;
        y = 0;
        z = 1;
        x = y+1;
        z = x+2;
        w = 1;
        if(x<3){
            z = z+1;
        }
        else{
            z = x+y;
        }
        System.out.println(""+x+","+y+","+z);
    }
}