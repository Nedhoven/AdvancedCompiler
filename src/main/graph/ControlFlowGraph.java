
package main.graph;

import main.backend.BasicBlock;
import main.backend.BlockType;
import main.backend.FunctionDeclare;

import main.optimizer.Instruction;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ControlFlowGraph {

    private static BasicBlock firstBlock;
    private static List<BasicBlock> blocks;
    public static Map<Integer, FunctionDeclare> allFunctions;
    public static List<Instruction> allInstructions;
    public static DelUseChain delUseChain;

    public ControlFlowGraph() {
        blocks = new ArrayList<>();
        firstBlock = new BasicBlock(BlockType.NORMAL);
        allFunctions = new HashMap<>();
        allInstructions = new ArrayList<>();
        delUseChain = new DelUseChain();
        allFunctions.put(0, new FunctionDeclare(0));
        allFunctions.put(1, new FunctionDeclare(1));
        allFunctions.put(2, new FunctionDeclare(2));
        allFunctions.put(2, new FunctionDeclare(2));
    }

    public static BasicBlock getFirstBlock() {
        return firstBlock;
    }

    public static List<BasicBlock> getBlocks() {
        return blocks;
    }

    public static Instruction getInstruction(int pc) {
        for (BasicBlock block : blocks) {
            if (block.findInstruction(pc) != null) {
                return block.findInstruction(pc);
            }
        }
        Error("UNABLE TO FIND INSTRUCTION WITH PC = " + pc + "!");
        return null;
    }

    public static void printInstruction() {
        for (BasicBlock block : blocks) {
            System.out.println("Block_" + block.getId() + "[");
            for (Map.Entry<Integer, Instruction> entry : block.getPhiFunctionGenerator().getPhiInstructionMap().entrySet()) {
                System.out.println(entry.toString());
            }
            for (Instruction in : block.getInstructions()) {
                System.out.println(in.toString());
            }
            System.out.println("]");
        }
    }

    private static void Error(String msg) {
        System.err.println("CONTROL FLOW GRAPH ERROR: " + msg);
    }

}
