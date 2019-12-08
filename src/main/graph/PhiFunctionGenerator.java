
package main.graph;

import main.frontend.Lexer;

import main.optimizer.Instruction;
import main.optimizer.InstructionType;
import main.optimizer.SSAValue;

import java.util.Map;
import java.util.HashMap;

public class PhiFunctionGenerator {

    private Map<Integer, Instruction> phiInstructionMap;

    public PhiFunctionGenerator() {
        phiInstructionMap = new HashMap<>();
    }

    public Instruction addPhiFunction(int address, SSAValue SSA) {
        if (phiInstructionMap.containsKey(address)) {
            return phiInstructionMap.get(address);
        }
        Instruction returnIns = new Instruction(InstructionType.PHI, address, SSA, SSA);
        phiInstructionMap.put(address,returnIns);
        ControlFlowGraph.delUseChain.updateXDefUseChains(SSA,returnIns);
        ControlFlowGraph.delUseChain.updateYDefUseChains(SSA,returnIns);
        return returnIns;
    }

    public void updatePhiFunction(int address,SSAValue SSA,PhiFunctionUpdateType type) {
        if (!phiInstructionMap.containsKey(address)) {
            Error("UNABLE TO FIND PHI FUNCTION FOR VARIABLE " + Lexer.ids.get(address) + "!");
        }
        else {
            Instruction ins = phiInstructionMap.get(address);
            if (type == PhiFunctionUpdateType.LEFT) {
                ins.setLeftLatestUpdated(true);
                ins.setLeftSSA(SSA);
                ControlFlowGraph.delUseChain.updateXDefUseChains(SSA, ins);
            }
            else {
                ins.setLeftLatestUpdated(false);
                ins.setRightSSA(SSA);
                ControlFlowGraph.delUseChain.updateYDefUseChains(SSA,ins);
            }
        }
    }

    public Map<Integer, Instruction> getPhiInstructionMap() {
        return phiInstructionMap;
    }

    private void Error(String msg) {
        System.err.println("PHI FUNCTION GENERATOR FAILED: " + msg);
    }

}
