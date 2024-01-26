//linked list with only the head live.
//multiple may-points-to info at line 22

public class tc3 {
    public static class Node{
        public Node next;
        public int val;
    };
    public static void main() {
        Node head = new Node();
        Node start = head;
        int i = 0;
        while(i<10){
            head.next = new Node();
            head.val = i;
            head = head.next;
            i++;
        }
        if(i==10){
            start.next = new Node();
            start.next.val = i*i;
        }
        System.out.println(start.val+"->"+start.next.val);
    }
}