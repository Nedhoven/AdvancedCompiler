
package main.optimizer;

import main.frontend.Lexer;
import main.frontend.Token;

import main.backend.BasicBlock;

import java.util.List;
import java.util.ArrayList;

public class Result implements Comparable<Result> {

    public enum ResultType {
        constant, variable, register, condition, branch, instruction
    }

    public ResultType type;
    public int value;
    public int varID;
    public SSAValue ssaVersion;
    public int regNum;
    public int fixUpLocation;
    public Token relOp;
    public BasicBlock branchBlock;
    public int instrRef;
    public boolean isMove = true;
    public boolean isArray = false;
    public boolean isArrayDesignator = false;
    public static int arrayAddressCounter = 0;
    public int arrayAddress;
    public List<Result> arrayDimension = new ArrayList<>();
    public List<Result> designatorDimension = new ArrayList<>();

    public Result() {
        type = null;
        value = -1;
        varID = -1;
        ssaVersion = null;
        regNum = 0;
        fixUpLocation = -1;
        relOp = null;
    }

    public Result(Result result) {
        type = result.type;
        value = result.value;
        varID = result.varID;
        ssaVersion = result.ssaVersion;
        regNum = result.regNum;
        fixUpLocation = result.fixUpLocation;
        relOp = result.relOp;
        branchBlock = result.branchBlock;
        instrRef = result.instrRef;
        isMove = result.isMove;
    }

    public Result(int var, Instruction ins, boolean isLeft) {
        type = ResultType.variable;
        varID = var;
        if (isLeft) {
            ssaVersion = ins.getLeftSSA();
        }
        else {
            ssaVersion = ins.getRightSSA();
        }
    }

    public void buildResult(ResultType rType, int inputValue){
        switch (type) {
            case constant:
                type = rType;
                value = inputValue;
                break;
            case variable:
                type = rType;
                varID = inputValue;
                break;
            case register:
                type = rType;
                regNum = inputValue;
                break;
            case instruction:
                type = rType;
                instrRef = inputValue;
                break;
            default:
                break;
        }
    }

    public void setSSAVersion(int inputSSAVersion){
        ssaVersion = new SSAValue(inputSSAVersion);
    }

    public static Result buildBranch(BasicBlock branchBlock) {
        Result result = new Result();
        result.type = ResultType.branch;
        result.branchBlock = branchBlock;
        return result;
    }

    public static Result buildConstant(int value){
        Result result = new Result();
        result.type = ResultType.constant;
        result.value = value;
        return result;
    }

    public Result deepClone(Result r) {
        return new Result(r);
    }

    @Override
    public int compareTo(Result other) {
        if (this.type != ResultType.variable || other.type != ResultType.variable) {
            try {
                throw new Exception("ONLY CAN COMPARE VARIABLE RESULTS!");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.varID == other.varID && this.ssaVersion == other.ssaVersion) {
            return 0;
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return varID * 17 + ssaVersion.hashCode() * 31;
    }

    @Override
    public boolean equals(Object other){
        if (other.getClass() != this.getClass()) {
            return false;
        }
        Result other2 = (Result) other;
        if (type != ResultType.variable || other2.type != ResultType.variable) {
            try {
                throw new Exception("RESULT ERROR: ONLY CAN COMPARE VARIABLE RESULTS!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return varID == other2.varID && ssaVersion == other2.ssaVersion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (type == null) {
            return "";
        }
        switch (type) {
            case constant:
                sb.append(value);
                break;
            case variable:
                sb.append(Lexer.ids.get(varID)).append("_").append(ssaVersion.getVersion());
                break;
            case register:
                sb.append("r").append(regNum);
                break;
            case condition:
                sb.append(fixUpLocation);
                break;
            case branch:
                sb.append(branchBlock != null ? "[" + branchBlock.getId() + "]": "-1");
                break;
            case instruction:
                sb.append("(").append(instrRef).append(")");
                break;
            default:
                return sb.toString();
        }
        return sb.toString();
    }

    public void setArrayDimension(List<Result> r) {
        arrayDimension = r;
    }

    public static void updateArrayAddressCounter(int length) {
        arrayAddressCounter  = arrayAddressCounter + length;
    }

    public void setArrayAddress(int address) {
        arrayAddress = address;
    }

    public boolean isIdentifier(int id, int oldSSA) {
        return type == ResultType.variable && varID == id && ssaVersion.getVersion() == oldSSA;
    }

}
