
package main.graph;

import main.backend.BasicBlock;
import main.backend.DominatorTreeNode;

import java.util.Set;
import java.util.HashSet;

public class DominatorTreeGenerator {

    public static DominatorTreeNode root;
    private static Set<BasicBlock> visited;

    public DominatorTreeGenerator() {
        root = new DominatorTreeNode(ControlFlowGraph.getFirstBlock());
        visited = new HashSet<>();
        visited.add(root.block);
    }

    public void buildDominatorTree(DominatorTreeNode start) {
        if (start != null) {
            if (start.getBasicBlock().getFollowBlock() != null && !(visited.contains(start.getBasicBlock().getFollowBlock()))) {
                DominatorTreeNode child = new DominatorTreeNode(start.getBasicBlock().getFollowBlock());
                start.getChildren().add(child);
                visited.add(start.getBasicBlock().getFollowBlock());
            }
            if (start.getBasicBlock().getJoinBlock() != null && !(visited.contains(start.getBasicBlock().getJoinBlock()))) {
                DominatorTreeNode child = new DominatorTreeNode(start.getBasicBlock().getJoinBlock());
                start.getChildren().add(child);
                visited.add(start.getBasicBlock().getJoinBlock());
            }
            if (start.getBasicBlock().getElseBlock() != null && !(visited.contains(start.getBasicBlock().getElseBlock()))) {
                DominatorTreeNode child = new DominatorTreeNode(start.getBasicBlock().getElseBlock());
                start.getChildren().add(child);
                visited.add(start.getBasicBlock().getElseBlock());
            }
            for (int i = 0; i < start.getChildren().size(); i++) {
                buildDominatorTree(start.getChildren().get(i));
            }
        }
        else {
            Error();
        }
    }

    private void Error() {
        System.err.println("DOMINATOR TREE GENERATOR FAILED: ROOT NODE IS NULL!");
    }

}
