
package main.backend;

import main.frontend.Token;

import main.graph.ControlFlowGraph;
import main.graph.VariableTable;

import main.optimizer.Instruction;
import main.optimizer.InstructionType;
import main.optimizer.Result;

public class CodeGenerator {

    private CodeTable codeTable;

    public CodeGenerator() {
        codeTable = new CodeTable();
    }

    public void generateArithmeticIC(BasicBlock curr, Token token, Result x, Result y) {
        if (x.type == Result.ResultType.constant && y.type == Result.ResultType.constant) {
            switch (codeTable.arithmeticCode.get(token)) {
                case ADD:
                    x.value = x.value + y.value;
                    break;
                case SUB:
                    x.value = x.value - y.value;
                    break;
                case MUL:
                    x.value = x.value * y.value;
                    break;
                case DIV:
                    x.value = x.value / y.value;
                    break;
            }
        }
        else {
            curr.generateInstruction(codeTable.arithmeticCode.get(token), x, y);
        }
    }

    public void generateCMPIC(BasicBlock curBlock, Result x, Result y) {
        curBlock.generateInstruction(InstructionType.CMP, x, y);
    }

    public Instruction generateIOIC(BasicBlock curBlock, int ioType, Result x){
        return curBlock.generateInstruction(codeTable.predefinedFunctionCode.get(ioType), x, null);
    }

    public void generateVarDeclareIC(BasicBlock curBlock, Result x, FunctionDeclare function) {
        x.setSSAVersion(Instruction.getPc());
        VariableTable.addSSAUseChain(x.varID, x.ssaVersion);
        if (function != null) {
            function.addLocalVariable(x.varID);
        }
        else {
            VariableTable.addGlobalVariable(x.varID);
        }
        Result zeroConstant = Result.buildConstant(0);
        curBlock.generateInstruction(InstructionType.MOVE, zeroConstant, x);
    }

    public void generateReturnIC(BasicBlock curBlock, Result x, FunctionDeclare function) {
        Result curIns = new Result();
        curIns.buildResult(Result.ResultType.instruction, Instruction.getPc());
        curBlock.generateInstruction(InstructionType.MOVE, x, curIns);
        function.setReturnInstr(curIns);
    }

    public void generateASSIGNMENTIC(BasicBlock curBlock, Result x, Result parameter) {
        curBlock.generateInstruction(InstructionType.MOVE, x, parameter);
    }

    public void assignmentIC(BasicBlock curBlock,BasicBlock joinBlock, Result variable, Result value) {
        variable.setSSAVersion(Instruction.getPc());
        VariableTable.addSSAUseChain(variable.varID, variable.ssaVersion);
        curBlock.generateInstruction(InstructionType.MOVE, value, variable);
        if (joinBlock != null) {
            joinBlock.updatePhiFunction(variable.varID, variable.ssaVersion, curBlock.getType());
        }
    }

    public void returnStateIC(BasicBlock curBlock, Result variable, FunctionDeclare function) {
        Result returnInstr = new Result();
        returnInstr.buildResult(Result.ResultType.instruction, Instruction.getPc());
        curBlock.generateInstruction(InstructionType.MOVE, variable, returnInstr);
        function.setReturnInstr(returnInstr);
    }

    public void condNegBraFwd(BasicBlock curBlock, Result relation) {
        relation.fixUpLocation = Instruction.getPc();
        curBlock.generateInstruction(codeTable.branchCode.get(relation.relOp), relation, Result.buildBranch(null));
    }

    public void unCondBraFwd(BasicBlock curBlock, Result follow) {
        Result branch = Result.buildBranch(null);
        branch.fixUpLocation = follow.fixUpLocation;
        curBlock.generateInstruction(InstructionType.BRA, null, branch);
        follow.fixUpLocation = Instruction.getPc() - 1;
    }

    public void fix(int pc, BasicBlock referenceBlock) {
        // TODO: might lead to null pointer
        ControlFlowGraph.getInstruction(pc).getRightResult().branchBlock = referenceBlock;
    }

    public void fixAll(int pc, BasicBlock referenceBlock) {
        while (pc != 0) {
            // TODO: might lead to null pointer
            int next = ControlFlowGraph.getInstruction(pc).getRightResult().fixUpLocation;
            fix(pc, referenceBlock);
            pc = next;
        }
    }

}
