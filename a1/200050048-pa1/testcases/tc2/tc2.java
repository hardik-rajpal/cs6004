//case with array and break statement in a for loop.
//liveness can only be determined with semantic deductions.
//no nullref possible.
public class tc2 {
    public static void main() {
        int a[] = new int[10];
        int x = 1;
        for (int i = 0; i < 10; i++) {
            a[i] = i * i;
            if (i + a[i] > 2 * i) {
                x = a[i];
                break;
            }
        }
        a[0] = x;
        System.out.println(a[0] + ',' + x + ',' + a[9]);
    }
}