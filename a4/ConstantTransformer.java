import soot.SootMethod;

public class ConstantTransformer {
    public static void transformProgram(SootMethod mainMethod, ConstantPropagation cp){
        /*
         backwards:
         for unit in body...
             for use in useboxes:
                if(cp.before(unit).get(use) is constant){
                    replace use by constant value...
                }

                //TODO: confirm this means use boxes of def unit are removed.

            def = defboxes(unit).first
            if(def.uses.size()==0){
                body.remove(unit)
            }
        */
    }
}
