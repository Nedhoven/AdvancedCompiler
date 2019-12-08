
package main.graph;

import main.frontend.Lexer;

import main.optimizer.Result;
import main.optimizer.SSAValue;

import java.util.*;

public class VariableTable {

    private static Set<Integer> GlobalVariableIdentifier;
    private static Map<Integer, List<SSAValue>> SSAUseChain;
    public static Map<Integer, Result> ArrayDefinition;

    public VariableTable() {
        GlobalVariableIdentifier = new HashSet<>();
        SSAUseChain = new HashMap<>();
        ArrayDefinition = new HashMap<>();
    }

    public static void addGlobalVariable(int var) {
        GlobalVariableIdentifier.add(var);
    }

    public static SSAValue getLatestVersion(int var) {
        if (!SSAUseChain.containsKey(var)) {
            System.out.println(Lexer.ids.get(var));
            Error("CANNOT FIND ADDRESS " + var + "!");
            return null;
        }
        else {
            return SSAUseChain.get(var).get(SSAUseChain.get(var).size() - 1);
        }
    }

    public static void addSSAUseChain(int var, int version) {
        if (!SSAUseChain.containsKey(var)) {
            SSAUseChain.put(var, new ArrayList<>());
        }
        SSAUseChain.get(var).add(new SSAValue(version));
    }

    public static void addSSAUseChain(int var, SSAValue ssa) {
        if (!SSAUseChain.containsKey(var)) {
            SSAUseChain.put(var, new ArrayList<>());
        }
        SSAUseChain.get(var).add(ssa);
    }

    public static Map<Integer, List<SSAValue>> cloneSSAUseChain() {
        Map<Integer, List<SSAValue>> clone = new HashMap<>();
        for (Map.Entry<Integer, List<SSAValue>> entry : SSAUseChain.entrySet()){
            clone.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return clone;
    }

    public static Set<Integer> getGlobalVariableIdentifier() {
        return GlobalVariableIdentifier;
    }

    public static void setGlobalVariableIdentifier(Set<Integer> set) {
        GlobalVariableIdentifier = set;
    }

    public static Map<Integer, List<SSAValue>> getSSAUseChain() {
        return SSAUseChain;
    }

    public static void setSSAUseChain(Map<Integer, List<SSAValue>> set) {
        SSAUseChain = set;
    }

    private static void Error(String msg) {
        System.err.println("VARIABLE TABLE ERROR: " + msg);
    }

}
