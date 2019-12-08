
package main.backend;

import main.optimizer.Result;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class FunctionDeclare {

    private int funcID;
    private BasicBlock firstFuncBlock;
    private List<Integer> localVariables;
    private List<Integer> globalVariables;
    public Map<Integer, Result> localArray;
    private List<Result> parameters;
    private Result returnInstr;

    public FunctionDeclare(int id) {
        funcID = id;
        firstFuncBlock = new BasicBlock(BlockType.NORMAL);
        localVariables = new ArrayList<>();
        globalVariables = new ArrayList<>();
        parameters = new ArrayList<>();
        returnInstr = new Result();
        localArray = new HashMap<>();
    }

    public void addLocalVariable(int localVariable) {
        getLocalVariables().add(localVariable);
    }

    public void addGlobalVariable(int globalVariable) {
        getGlobalVariables().add(globalVariable);
    }

    public int getFuncID() {
        return funcID;
    }

    public void setFuncID(int id) {
        funcID = id;
    }

    public BasicBlock getFirstFuncBlock() {
        return firstFuncBlock;
    }

    public void setFirstFuncBlock(BasicBlock bb) {
        firstFuncBlock = bb;
    }

    public Integer getLocalVariable(int index) {
        if (index < localVariables.size()) {
            return localVariables.get(index);
        }
        Error("UNABLE TO FIND LOCAL VARIABLE (INDEX OUT OF BOUND)!");
        return null;
    }

    public List<Integer> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<Integer> list) {
        localVariables = list;
    }

    public Integer getGlobalVariable(int index) {
        if (index < globalVariables.size()) {
            return globalVariables.get(index);
        }
        Error("UNABLE TO FIND GLOBAL VARIABLE (INDEX OUT OF BOUND)!");
        return null;
    }

    public List<Integer> getGlobalVariables() {
        return globalVariables;
    }

    public void setGlobalVariables(List<Integer> list) {
        globalVariables = list;
    }

    public List<Result> getParameters() {
        return parameters;
    }

    public void setParameters(List<Result> list) {
        parameters = list;
    }

    public Result getReturnInstr() {
        return returnInstr;
    }

    public void setReturnInstr(Result r) {
        returnInstr = r;
    }

    private void Error(String msg) {
        System.err.println("FUNCTION DECLARE ERROR: " + msg);
    }

}
