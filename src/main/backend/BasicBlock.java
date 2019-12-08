
package main.backend;

import main.graph.ControlFlowGraph;
import main.graph.PhiFunctionGenerator;
import main.graph.PhiFunctionUpdateType;
import main.graph.VariableTable;

import main.optimizer.Instruction;
import main.optimizer.InstructionType;
import main.optimizer.Result;
import main.optimizer.SSAValue;

import java.util.*;

public class BasicBlock {

    private BlockType type;
    private int id;
    private List<Instruction> instructions;
    private BasicBlock followBlock;
    private BasicBlock elseBlock;
    private BasicBlock joinBlock;
    private BasicBlock preBlock;
    private BasicBlock backBlock;
    private PhiFunctionGenerator phiGenerator;

    public BasicBlock(BlockType temp) {
        type = temp;
        ControlFlowGraph.getBlocks().add(this);
        id = ControlFlowGraph.getBlocks().size();
        phiGenerator = new PhiFunctionGenerator();
        instructions = new ArrayList<>();
        followBlock = null;
        elseBlock = null;
        joinBlock = null;
        preBlock = null;
        backBlock = null;
    }

    public int getId() {
        return id;
    }

    public BlockType getType() {
        return type;
    }

    public BasicBlock getFollowBlock() {
        return followBlock;
    }

    public BasicBlock getJoinBlock() {
        return joinBlock;
    }

    public BasicBlock getElseBlock() {
        return elseBlock;
    }

    public BasicBlock getPreBlock() {
        return preBlock;
    }

    public BasicBlock getBackBlock() {
        return backBlock;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Map<Integer, Instruction> getPhiFunctions() {
        return phiGenerator.getPhiInstructionMap();
    }

    public Map<Integer, Instruction> getPhiFunctionsFromStartBlock(BasicBlock startBlock) {
        Map<Integer, Instruction> map = new HashMap<>();
        BasicBlock curr = this;
        while (curr != null && curr != startBlock) {
            map.putAll(curr.getPhiFunctions());
            curr = curr.getPreBlock();
        }
        return map;
    }

    public Set<Integer> getPhiVars() {
        return new HashSet<>(getPhiFunctions().keySet());
    }

    public Set<Integer> getPhiVars(BasicBlock startBlock) {
        Set<Integer> vars = new HashSet<>();
        BasicBlock curr = this;
        while (curr != null && curr!= startBlock) {
            vars.addAll(curr.getPhiFunctions().keySet());
            curr = curr.getPreBlock();
        }
        return vars;
    }

    public void setType(BlockType bt) {
        type = bt;
    }

    public void setID(int temp) {
        id = temp;
    }

    public void setInstructions(List<Instruction> ins) {
        instructions = ins;
    }

    public void setFollowBlock(BasicBlock follow) {
        followBlock = follow;
    }

    public void setJoinBlock(BasicBlock join) {
        joinBlock = join;
    }

    public void setElseBlock(BasicBlock elseBlock1) {
        elseBlock = elseBlock1;
    }

    public void setPreBlock(BasicBlock pre) {
        preBlock = pre;
    }

    public void setBackBlock(BasicBlock back) {
        backBlock = back;
    }

    public void updateJoinBlock(int address, SSAValue ssa) {
        if (type != BlockType.WHILE_JOIN) {
            Error("ONLY UPDATE VALUES IN WHILE_JOIN LOOP!");
        }
        else {
            Instruction cond = findConditionInstruction(address);
            if (cond != null) {
                if (cond.getLeftResult().varID == address) {
                    cond.getLeftResult().setSSAVersion(ssa.getVersion());
                }
                else {
                    cond.getRightResult().setSSAVersion(ssa.getVersion());
                }
            }
        }
    }

    public void updateValueReference(BasicBlock block, int address, SSAValue oldSSA, SSAValue newSSA) {
        for (Instruction ins : block.getInstructions()) {
            Result left = ins.getLeftResult();
            Result right = ins.getRightResult();
            if (left != null && left.type == Result.ResultType.variable && left.varID == address && left.ssaVersion.getVersion() == oldSSA.getVersion()) {
                ins.getLeftResult().setSSAVersion(oldSSA.getVersion());
            }
            if (right != null && right.type == Result.ResultType.variable && right.varID == address && right.ssaVersion.getVersion() == oldSSA.getVersion()) {
                ins.getRightResult().setSSAVersion(oldSSA.getVersion());
            }
        }
    }

    public void updateVarReferenceToPhi(int id, int oldSSA, int newSSA, BasicBlock startBlock, BasicBlock endBlock) {
        updateVarReferenceInLoopBody(startBlock, endBlock, id, oldSSA, newSSA);
    }

    public void updateVarReferenceInLoopBody(BasicBlock startBlock, BasicBlock endBlock, int id, int oldSSA, int newSSA) {
        Set<BasicBlock> visited = new HashSet<>();
        Queue<BasicBlock> q = new LinkedList<>();
        q.add(startBlock);
        while (!q.isEmpty()) {
            BasicBlock curr = q.poll();
            visited.add(curr);
            updateVarReferenceInBasicBlock(curr, id, oldSSA, newSSA);
            if (curr == endBlock) {
                continue;
            }
            Queue<BasicBlock> newSuccessors = curr.getSuccessors();
            for (BasicBlock successor : newSuccessors) {
                if (!visited.contains(successor)) {
                    q.add(successor);
                }
            }
        }
    }

    public void updateVarReferenceInBasicBlock(BasicBlock block, int id, int oldSSA, int newSSA) {
        for (Instruction i : block.getInstructions()) {
            if (i.getLeftResult() != null && i.getLeftResult().isIdentifier(id, oldSSA)) {
                i.getLeftResult().setSSAVersion(newSSA);
            }
            if (i.getRightResult() != null && i.getRightResult().isIdentifier(id, oldSSA)) {
                i.getRightResult().setSSAVersion(newSSA);
            }
        }
    }

    public Queue<BasicBlock> getSuccessors() {
        Queue<BasicBlock> successors = new LinkedList<>();
        if (followBlock != null) {
            successors.add(followBlock);
        }
        if (elseBlock != null && !successors.contains(elseBlock)) {
            successors.add(elseBlock);
        }
        if (joinBlock != null && !successors.contains(joinBlock)) {
            successors.add(joinBlock);
        }
        return successors;
    }

    public void updateVarReferenceInJoinBlock(int id, int oldSSA, int newSSA) {
        Instruction cond = findConditionInstruction(id);
        if (cond != null) {
            if (cond.getLeftResult().varID == id) {
                cond.getLeftResult().setSSAVersion(newSSA);
            }
            else {
                cond.getRightResult().setSSAVersion(newSSA);
            }
        }
    }

    public BasicBlock createIfBlock() {
        BasicBlock ifBlock = new BasicBlock(BlockType.IF);
        followBlock = ifBlock;
        preBlock = this;
        return ifBlock;
    }

    public BasicBlock createElseBlock() {
        elseBlock = new BasicBlock(BlockType.ELSE);
        elseBlock.preBlock = this;
        return elseBlock;
    }

    public BasicBlock createDoBlock(){
        BasicBlock doBlock = new BasicBlock(BlockType.DO);
        followBlock = doBlock;
        doBlock.preBlock = this;
        return doBlock;
    }

    public Instruction createPhiFunction(int var) {
        return phiGenerator.addPhiFunction(var, VariableTable.getLatestVersion(var));
    }

    public void updatePhiFunction(int var, SSAValue ssa, BlockType type) {
        PhiFunctionUpdateType.setBlockPhiMap();
        phiGenerator.updatePhiFunction(var, ssa, PhiFunctionUpdateType.getBlockPhiMap().get(type));
    }

    public Instruction findConditionInstruction(int address) {
        for (Instruction ins : instructions) {
            if (ins.getOp() == InstructionType.CMP && ins.getLeftResult().varID == address || ins.getRightResult().varID == address) {
                return ins;
            }
        }
        Error("UNABLE TO FIND CONDITION INSTRUCTION WITH OPERAND ADDRESS " + address + "!");
        return null;
    }

    public Instruction findInstruction(int pc) {
        for (Instruction ins : instructions) {
            if (ins.getInstructionPC() == pc) {
                return ins;
            }
        }
        return null;
    }

    public SSAValue findLastSSA(int id, BasicBlock startBlock) {
        BasicBlock curr = this;
        while (curr != null && curr != startBlock) {
            for (Map.Entry<Integer, Instruction> entry : curr.getPhiFunctions().entrySet()) {
                if (entry.getKey() == id) {
                    return new SSAValue(entry.getValue().getInstructionPC());
                }
            }
            curr = curr.getPreBlock();
        }
        return null;
    }

    public void assignNewSSA(int id, SSAValue ssa, SSAValue newSSA, BasicBlock startBlock) {
        BasicBlock curr = this;
        while (curr != null) {
            for (Map.Entry<Integer, Instruction> entry : curr.getPhiFunctions().entrySet()) {
                if (entry.getKey() == id && entry.getValue().getLeftSSA() == ssa) {
                    entry.getValue().setLeftSSA(newSSA);
                }
            }
            Set<Instruction> in = startBlock.getAllDoInstructions();
            for (Instruction instr : in) {
                if (instr.getLeftResult() != null && instr.getLeftResult().varID == id && instr.getLeftResult().ssaVersion == ssa) {
                    instr.getLeftResult().ssaVersion = newSSA;
                }
                if (instr.getRightResult() != null && instr.getRightResult().varID == id && instr.getRightResult().ssaVersion == ssa) {
                    instr.getRightResult().ssaVersion = newSSA;
                }
            }
            if (curr == startBlock) {
                break;
            }
            curr = curr.getPreBlock();
        }
    }

    public Set<Instruction> getAllDoInstructionsUtil(BasicBlock block){
        Set<Instruction> in = new HashSet<>();
        if (block == null) {
            return in;
        }
        in.addAll(block.getInstructions());
        if (block.followBlock != null) {
            in.addAll(getAllDoInstructionsUtil(block.followBlock));
        }
        if (block.elseBlock != null) {
            in.addAll(getAllDoInstructionsUtil(block.elseBlock));
        }
        if (block.joinBlock != null) {
            in.addAll(getAllDoInstructionsUtil(block.joinBlock));
        }
        return in;
    }

    public Set<Instruction> getAllDoInstructions() {
        if (this.type != BlockType.WHILE_JOIN) {
            return null;
        }
        return getAllDoInstructionsUtil(this);
    }

    public Instruction generateInstruction(InstructionType type, Result r1, Result r2) {
        Instruction newIns = new Instruction(type, r1 == null ? null : r1.deepClone(r1), r2 == null ? null : r2.deepClone(r2));
        instructions.add(newIns);
        return newIns;
    }

    public PhiFunctionGenerator getPhiFunctionGenerator() {
        return phiGenerator;
    }

    public Instruction getNextInstruction(Instruction ins) {
        for (int i = 0; i < instructions.size() - 1; i++) {
            if (instructions.get(i) == ins) {
                return instructions.get(i + 1);
            }
        }
        return null;
    }

    private void Error(String msg) {
        System.err.println("BASIC BLOCK ERROR: " + msg);
    }

}
