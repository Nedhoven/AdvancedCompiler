
package main.frontend;

import main.backend.CodeGenerator;

import main.graph.ControlFlowGraph;
import main.graph.VariableTable;

import main.backend.BasicBlock;
import main.backend.BlockType;
import main.backend.FunctionDeclare;

import main.optimizer.Instruction;
import main.optimizer.InstructionType;
import main.optimizer.Result;
import main.optimizer.SSAValue;

import java.io.IOException;
import java.util.*;

public class Parser {

    private Lexer lexer;
    private Token curToken;
    private CodeGenerator codeGenerator;
    private Result ref;

    public Parser (String path) throws IOException {
        lexer = new Lexer(path);
        codeGenerator = new CodeGenerator();
        ref = new Result();
        new ControlFlowGraph();
        new VariableTable();
    }

    public void parser() throws Throwable {
        lexer.startScanner();
        Next();
        computation();
        lexer.closeScanner();
    }

    private Result designator(BasicBlock curr, Stack<BasicBlock> joinBlocks, FunctionDeclare function) throws IOException, Error {
        Result designator = new Result();
        List<Result> dimensions = new ArrayList<>();
        if (curToken == Token.IDENTIFIER) {
            designator.buildResult(Result.ResultType.variable, lexer.getVarID());
            Next();
            while (curToken == Token.OPEN_BRACKET) {
                Next();
                dimensions.add(expression(curr, joinBlocks, function));
                if (curToken == Token.CLOSE_BRACKET) {
                    Next();
                }
                else {
                    Error("DESIGNATOR EXPECT ']'!");
                }
            }
        }
        else {
            Error("DESIGNATOR EXPECT IDENTIFIER!");
        }
        if (!dimensions.isEmpty()) {
            designator.isArrayDesignator = true;
            designator.designatorDimension = dimensions;
            Result arr;
            if (function != null && function.localArray.containsKey(designator.varID)) {
                arr = function.localArray.get(designator.varID);
            }
            else {
                arr = VariableTable.ArrayDefinition.get(designator.varID);
            }
            Instruction ins = calculateArray(curr, arr, dimensions);
            ref.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
            curr.generateInstruction(InstructionType.LOADADD, ref, null);
        }
        return designator;
    }

    private Result factor(BasicBlock curr, Stack<BasicBlock> joinBlocks, FunctionDeclare f) throws IOException, Error{
        Result factor = new Result();
        if (curToken == Token.IDENTIFIER) {
            factor = designator(curr, joinBlocks, f);
            if (!factor.isArrayDesignator) {
                factor.ssaVersion = VariableTable.getLatestVersion(factor.varID);
            }
            else {
                Result ref1 = new Result();
                ref1.buildResult(Result.ResultType.instruction, ref.instrRef);
                return ref1;
            }
        }
        else if (curToken == Token.NUMBER) {
            factor.buildResult(Result.ResultType.constant, lexer.getVal());
            Next();
        }
        else if (curToken == Token.OPEN_PARENTHESIS) {
            Next();
            factor = expression(curr, joinBlocks, f);
            if (curToken == Token.CLOSE_PARENTHESIS) {
                Next();
            }
            else {
                Error("FACTOR EXPECT ')'!");
            }
        }
        else if (curToken == Token.CALL) {
            factor = functionCall(curr, joinBlocks);
        }
        else {
            Error("INVALID FACTOR!");
        }
        return factor;
    }

    private Result term(BasicBlock curr, Stack<BasicBlock> joinBlocks, FunctionDeclare f) throws IOException, Error {
        Result x = factor(curr, joinBlocks, f);
        while (curToken == Token.TIMES || curToken == Token.DIVIDE) {
            Token operator = curToken;
            Next();
            x.isMove = false;
            Result y = factor(curr, joinBlocks, f);
            x.instrRef = Instruction.getPc();
            codeGenerator.generateArithmeticIC(curr, operator, x, y);
            ControlFlowGraph.delUseChain.updateDefUseChain(x, y);
        }
        return x;
    }

    private Result expression(BasicBlock curr, Stack<BasicBlock> join, FunctionDeclare f) throws IOException, Error {
        Result x = term(curr, join, f);
        while(curToken == Token.PLUS || curToken == Token.MINUS) {
            x.isMove = false;
            Token operator = curToken;
            Next();
            Result y = term(curr, join, f);
            x.instrRef=Instruction.getPc();
            if (!y.isMove) {
                y.type = Result.ResultType.instruction;
            }
            codeGenerator.generateArithmeticIC(curr, operator, x, y);
            ControlFlowGraph.delUseChain.updateDefUseChain(x, y);
            x.instrRef = Instruction.getPc() - 1;
        }
        if (!x.isMove) {
            x.type = Result.ResultType.instruction;
        }
        return x;
    }

    private Result relation(BasicBlock curr, Stack<BasicBlock> join, FunctionDeclare f) throws IOException, Error{
        Result relation = null;
        Result x = expression(curr, join, f);
        if (curToken == Token.EQL || curToken == Token.NEQ || curToken == Token.LEQ || curToken == Token.LSS || curToken == Token.GEQ || curToken == Token.GRE) {
            Token relOp = curToken;
            Next();
            Result y = expression(curr, join, f);
            codeGenerator.generateCMPIC(curr, x, y);
            relation = new Result();
            relation.relOp = relOp;
            relation.type = Result.ResultType.condition;
            relation.fixUpLocation = 0;
        }
        else {
            Error("RELATION EXPECT VALID RELATION OPERATOR!");
        }
        return relation;
    }

    private void assignment(BasicBlock curr, Stack<BasicBlock> join, FunctionDeclare function) throws IOException, Error{
        if (curToken == Token.LET) {
            Next();
            Result variable = designator(curr, join, function);
            if (join != null && join.size() > 0) {
                if (!variable.isArrayDesignator) {
                    join.peek().createPhiFunction(variable.varID);
                }
            }
            if (curToken == Token.BECOMES) {
                Next();
                Result value = expression(curr, join, function);
                if (!variable.isArrayDesignator) {
                    if (join != null) {
                        codeGenerator.assignmentIC(curr, join.peek(), variable, value);
                    }
                    else {
                        codeGenerator.assignmentIC(curr, null, variable, value);
                    }
                }
                else {
                    curr.generateInstruction(InstructionType.STOREADD, value, ref);
                }
            }
            else {
                Error("EXPECT '<-'!");
            }
        }
        else {
            Error("EXPECT 'let'!");
        }
    }

    private Result functionCall(BasicBlock curr, Stack<BasicBlock> join) throws IOException {
        Result x = new Result();
        FunctionDeclare function = null;
        if (curToken == Token.CALL) {
            Next();
            if (curToken == Token.IDENTIFIER) {
                x.buildResult(Result.ResultType.variable, lexer.getID());
                function = ControlFlowGraph.allFunctions.get(x.varID);
                Next();
                int index = 0;
                if (curToken == Token.OPEN_PARENTHESIS) {
                    Next();
                    Result y = new Result();
                    if (isExpression(curToken)) {
                        y = expression(curr, join, function);
                        if (x.varID >= 3) {
                            codeGenerator.generateASSIGNMENTIC(curr, y, function.getParameters().get(index++));
                        }
                        while (curToken == Token.COMMA) {
                            Next();
                            y = expression(curr, join, function);
                            if (x.varID >= 3) {
                                codeGenerator.generateASSIGNMENTIC(curr, y, function.getParameters().get(index++));
                            }
                        }
                    }
                    if (curToken == Token.CLOSE_PARENTHESIS) {
                        Next();
                    }
                    else {
                        Error("EXPECT ')'!");
                    }
                    if (x.varID == 0) {
                        Instruction ins = codeGenerator.generateIOIC(curr, x.varID, null);
                        Result returnRE = new Result();
                        returnRE.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
                        return returnRE;
                    }
                    else if (x.varID < 3) {
                        codeGenerator.generateIOIC(curr, x.varID, y);
                    }
                    else {
                        Result branch = Result.buildBranch(ControlFlowGraph.allFunctions.get(x.varID).getFirstFuncBlock());
                    }
                }
                else {
                    if (x.varID == 0) {
                        Instruction ins = codeGenerator.generateIOIC(curr, x.varID, null);
                        Result returnRE = new Result();
                        returnRE.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
                        return returnRE;
                    }
                    else if (x.varID < 3) {
                        codeGenerator.generateIOIC(curr, x.varID, null);
                    }
                    else {
                        Result branch = Result.buildBranch(ControlFlowGraph.allFunctions.get(x.varID).getFirstFuncBlock());
                    }
                }
            }
            else {
                Error("EXPECT IDENTIFIER!");
            }
        }
        else {
            Error("EXPECT CALL TOKEN!");
        }
        // TODO: MAY PRODUCE NULL POINTER EXCEPTION
        return x.varID < 3 ? null : function.getReturnInstr();
    }

    private BasicBlock ifStatement(BasicBlock curr, Stack<BasicBlock> join, FunctionDeclare function) throws IOException, Error {
        Map<Integer, List<SSAValue>> ssaUseChain = VariableTable.cloneSSAUseChain();
        if (curToken == Token.IF) {
            Next();
            Result follow = new Result();
            follow.fixUpLocation = 0;
            Result relation = relation(curr,null, function);
            BasicBlock joinBlock = new BasicBlock(BlockType.IF_JOIN);
            curr.setJoinBlock(joinBlock);
            codeGenerator.condNegBraFwd(curr, relation);
            BasicBlock thenEndBlock = null;
            BasicBlock elseEndBlock = null;
            if (curToken == Token.THEN) {
                Next();
                if (join == null) {
                    join = new Stack<>();
                }
                join.push(joinBlock);
                thenEndBlock = stateSequence(curr.createIfBlock(), join,null);
                join.pop();
                if (curToken == Token.ELSE) {
                    VariableTable.setSSAUseChain(ssaUseChain);
                    codeGenerator.unCondBraFwd(thenEndBlock, follow);
                    Next();
                    BasicBlock elseBlock = curr.createElseBlock();
                    codeGenerator.fix(relation.fixUpLocation, elseBlock);
                    join.push(joinBlock);
                    elseEndBlock = stateSequence(elseBlock, join,null);
                    join.pop();
                }
                else {
                    codeGenerator.fix(relation.fixUpLocation, joinBlock);
                }
                if (curToken == Token.FI) {
                    Next();
                    codeGenerator.fixAll(follow.fixUpLocation, joinBlock);
                    thenEndBlock.setJoinBlock(joinBlock);
                    if (elseEndBlock != null) {
                        elseEndBlock.setJoinBlock(joinBlock);
                    }
                    else {
                        return null;
                    }
                    VariableTable.setSSAUseChain(ssaUseChain);
                    updateReferenceForPhiVarInJoinBlock(joinBlock);
                    return joinBlock;
                }
                else {
                    Error("EXPECT 'fi'!");
                }
            }
            else {
                Error("EXPECT 'then'!");
            }
        }
        else {
            Error("EXPECT 'if'!");
        }
        return null;
    }

    private void createPhiInIfJoinBlocks(BasicBlock curr, BasicBlock ifEndBlock, BasicBlock elseEndBlock, BasicBlock joinBlock, Map<Integer, List<SSAValue>> ssaUseChain) throws Error {
        Set<Integer> ifPhiVars = ifEndBlock.getPhiVars(curr);
        Set<Integer> phiVars = new HashSet<>(ifPhiVars);
        Set<Integer> elsePhiVars = new HashSet<>();
        if (elseEndBlock != null) {
            elsePhiVars = elseEndBlock.getPhiVars(curr);
            phiVars.addAll(elsePhiVars);
        }
        Set<Integer> curPhiVars = joinBlock.getPhiVars();
        for (Integer phiVar : phiVars) {
            if (!curPhiVars.contains(phiVar)) {
                joinBlock.createPhiFunction(phiVar);
                if (ifPhiVars.contains(phiVar))
                    joinBlock.updatePhiFunction(phiVar, ifEndBlock.findLastSSA(phiVar, curr), ifEndBlock.getType());
                if (elseEndBlock != null && elsePhiVars.contains(phiVar))
                    joinBlock.updatePhiFunction(phiVar, elseEndBlock.findLastSSA(phiVar, curr), elseEndBlock.getType());
                else {
                    // TODO: may produce null pointer exception
                    joinBlock.updatePhiFunction(phiVar, ssaUseChain.get(phiVar).get(ssaUseChain.get(phiVar).size() - 1), elseEndBlock.getType());
                }
            }
        }
    }

    private void updateReferenceForPhiVarInJoinBlock(BasicBlock join) {
        for (Map.Entry<Integer, Instruction> entry : join.getPhiFunctions().entrySet()) {
            VariableTable.addSSAUseChain(entry.getKey(), entry.getValue().getInstructionPC());
            for (Instruction instr : join.getInstructions()) {
                if (instr.getLeftResult() != null && instr.getLeftResult().varID == entry.getKey()) {
                    instr.getLeftResult().setSSAVersion(entry.getValue().getInstructionPC());
                }
                if (instr.getRightResult() != null && instr.getRightResult().varID == entry.getKey()) {
                    instr.getRightResult().setSSAVersion(entry.getValue().getInstructionPC());
                }
            }
        }
    }

    private void updateValuesInOuterPhiFunc(Map<Integer, Instruction> phi, BasicBlock outJoinBlock, boolean Left) {
        if (Left) {
            for (Map.Entry<Integer, Instruction> entry1 : phi.entrySet()) {
                for (Map.Entry<Integer, Instruction> entry2 : outJoinBlock.getPhiFunctions().entrySet()) {
                    if (entry1.getKey().equals(entry2.getKey())) {
                        entry2.getValue().setLeftSSA(new SSAValue(entry1.getValue().getInstructionPC()));
                    }
                }
            }
        }
        else {
            for (Map.Entry<Integer, Instruction> entry1 : phi.entrySet()) {
                for (Map.Entry<Integer, Instruction> entry2 : outJoinBlock.getPhiFunctions().entrySet()) {
                    if (entry1.getKey().equals(entry2.getKey())) {
                        entry2.getValue().setRightSSA(new SSAValue(entry1.getValue().getInstructionPC()));
                    }
                }
            }
        }
    }

    private BasicBlock whileStatement(BasicBlock curBlock, Stack<BasicBlock> joinBlocks, FunctionDeclare function) throws IOException, Error {
        Map<Integer, List<SSAValue>> ssaUseChain = VariableTable.cloneSSAUseChain();
        if (curToken == Token.WHILE) {
            Next();
            BasicBlock joinBlock = new BasicBlock(BlockType.WHILE_JOIN);
            curBlock.setFollowBlock(joinBlock);
            curBlock = joinBlock;
            Result relation = relation(curBlock, null, function);
            codeGenerator.condNegBraFwd(curBlock, relation);
            BasicBlock doEndBlock = null;
            if (curToken== Token.DO) {
                Next();
                BasicBlock startBlock = curBlock.createDoBlock();
                if (joinBlocks == null) {
                    joinBlocks = new Stack<>();
                }
                joinBlocks.push(joinBlock);
                doEndBlock = stateSequence(startBlock, joinBlocks, null);
                doEndBlock.generateInstruction(InstructionType.BRA, null, Result.buildBranch(curBlock));
                updateReferenceForPhiVarInLoopBody(joinBlock, startBlock, doEndBlock);
                BasicBlock followBlock = curBlock.createElseBlock();
                codeGenerator.fix(relation.fixUpLocation, followBlock);
                doEndBlock.setBackBlock(curBlock);
                joinBlocks.pop();
                for (BasicBlock jB : joinBlocks) {
                    updateValuesInOuterPhiFunc(joinBlock.getPhiFunctions(), jB, false);
                }
                createPhiInWhileJoinBlocks(curBlock, doEndBlock, joinBlock, ssaUseChain);
                VariableTable.setSSAUseChain(ssaUseChain);
                updateReferenceForPhiVarInJoinBlock(joinBlock);
                if (curToken == Token.OD) {
                    Next();
                    return followBlock;
                }
                else {
                    Error("EXPECT 'od'!");
                }
            }
            else {
                Error("EXPECT 'do'!");
            }
        }
        else {
            Error("EXPECT 'while'!");
        }
        return null;
    }

    private void createPhiInWhileJoinBlocks(BasicBlock curBlock, BasicBlock doEndBlock, BasicBlock joinBlock, Map<Integer, List<SSAValue>> ssaUseChain) {
        Set<Integer> phiVars = new HashSet<>(doEndBlock.getPhiVars(curBlock));
        Set<Integer> curPhiVars = joinBlock.getPhiVars();
        for (Integer phiVar : phiVars) {
            if (!curPhiVars.contains(phiVar)) {
                Instruction curInstr = joinBlock.createPhiFunction(phiVar);
                joinBlock.updatePhiFunction(phiVar, doEndBlock.findLastSSA(phiVar, curBlock), doEndBlock.getType());
                joinBlock.updatePhiFunction(phiVar, ssaUseChain.get(phiVar).get(ssaUseChain.get(phiVar).size() - 1), BlockType.IF );
                if (joinBlock.getType() == BlockType.WHILE_JOIN) {
                    doEndBlock.assignNewSSA(phiVar, ssaUseChain.get(phiVar).get(ssaUseChain.get(phiVar).size() - 1), new SSAValue(curInstr.getInstructionPC()), curBlock);
                }
            }
        }
    }

    public void updateReferenceForPhiVarInLoopBody(BasicBlock innerJoinBlock, BasicBlock startBlock, BasicBlock doLastBlock) {
        for (Map.Entry<Integer, Instruction> entry : innerJoinBlock.getPhiFunctions().entrySet()) {
            innerJoinBlock.updateVarReferenceToPhi(entry.getKey(), entry.getValue().getLeftSSA().getVersion(), entry.getValue().getInstructionPC(), startBlock, doLastBlock);
        }
    }

    private Result returnStatement(BasicBlock curBlock, Stack<BasicBlock>joinBlocks, FunctionDeclare function) throws IOException {
        Result x = new Result();
        if (curToken == Token.RETURN) {
            Next();
            if (isExpression(curToken)) {
                x = expression(curBlock, joinBlocks, function);
            }
        }
        else {
            Error("EXPECT RETURN!");
        }
        return x;
    }

    private BasicBlock statement(BasicBlock curBlock, Stack<BasicBlock> joinBlocks, FunctionDeclare function) throws IOException, Error {
        if (curToken == Token.LET) {
            assignment(curBlock, joinBlocks, function);
            return curBlock;
        }
        else if (curToken == Token.CALL) {
            functionCall(curBlock, joinBlocks);
            return curBlock;
        }
        else if (curToken == Token.IF) {
            return ifStatement(curBlock, joinBlocks, function);
        }
        else if (curToken == Token.WHILE) {
            return whileStatement(curBlock, joinBlocks, function);
        }
        else if (curToken == Token.RETURN) {
            Result x = returnStatement(curBlock, joinBlocks, function);
            codeGenerator.generateReturnIC(curBlock, x, function);
            return curBlock;
        }
        else {
            Error("INVALID STATEMENT!");
            return null;
        }
    }

    private BasicBlock stateSequence(BasicBlock curBlock, Stack<BasicBlock> joinBlocks, FunctionDeclare function) throws IOException, Error {
        BasicBlock nextBlock = statement(curBlock, joinBlocks, function);
        while (curToken == Token.SEMICOLON) {
            Next();
            nextBlock = statement(nextBlock, joinBlocks, function);
        }
        return nextBlock;
    }

    private List<Result> typeDeclare() throws IOException {
        if (curToken == Token.VAR) {
            Next();
            return null;
        }
        else if (curToken == Token.ARRAY) {
            List<Result> returnRes = new ArrayList<>();
            Next();
            boolean hasDimension = false;
            while (curToken == Token.OPEN_BRACKET) {
                hasDimension = true;
                Next();
                Result x = new Result();
                if (curToken == Token.NUMBER) {
                    x.buildResult(Result.ResultType.constant, lexer.getVal());
                    returnRes.add(x);
                    Next();
                }
                else {
                    Error("NUMBER OUT OF RANGE OF ARRAY DECLARATION!");
                }
                if (curToken == Token.CLOSE_BRACKET) {
                    Next();
                }
                else {
                    Error("EXPECT ']'!");
                }
            }
            if (!hasDimension) {
                Error("EXPECT '[]'!");
            }
            else {
                return returnRes;
            }
        }
        else {
            Error("EXPECT VAR OR ARRAY!");
        }
        return null;
    }

    private void varDeclare(BasicBlock curBlock, FunctionDeclare function) throws IOException {
        Result x = null;
        List<Result> r = typeDeclare();
        if (curToken == Token.IDENTIFIER) {
            x = new Result();
            x.buildResult(Result.ResultType.variable, lexer.getID());
            if (r != null) {
                x.setArrayDimension(r);
                x.isArray = true;
                x.setArrayAddress(Result.arrayAddressCounter);
                int length = 1;
                for (int i = r.size() - 1; i >= 0; i--) {
                    length = length * r.get(i).value;
                }
                Result.updateArrayAddressCounter(length);
            }
            declareVariable(curBlock, x, function);
            Next();
            while (curToken == Token.COMMA) {
                Next();
                if (curToken == Token.IDENTIFIER) {
                    x = new Result();
                    x.buildResult(Result.ResultType.variable, lexer.getID());
                    if (r != null) {
                        x.setArrayDimension(r);
                        x.isArray = true;
                        x.setArrayAddress(Result.arrayAddressCounter);
                        int length = 1;
                        for (int i = r.size() - 1; i >= 0; i--) {
                            length = length * r.get(i).value;
                        }
                        Result.updateArrayAddressCounter(length);
                    }
                    declareVariable(curBlock, x, function);
                    Next();
                }
                else {
                    Error("EXPECT IDENTIFIER!");
                }
            }
            if (curToken == Token.SEMICOLON) {
                Next();
            }
            else {
                Error("EXPECT ';'!");
            }
        }
        else {
            Error("EXPECT IDENTIFIER!");
        }
    }

    private void declareVariable(BasicBlock curBlock, Result x, FunctionDeclare function) {
        if (x.type != Result.ResultType.variable) {
            this.Error("TYPE SHOULD BE VARIABLE! (LINE: " + lexer.getLineNumber() + ")");
        }
        else if (!x.isArray) {
            int var = x.varID;
            x.setSSAVersion(Instruction.getPc());
            VariableTable.addSSAUseChain(x.varID, x.ssaVersion);
            if (function != null) {
                function.getLocalVariables().add(var);
            }
            else {
                VariableTable.addGlobalVariable(var);
            }
            curBlock.generateInstruction(InstructionType.MOVE, Result.buildConstant(0), x);
        }
        else {
            x.setSSAVersion(Instruction.getPc());
            VariableTable.addSSAUseChain(x.varID, x.ssaVersion);
            if (function != null) {
                function.localArray.put(x.varID, x);
            }
            else {
                VariableTable.ArrayDefinition.put(x.varID, x);
            }
        }
    }

    private void funcDeclare() throws IOException {
        if (curToken == Token.FUNCTION || curToken == Token.PROCEDURE) {
            Next();
            if (curToken == Token.IDENTIFIER) {
                Result x = new Result();
                x.buildResult(Result.ResultType.variable, lexer.getID());
                FunctionDeclare function = declareFunction(x);
                Next();
                if (curToken == Token.OPEN_PARENTHESIS) {
                    formalParam(function);
                }
                if (curToken == Token.SEMICOLON) {
                    Next();
                    funcBody(function);
                    if (curToken == Token.SEMICOLON) {
                        Next();
                    }
                    else {
                        Error("EXPECT ';'");
                    }
                }
                else {
                    Error("EXPECT ';' AFTER IDENTIFIER");
                }
            }
            else {
                Error("EXPECT IDENTIFIER!");
            }
        }
        else {
            Error("EXPECT FUNCTION OR PROCEDURE NAME!");
        }
    }

    private FunctionDeclare declareFunction(Result x) {
        if (x.type != Result.ResultType.variable) {
            Error("TYPE SHOULD BE VARIABLE!");
        }
        else {
            int functionIdentifier = x.varID;
            if (!ControlFlowGraph.allFunctions.containsKey(functionIdentifier)) {
                FunctionDeclare func = new FunctionDeclare(functionIdentifier);
                ControlFlowGraph.allFunctions.put(functionIdentifier, func);
                return func;
            }
            else {
                Error("FUNCTION NAME REDEFINED!");
                return null;
            }
        }
        return null;
    }

    private void formalParam(FunctionDeclare function) throws IOException {
        Result x;
        if (curToken == Token.OPEN_PARENTHESIS) {
            Next();
            if (curToken == Token.IDENTIFIER) {
                x = new Result();
                x.buildResult(Result.ResultType.variable, lexer.getID());
                declareVariable(function.getFirstFuncBlock(), x, function);
                function.getParameters().add(x);
                Next();
                while (curToken == Token.COMMA) {
                    Next();
                    if (curToken == Token.IDENTIFIER) {
                        x = new Result();
                        x.buildResult(Result.ResultType.variable, lexer.getID());
                        declareVariable(function.getFirstFuncBlock(), x, function);
                        function.getParameters().add(x);
                        Next();
                    }
                    else {
                        Error("EXPECT IDENTIFIER!");
                    }
                }
            }
            if (curToken == Token.CLOSE_PARENTHESIS) {
                Next();
            }
            else {
                Error("EXPECT ')'!");
            }
        }
        else {
            Error("EXPECT '('!");
        }
    }

    private void funcBody(FunctionDeclare function) throws IOException {
        while (curToken == Token.VAR || curToken == Token.ARRAY) {
            varDeclare(function.getFirstFuncBlock(), function);
        }
        if (curToken == Token.OPEN_BRACE) {
            Next();
            if (isStatement(curToken)) {
                stateSequence(function.getFirstFuncBlock(), null, function);
            }
            if (curToken == Token.CLOSE_BRACE) {
                Next();
            }
            else {
                Error("EXPECT '}'!");
            }
        }
        else {
            Error("EXPECT '{'!");
        }
    }

    private void computation() throws IOException{
        if (curToken == Token.MAIN) {
            Next();
            while (curToken == Token.VAR || curToken == Token.ARRAY) {
                varDeclare(ControlFlowGraph.getFirstBlock(), null);
            }
            while (curToken == Token.FUNCTION || curToken == Token.PROCEDURE) {
                funcDeclare();
            }
            if (curToken == Token.OPEN_BRACE) {
                Next();
                BasicBlock lastBlock = stateSequence(ControlFlowGraph.getFirstBlock(),null,null);
                if (curToken == Token.CLOSE_BRACE) {
                    Next();
                    if (curToken == Token.PERIOD) {
                        lastBlock.generateInstruction(InstructionType.END, null, null);
                    }
                    else {
                        Error("EXPECTED '.'!");
                    }
                }
                else {
                    Error("EXPECTED '}'!");
                }
            }
            else {
                Error("EXPECTED '{'!");
            }
        }
        else {
            Error("EXPECTED 'MAIN'!");
        }
    }

    private void Next() throws IOException {
        curToken = lexer.getNextToken();
    }

    private void Error(String msg) {
        System.err.println("PARSER ERROR: " + msg);
    }

    private boolean isStatement(Token token) {
        return (token == Token.IF || token == Token.WHILE || token == Token.CALL || token == Token.RETURN || token == Token.LET);
    }

    private boolean isExpression(Token token) {
        return (token == Token.IDENTIFIER || token == Token.NUMBER || token == Token.OPEN_PARENTHESIS || token == Token.CALL);
    }

    private Instruction calculateArray(BasicBlock curBlock, Result array, List<Result> dimension) {
        if (dimension.size() > 1) {
            Instruction prev = null;
            for (int i = 0; i < dimension.size() - 1; i++) {
                Result d = dimension.get(i);
                Instruction ins;
                if (d.type == Result.ResultType.constant) {
                    ins = curBlock.generateInstruction(InstructionType.MUL, Result.buildConstant(d.value), Result.buildConstant(array.arrayDimension.get(i).value));
                }
                else if (d.type == Result.ResultType.variable) {
                    Result ref1 = new Result();
                    ref1.buildResult(Result.ResultType.instruction, d.ssaVersion.getVersion());
                    ins = curBlock.generateInstruction(InstructionType.ADD, ref1, Result.buildConstant(array.arrayDimension.get(i).value));
                }
                else {
                    Result ref = new Result();
                    ref.buildResult(Result.ResultType.instruction, d.instrRef);
                    ins = curBlock.generateInstruction(InstructionType.MUL, ref, Result.buildConstant(array.arrayDimension.get(i).value));
                }
                if (prev != null) {
                    Result ref1 = new Result();
                    ref1.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
                    Result ref2 = new Result();
                    ref2.buildResult(Result.ResultType.instruction, prev.getInstructionPC());
                    prev = curBlock.generateInstruction(InstructionType.ADD, ref1, ref2);
                }
                else {
                    prev = ins;
                }
            }
            Result d = dimension.get(dimension.size() - 1);
            Result ref = new Result();
            ref.buildResult(Result.ResultType.instruction, prev.getInstructionPC());
            Instruction ins;
            if (d.type == Result.ResultType.constant) {
                ins = curBlock.generateInstruction(InstructionType.ADD, Result.buildConstant(d.value), ref);
            }
            else if (d.type == Result.ResultType.variable) {
                Result ref1 = new Result();
                ref1.buildResult(Result.ResultType.instruction, d.ssaVersion.getVersion());
                ins = curBlock.generateInstruction(InstructionType.ADD, ref1, ref);
            }
            else {
                Result ref1 = new Result();
                ref1.buildResult(Result.ResultType.instruction, d.instrRef);
                ins = curBlock.generateInstruction(InstructionType.ADD, ref1, ref);
            }
            ref.buildResult(Result.ResultType.instruction, ins.getInstructionPC());
            return curBlock.generateInstruction(InstructionType.ADDA, ref, Result.buildConstant(array.arrayAddress));
        }
        else {
            Result d = dimension.get(0);
            Instruction adda;
            if (d.type == Result.ResultType.constant) {
                adda = curBlock.generateInstruction(InstructionType.ADDA, Result.buildConstant(d.value), Result.buildConstant(array.arrayAddress));
            }
            else if (d.type == Result.ResultType.variable) {
                Result ref = new Result();
                ref.buildResult(Result.ResultType.instruction, d.ssaVersion.getVersion());
                adda = curBlock.generateInstruction(InstructionType.ADDA, ref, Result.buildConstant(array.arrayAddress));
            }
            else {
                Result ref = new Result();
                ref.buildResult(Result.ResultType.instruction, d.instrRef);
                adda = curBlock.generateInstruction(InstructionType.ADDA, ref, Result.buildConstant(array.arrayAddress));
            }
            return adda;
        }
    }

}
