package testcases;
/*
 Function is pure if:
 1. No non-local modifications:
    for unit in body:
        for(Value v: unit.getDefboxes()){
            if(v of staticfieldref type)
            or(v uses this...)
            return false;
        }
 2. No parameter modifications:
    2.1 parameters are primitives
    or
    2.2 parameters' pointees are not defined [?]
 3. No calls to non-pure other functions.
 4. all library functions (unavailable for inter-proc analysis) are impure.
 calls to pure functions w/o using return value.
 */
public class T1 {
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
        a = a+1;//dead code
        b = b*2;//dead code
        c = c+1*c-2;//dead code
        a = b+c;
        c = a+b;
        // if(a==b){
        //     a = a+1;//dead code
        // }
        // foo(a,b);
        System.out.println(c+a+b);
    }
}
