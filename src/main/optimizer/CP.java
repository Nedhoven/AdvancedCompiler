
package main.optimizer;

import main.backend.BlockType;
import main.backend.DominatorTreeNode;

import java.util.HashMap;
import java.util.Map;

public class CP {

    public CP() { }

    public void optimize(DominatorTreeNode root) {
        optimizeRecursion(root, new HashMap<>(), new HashMap<>());
    }

    private void optimizeRecursion(DominatorTreeNode root, Map<Result, Integer> ResultTOInstruction, Map<Result, Integer> ResultTOConstant) {
        if (root == null) {
            return;
        }
        if (root.block.getPhiFunctions() != null) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getPhiFunctions().entrySet()) {
                Result x = new Result();
                x.buildResult(Result.ResultType.variable, entry.getKey());
                x.setSSAVersion(entry.getValue().getInstructionPC());
                ResultTOInstruction.put(x, entry.getValue().getInstructionPC());
            }
        }
        for (Instruction ins : root.block.getInstructions()) {
            if (ins.isReadInstruction()) {
                int pc = ins.getInstructionPC();
                Instruction next = root.block.getNextInstruction(ins);
                if (next.getOp() == InstructionType.MOVE) {
                    Result y = new Result();
                    ins.setLeftResult(y);
                    y.type = next.getRightResult().type;
                    y.varID = next.getRightResult().varID;
                    y.ssaVersion = new SSAValue(next.getRightResult().ssaVersion.getVersion() - 1);
                    y = next.getRightResult();
                    ResultTOInstruction.put(y, pc);
                    next.deleted = true;
                }
            }
            else if (ins.isMoveConstant()) {
                int constant = ins.getLeftResult().value;
                ResultTOConstant.put(ins.getRightResult(), constant);
                ins.deleted = true;
            }
            else if (ins.isMoveInstruction()) {
                int pc = ins.getLeftResult().instrRef;
                ResultTOInstruction.put(ins.getRightResult(), pc);
                ins.deleted = true;
            }
            else if (ins.isMoveVar()) {
                Result left = ins.getLeftResult();
                Result right = ins.getRightResult();
                if (ResultTOConstant.containsKey(left)) {
                    int constant = ResultTOConstant.get(left);
                    ResultTOConstant.put(right, constant);
                    ins.deleted = true;
                }
                else {
                    int pc = ResultTOInstruction.get(left);
                    ResultTOInstruction.put(right, pc);
                    ins.deleted = true;
                    ins.referenceInstrId = pc;
                }
            }
            else {
                Result left = ins.getLeftResult();
                Result right = ins.getRightResult();
                if (left != null && left.type == Result.ResultType.variable) {
                    if (ResultTOConstant.containsKey(left)) {
                        int constant = ResultTOConstant.get(left);
                        left.type = Result.ResultType.constant;
                        left.value = constant;
                    }
                    else {
                        for (Map.Entry<Result, Integer> entry : ResultTOInstruction.entrySet()) {
                            if (entry.getKey().varID == left.varID && entry.getKey().ssaVersion.getVersion() == left.ssaVersion.getVersion()) {
                                int pc = entry.getValue();
                                left.type = Result.ResultType.instruction;
                                left.instrRef = pc;
                            }
                        }
                    }
                }
                else if (left != null && left.type == Result.ResultType.instruction) {
                    for (Map.Entry<Result, Integer> entry : ResultTOInstruction.entrySet()) {
                        if (entry.getKey().ssaVersion.getVersion()==left.instrRef) {
                            int pc = entry.getValue();
                            left.type = Result.ResultType.instruction;
                            left.instrRef = pc;
                        }
                    }
                }
                if (right != null && right.type == Result.ResultType.variable) {
                    if (ResultTOConstant.containsKey(right)) {
                        int constant = ResultTOConstant.get(right);
                        right.type = Result.ResultType.constant;
                        right.value = constant;
                    }
                    else if (ResultTOInstruction.containsKey(right)) {
                        int pc = ResultTOInstruction.get(right);
                        right.type = Result.ResultType.instruction;
                        right.instrRef = pc;
                    }
                }
                else if (right != null && right.type == Result.ResultType.instruction) {
                    for (Map.Entry<Result, Integer> entry : ResultTOInstruction.entrySet()) {
                        if (entry.getKey().ssaVersion.getVersion() == right.instrRef) {
                            int pc = entry.getValue();
                            right.type = Result.ResultType.instruction;
                            right.instrRef = pc;
                        }
                    }
                }
            }
        }
        if (root.block.getType() == BlockType.IF && root.block.getPreBlock().getElseBlock() != null) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getJoinBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                Instruction oldIns = entry.getValue();
                Result left = new Result(entry.getKey(), oldIns,true);
                int instrId;
                if (ResultTOConstant.containsKey(left)) {
                    int constant = ResultTOConstant.get(left);
                    left.type = Result.ResultType.constant;
                    left.value = constant;
                    oldIns.setLeftResult(left);
                }
                else if (ResultTOInstruction.containsKey(left)) {
                    instrId = ResultTOInstruction.get(left);
                    left.type = Result.ResultType.instruction;
                    left.instrRef = instrId;
                    oldIns.leftRepresentedByInstrId = true;
                    oldIns.setLeftResult(left);
                }
            }
        }
        else if (root.block.getType() == BlockType.ELSE && !(root.block.getPreBlock().getType() == BlockType.WHILE_JOIN)) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getJoinBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                Instruction oldIns = entry.getValue();
                Result right = new Result(entry.getKey(), oldIns,false);
                int instrId;
                if (ResultTOConstant.containsKey(right)) {
                    int constant = ResultTOConstant.get(right);
                    right.type = Result.ResultType.constant;
                    right.value = constant;
                    oldIns.setRightResult(right);
                }
                else if (ResultTOInstruction.containsKey(right)) {
                    instrId = ResultTOInstruction.get(right);
                    right.type = Result.ResultType.instruction;
                    right.instrRef = instrId;
                    oldIns.rightRepresentedByInstrId = true;
                    oldIns.setRightResult(right);
                }
            }
        }
        else if (root.block.getBackBlock()!= null) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getBackBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                Instruction oldIns = entry.getValue();
                Result right = new Result(entry.getKey(), oldIns,false);
                int instrId;
                if (ResultTOConstant.containsKey(right)) {
                    int constant = ResultTOConstant.get(right);
                    right.type = Result.ResultType.constant;
                    right.value = constant;
                    oldIns.setRightResult(right);
                }
                else if (ResultTOInstruction.containsKey(right)) {
                    instrId = ResultTOInstruction.get(right);
                    right.type = Result.ResultType.instruction;
                    right.instrRef = instrId;
                    oldIns.rightRepresentedByInstrId = true;
                    oldIns.setRightResult(right);
                }
            }
        }
        else if (root.block.getFollowBlock() != null && root.block.getFollowBlock().getType() == BlockType.WHILE_JOIN) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getFollowBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                Instruction oldIns = entry.getValue();
                Result left = new Result(entry.getKey(), oldIns,true);
                int instrId;
                if (ResultTOConstant.containsKey(left)) {
                    int constant = ResultTOConstant.get(left);
                    left.type = Result.ResultType.constant;
                    left.value = constant;
                    oldIns.setLeftResult(left);
                }
                else if (ResultTOInstruction.containsKey(left)) {
                    instrId = ResultTOInstruction.get(left);
                    left.type = Result.ResultType.instruction;
                    left.instrRef = instrId;
                    oldIns.leftRepresentedByInstrId = true;
                    oldIns.setLeftResult(left);
                }
            }
        }
        else if (root.block.getJoinBlock() != null && root.block.getJoinBlock().getType() == BlockType.IF_JOIN && root.block.getElseBlock() == null) {
            for (Map.Entry<Integer, Instruction> entry : root.block.getJoinBlock().getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                Instruction oldIns = entry.getValue();
                Result left = new Result(entry.getKey(), oldIns,true);
                int instrId;
                if (ResultTOConstant.containsKey(left)) {
                    int constant = ResultTOConstant.get(left);
                    left.type = Result.ResultType.constant;
                    left.value = constant;
                    oldIns.setLeftResult(left);
                }
                else if (ResultTOInstruction.containsKey(left)) {
                    instrId = ResultTOInstruction.get(left);
                    left.type = Result.ResultType.instruction;
                    left.instrRef = instrId;
                    oldIns.leftRepresentedByInstrId = true;
                    oldIns.setLeftResult(left);
                }
                Result right = new Result(entry.getKey(), oldIns,false);
                if (ResultTOConstant.containsKey(right)) {
                    int constant = ResultTOConstant.get(right);
                    right.type = Result.ResultType.constant;
                    right.value = constant;
                    oldIns.setRightResult(right);
                }
                else if (ResultTOInstruction.containsKey(right)) {
                    instrId = ResultTOInstruction.get(right);
                    right.type = Result.ResultType.instruction;
                    right.instrRef = instrId;
                    oldIns.rightRepresentedByInstrId = true;
                    oldIns.setRightResult(right);
                }
            }
        }
        for (DominatorTreeNode child : root.children) {
            optimizeRecursion(child, new HashMap<>(ResultTOInstruction), new HashMap<>(ResultTOConstant));
        }
    }

}
