
package main.optimizer;

import main.backend.DominatorTreeNode;
import main.backend.ExpressionNode;

import java.util.Map;
import java.util.HashMap;

public class CSE {

    private Map<InstructionType, Map<ExpressionNode, Integer>> cse;

    public CSE() {
        cse = new HashMap<>();
        cse.put(InstructionType.NEG, new HashMap<>());
        cse.put(InstructionType.ADD, new HashMap<>());
        cse.put(InstructionType.MUL, new HashMap<>());
        cse.put(InstructionType.DIV, new HashMap<>());
        cse.put(InstructionType.SUB, new HashMap<>());
        cse.put(InstructionType.ADDA, new HashMap<>());
        cse.put(InstructionType.LOADADD, new HashMap<>());
        cse.put(InstructionType.STOREADD, new HashMap<>());
    }

    public void optimize(DominatorTreeNode root) {
        optimizeRecursion(root, new HashMap<>());
    }

    private void optimizeRecursion(DominatorTreeNode root, Map<Integer, Integer> map) {
        if (root == null) {
            return;
        }
        for (Instruction ins : root.block.getInstructions()) {
            if (ins.deleted) {
                continue;
            }
            Result left = ins.getLeftResult();
            Result right = ins.getRightResult();
            if (left != null && left.type == Result.ResultType.instruction && map.containsKey(left.instrRef)) {
                left.instrRef = map.get(left.instrRef);
            }
            if (right != null && right.type == Result.ResultType.instruction && map.containsKey(right.instrRef)) {
                right.instrRef = map.get(right.instrRef);
            }
            if (ins.isExpressionOp()) {
                Map<ExpressionNode, Integer> tempExp = cse.get(ins.getOp());
                ExpressionNode curExp = new ExpressionNode(left, right);
                if (!tempExp.containsKey(curExp)) {
                    tempExp.put(curExp, ins.getInstructionPC());
                    Instruction next = root.block.getNextInstruction(ins);
                    if (next != null && !next.deleted && next.getOp() == InstructionType.MOVE) {
                        next.setState(Instruction.State.REPLACE);
                        next.referenceInstrId = ins.getInstructionPC();
                    }
                }
                else {
                    ins.deleted = true;
                    Instruction next = root.block.getNextInstruction(ins);
                    if (next != null && !next.deleted && next.getOp() == InstructionType.MOVE) {
                        next.setState(Instruction.State.REPLACE);
                        next.referenceInstrId = tempExp.get(curExp);
                    }
                    map.put(ins.getInstructionPC(), tempExp.get(curExp));
                }
            }
        }
        for (Map.Entry<Integer, Instruction> entry : root.block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
            Instruction instr = entry.getValue();
            Result left = instr.getLeftResult();
            Result right = instr.getRightResult();
            if (left != null && left.type == Result.ResultType.instruction && map.containsKey(left.instrRef)) {
                left.instrRef = map.get(left.instrRef);
            }
            if (right != null && right.type == Result.ResultType.instruction && map.containsKey(right.instrRef)) {
                right.instrRef = map.get(right.instrRef);
            }
        }
        if (root.block.getBackBlock() != null) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getBackBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                Instruction instr = entry.getValue();
                Result left = instr.getLeftResult();
                Result right = instr.getRightResult();
                if (left != null && left.type == Result.ResultType.instruction && map.containsKey(left.instrRef)) {
                    left.instrRef = map.get(left.instrRef);
                }
                if (right != null && right.type == Result.ResultType.instruction && map.containsKey(right.instrRef)) {
                    right.instrRef = map.get(right.instrRef);
                }
            }
        }
        for (DominatorTreeNode child : root.getChildren()) {
            optimizeRecursion(child, new HashMap<>(map));
        }
    }

}
