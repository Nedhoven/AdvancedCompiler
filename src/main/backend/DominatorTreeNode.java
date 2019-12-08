
package main.backend;

import java.util.List;
import java.util.ArrayList;

public class DominatorTreeNode {

    public BasicBlock block;
    public List<DominatorTreeNode> children;

    public DominatorTreeNode(BasicBlock block){
        this.block = block;
        this.children = new ArrayList<>();
    }

    public BasicBlock getBasicBlock() {
        return block;
    }

    public void setBasicBlock(BasicBlock bb) {
        block = bb;
    }

    public List<DominatorTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<DominatorTreeNode> kids) {
        children = kids;
    }

}
