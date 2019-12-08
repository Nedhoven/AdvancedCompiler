
package main.graph;

import main.frontend.Lexer;

import main.backend.BasicBlock;
import main.backend.DominatorTreeNode;

import main.optimizer.Instruction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class VCGGraphGenerator {

    private PrintWriter writer;

    public VCGGraphGenerator(String outputName){
        try {
            String path = "src/test/";
            writer = new PrintWriter(new FileWriter(path + "vcg/" + outputName + ".vcg"));
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void printCFG() {
        writer.println("graph: { title: \"Control Flow Graph\"");
        writer.println("layout algorithm: dfs");
        writer.println("manhattan_edges: yes");
        writer.println("s_manhattan_edges: yes");
        for (BasicBlock block : ControlFlowGraph.getBlocks()) {
            printCFGNode(block);
        }
        writer.println("}");
        writer.close();
    }

    public void printDominantTree() {
        writer.println("graph: { title: \"Dominant Tree\"");
        writer.println("layout algorithm: dfs");
        writer.println("manhattan_edges: yes");
        writer.println("s_manhattan_edges: yes");
        printDominantTreeUtil(DominatorTreeGenerator.root);
        writer.println("}");
        writer.close();
    }

    private void printDominantTreeUtil(DominatorTreeNode root) {
        if (root == null) {
            return;
        }
        printDTNode(root);
        for (DominatorTreeNode child : root.children) {
            printDominantTreeUtil(child);
        }
    }

    private void printCFGNode(BasicBlock block) {
        writer.println("node: {");
        writer.println("title: \"" + block.getId() + "\"");
        writer.println("label: \"" + block.getId() + "[");
        for (Map.Entry<Integer, Instruction> entry : block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
            String var = Lexer.ids.get(entry.getKey());
            Instruction instr = entry.getValue();
            instr.setVariableName(var);
            printInstruction(entry.getValue());
        }
        for (Instruction inst : block.getInstructions()) {
            printInstruction(inst);
        }
        writer.println("]\"");
        writer.println("}");
        if (block.getFollowBlock() != null) {
            printEdge(block.getId(), block.getFollowBlock().getId());
        }
        if (block.getElseBlock() != null) {
            printEdge(block.getId(), block.getElseBlock().getId());
        }
        if (block.getBackBlock() != null) {
            printEdge(block.getId(), block.getBackBlock().getId());
        }
        if (block.getJoinBlock() != null && block.getFollowBlock() == null) {
            printEdge(block.getId(), block.getJoinBlock().getId());
        }
    }

    private void printDTNode(DominatorTreeNode node) {
        writer.println("node: {");
        writer.println("title: \"" + node.block.getId() + "\"");
        writer.println("label: \"" + node.block.getId() + "[");
        for (Map.Entry<Integer, Instruction> entry : node.block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
            String var = Lexer.ids.get(entry.getKey());
            Instruction instr = entry.getValue();
            instr.setVariableName(var);
            printInstruction(entry.getValue());
        }
        for (Instruction inst : node.block.getInstructions()) {
            printInstruction(inst);
        }
        writer.println("]\"");
        writer.println("}");
        for (DominatorTreeNode child : node.children){
            printEdge(node.block.getId(), child.block.getId());
        }
    }

    public void printEdge(int sourceId, int targetId) {
        writer.println("edge: { source name: \"" + sourceId + "\"");
        writer.println("target name: \"" + targetId + "\"");
        writer.println("color: blue");
        writer.println("}");
    }

    public void printInstruction(Instruction instruction) {
        writer.println(instruction);
    }

}
