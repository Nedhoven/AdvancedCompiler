
package main.backend;

import main.frontend.Token;

import main.optimizer.InstructionType;

import java.util.Map;
import java.util.HashMap;

public class CodeTable {

    public final int InputNum = 0;
    public final int OutputNum = 1;
    public final int OutputNewLine = 2;
    public Map<Token, InstructionType> arithmeticCode;
    public Map<Token, InstructionType> branchCode;
    public Map<Integer, InstructionType> predefinedFunctionCode;

    public CodeTable() {
        arithmeticCode = new HashMap<>();
        branchCode = new HashMap<>();
        predefinedFunctionCode = new HashMap<>();
        arithmeticCode.put(Token.PLUS, InstructionType.ADD);
        arithmeticCode.put(Token.MINUS, InstructionType.SUB);
        arithmeticCode.put(Token.TIMES, InstructionType.MUL);
        arithmeticCode.put(Token.DIVIDE, InstructionType.DIV);
        branchCode.put(Token.EQL, InstructionType.BNE);
        branchCode.put(Token.NEQ, InstructionType.BEQ);
        branchCode.put(Token.LSS, InstructionType.BGE);
        branchCode.put(Token.LEQ, InstructionType.BGT);
        branchCode.put(Token.GRE, InstructionType.BLE);
        branchCode.put(Token.GEQ, InstructionType.BLT);
        predefinedFunctionCode.put(InputNum, InstructionType.READ);
        predefinedFunctionCode.put(OutputNum, InstructionType.WRITE);
        predefinedFunctionCode.put(OutputNewLine, InstructionType.WLN);
    }

}
