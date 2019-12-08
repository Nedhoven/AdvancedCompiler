
package main.backend;

import main.optimizer.Result;

public class ExpressionNode {

    private Result result1;
    private Result result2;

    public ExpressionNode(Result r1, Result r2) {
        result1 = r1;
        result2 = r2;
    }

    @Override
    public int hashCode() {
        int hashcode1;
        int hashcode2;
        if (result1.type == Result.ResultType.variable) {
            hashcode1 = result1.varID * 17 + result1.ssaVersion.hashCode() * 31;
        }
        else {
            hashcode1 = result1.value * 61;
        }
        if (result2 != null && result2.type == Result.ResultType.variable) {
            hashcode2 = result2.varID * 41 + result2.ssaVersion.hashCode() * 59;
        }
        else if (result2 != null) {
            hashcode2 = result2.value * 61;
        }
        else {
            hashcode2 = 0;
        }
        return hashcode1 + hashcode2;
    }

    @Override
    public boolean equals(Object object) {
        if (object.getClass() != this.getClass()) {
            return false;
        }
        return isEqualResult(result1, ((ExpressionNode)object).result1) && (isEqualResult(result2, ((ExpressionNode)object).result2));
    }

    @Override
    public String toString(){
        return result1.varID + "_" + result1.ssaVersion.getVersion() + " " + result2.varID + "_" + result2.ssaVersion.getVersion();
    }

    private boolean isEqualResult(Result temp1, Result temp2) {
        if (temp2 == null && temp1 == null) {
            return true;
        }
        else if (temp1 == null || temp2 == null) {
            return false;
        }
        if (temp1.type != temp2.type) {
            return false;
        }
        if (temp1.type == Result.ResultType.variable) {
            return temp1.varID == temp2.varID && temp1.ssaVersion.equals(temp2.ssaVersion);
        }
        else if (temp1.type == Result.ResultType.instruction) {
            return temp1.instrRef == temp2.instrRef;
        }
        else {
            return temp1.value == temp2.value;
        }
    }

}
