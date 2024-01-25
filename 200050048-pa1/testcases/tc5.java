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
        System.out.println(a[0] + ',' + x);
    }
}