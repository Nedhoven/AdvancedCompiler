
package main.optimizer;

import main.frontend.Lexer;

import main.graph.ControlFlowGraph;

import java.util.List;
import java.util.ArrayList;

public class Instruction {

    public enum State {
        REPLACE, PHI, NORMAL
    }

    private List<InstructionType> arithmeticOp;
    private List<InstructionType> branchOp;
    private static int pc = 0;
    private int instructionPC;
    private InstructionType op;
    private State state;
    private Result result1;
    private Result result2;
    private SSAValue s1;
    private SSAValue s2;
    private String variableName;
    private boolean leftLatestUpdated;
    public boolean deleted = false;
    public boolean leftRepresentedByInstrId = false;
    public boolean rightRepresentedByInstrId = false;
    public int referenceInstrId;

    private void initializedOpList() {
        arithmeticOp = new ArrayList<>();
        arithmeticOp.add(InstructionType.ADD);
        arithmeticOp.add(InstructionType.DIV);
        arithmeticOp.add(InstructionType.MUL);
        arithmeticOp.add(InstructionType.SUB);
        arithmeticOp.add(InstructionType.CMP);
        branchOp = new ArrayList<>();
        branchOp.add(InstructionType.BNE);
        branchOp.add(InstructionType.BEQ);
        branchOp.add(InstructionType.BLT);
        branchOp.add(InstructionType.BLE);
        branchOp.add(InstructionType.BLT);
        branchOp.add(InstructionType.BLE);
    }

    public Instruction(InstructionType it, Result r1, Result r2) {
        initializedOpList();
        op = it;
        result1 = r1;
        result2 = r2;
        instructionPC = pc;
        pc++;
        state = State.NORMAL;
        ControlFlowGraph.allInstructions.add(this);
    }

    public Instruction(InstructionType it, int var, SSAValue temp1, SSAValue temp2) {
        initializedOpList();
        op = it;
        variableName= Lexer.ids.get(var);
        s1 = temp1;
        s2 = temp2;
        instructionPC = pc;
        pc++;
        state = State.PHI;
        ControlFlowGraph.allInstructions.add(this);
    }

    public String getVariableName() {
        return variableName;
    }

    public InstructionType getOp() {
        return op;
    }

    public static int getPc() {
        return pc;
    }

    public int getInstructionPC() {
        return instructionPC;
    }

    public int getReferenceInstrId() {
        return referenceInstrId;
    }

    public int getLeftAddress() {
        return result1.varID;
    }

    public int getRightAddress() {
        return result2.varID;
    }

    public Result getLeftResult() {
        return result1;
    }

    public Result getRightResult() {
        return result2;
    }

    public SSAValue getLeftSSA() {
        return s1;
    }

    public SSAValue getRightSSA() {
        return s2;
    }

    public void setVariableName(String vn) {
        variableName = vn;
    }

    public void setOp(InstructionType it) {
        op = it;
    }

    public void setPc(int newPC) {
        pc = newPC;
    }

    public void setInstructionPC(int inPC) {
        instructionPC = inPC;
    }

    public void setReferenceInstrId(int id) {
        referenceInstrId = id;
    }

    public void setLeftResult(Result left) {
        result1 = left;
    }

    public void setRightResult(Result right) {
        result2 = right;
    }

    public void setLeftLatestUpdated(boolean bool) {
        leftLatestUpdated = bool;
    }

    public void setRightSSA(SSAValue v2){
        s2 = s2;
    }

    public void setLeftSSA(SSAValue v1) {
        s1 = v1;
    }

    public void setState(Instruction.State s) {
        state = s;
    }

    public boolean isLeftLatestUpdated() {
        return leftLatestUpdated;
    }

    public boolean isReadInstruction() {
        return op == InstructionType.READ;
    }

    public boolean isWriteInstruction() {
        return op == InstructionType.WRITE || op == InstructionType.WLN;
    }

    public boolean isMoveConstant() {
        return op == InstructionType.MOVE && result1.type == Result.ResultType.constant && result2.type == Result.ResultType.variable;
    }

    public boolean isMoveInstruction() {
        return op == InstructionType.MOVE && result1.type == Result.ResultType.instruction && result2.type == Result.ResultType.variable;
    }

    public boolean isMoveVar() {
        return op == InstructionType.MOVE && result1.type == Result.ResultType.variable && result2.type == Result.ResultType.variable;
    }

    public boolean isArithmeticOrBranch() {
        if (result1 == null || result2 == null) {
            return false;
        }
        if (arithmeticOp.contains(op)) {
            return true;
        }
        return branchOp.contains(op) && result1.type == Result.ResultType.variable && result2.type == Result.ResultType.variable;
    }

    public boolean isExpressionOp() {
        boolean f1 = op == InstructionType.NEG;
        boolean f2 = op == InstructionType.ADD;
        boolean f3 = op == InstructionType.SUB;
        boolean f4 = op == InstructionType.MUL;
        boolean f5 = op == InstructionType.DIV;
        boolean f6 = op == InstructionType.ADDA;
        boolean f7 = op == InstructionType.LOADADD;
        boolean f8 = op == InstructionType.STOREADD;
        return f1 || f2 || f3 || f4 || f5 || f6 || f7 || f8;
    }

    public boolean isLoadInstruction() {
        return op == InstructionType.LOAD;
    }

    public String toString() {
        String res =  instructionPC + ": ";
        if (state == State.REPLACE) {
            res += " (" + referenceInstrId + ")";
            return res;
        }
        res += InstructionType.getInstructionName(op) + " ";
        if (state == State.NORMAL) {
            if (branchOp.contains(op)) {
                res += result2.toString();
            }
            else {
                res += result1 != null ? result1.toString() + " " : "";
                res += result2 != null ? result2.toString() : "";
            }
        }
        else {
            String var1 = "";
            String var2 = "";
            if (leftRepresentedByInstrId) {
                var1 = "(" + getLeftResult().instrRef + ")";
            }
            else if (result1 != null && result1.type == Result.ResultType.constant) {
                var1 = Integer.toString(result1.value);
            }
            else if (result1 != null && result1.type == Result.ResultType.instruction) {
                var1 = "(" + result1.instrRef + ")";
            }
            else {
                var1 = variableName + "_" + s1.toString();
            }
            if (rightRepresentedByInstrId) {
                var2 = "(" + getRightResult().instrRef + ")";
            }
            else if (result2 != null && result2.type == Result.ResultType.constant) {
                var2 = Integer.toString(result2.value);
            }
            else if (result2 != null && result2.type == Result.ResultType.instruction) {
                var2 = "(" + result2.instrRef + ")";
            }
            else {
                var2 = variableName + "_" + s2.toString();
            }
            res += variableName + "_" + instructionPC + " " + var1 + " " + var2;
        }
        if (deleted) {
            res += " (deleted)";
        }
        return res;
    }

}
