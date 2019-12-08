
package main.graph;

import main.optimizer.Instruction;
import main.optimizer.Result;
import main.optimizer.SSAValue;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class DelUseChain {

    public Map<Instruction, List<Instruction>> xDefUseChains;
    public Map<Instruction, List<Instruction>> yDefUseChains;

    public DelUseChain() {
        xDefUseChains = new HashMap<>();
        yDefUseChains = new HashMap<>();
    }

    public void updateDefUseChain(Result left, Result right) {
        List<Instruction> useInstructions = new ArrayList<>();
        Instruction curInstr = null;
        if (left.type == Result.ResultType.variable) {
            curInstr = ControlFlowGraph.allInstructions.get(left.instrRef);
            Instruction leftLastUse = ControlFlowGraph.allInstructions.get(left.ssaVersion.getVersion());
            if (xDefUseChains.containsKey(leftLastUse)) {
                useInstructions = xDefUseChains.get(leftLastUse);
            }
            else {
                xDefUseChains.put(leftLastUse, useInstructions);
            }
            useInstructions.add(curInstr);
        }
        if (right.type == Result.ResultType.variable) {
            Instruction rightLastUse = ControlFlowGraph.allInstructions.get(right.ssaVersion.getVersion());
            if (yDefUseChains.containsKey(rightLastUse)) {
                useInstructions = yDefUseChains.get(rightLastUse);
            }
            else {
                useInstructions = new ArrayList<>();
                yDefUseChains.put(rightLastUse, useInstructions);
            }
            useInstructions.add(curInstr);
        }
    }

    public void updateXDefUseChains(SSAValue ssaDef, Instruction use) {
        updateXDefUseChains(ControlFlowGraph.allInstructions.get(ssaDef.getVersion()), use);
    }

    public void updateYDefUseChains(SSAValue ssaDef, Instruction use) {
        updateYDefUseChains(ControlFlowGraph.allInstructions.get(ssaDef.getVersion()), use);
    }

    public void updateXDefUseChains(Instruction def, Instruction use) {
        List<Instruction> useInstructions;
        if (xDefUseChains.containsKey(def)) {
            useInstructions = xDefUseChains.get(def);
        }
        else {
            useInstructions = new ArrayList<>();
            xDefUseChains.put(def, useInstructions);
        }
        useInstructions.add(use);
    }

    public void updateYDefUseChains(Instruction def, Instruction use) {
        List<Instruction> useInstructions;
        if (yDefUseChains.containsKey(def)) {
            useInstructions = yDefUseChains.get(def);
        }
        else {
            useInstructions = new ArrayList<>();
            yDefUseChains.put(def, useInstructions);
        }
        useInstructions.add(use);
    }

}
