
package main.optimizer;

import main.backend.BlockType;
import main.backend.DominatorTreeNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class RegisterAllocation {

    private static class Graph {

        private Map<Instruction, List<Instruction>> edgeList;

        Graph() {
            edgeList = new HashMap<>();
        }

        void addNode(Instruction a) {
            if (!edgeList.containsKey(a)) {
                edgeList.put(a, new ArrayList<>());
            }
        }

        void addEdge(Instruction a, Instruction b) {
            if (!edgeList.containsKey(a)) {
                edgeList.put(a, new ArrayList<>());
            }
            if (!edgeList.containsKey(b)) {
                edgeList.put(b, new ArrayList<>());
            }
            if (!isEdge(a, b)) {
                edgeList.get(a).add(b);
                edgeList.get(b).add(a);
            }
        }

        List<Instruction> getNeighbors(Instruction a) {
            if (!edgeList.containsKey(a)) {
                return new ArrayList<>();
            }
            return edgeList.get(a);
        }

        int getDegree(Instruction a) {
            if (!edgeList.containsKey(a)) {
                System.err.println("REGISTER ALLOCATION ERROR: INSTRUCTION NOT FOUND!");
                return -1;
            }
            return edgeList.get(a).size();
        }

        boolean isEdge(Instruction a, Instruction b) {
            return edgeList.containsKey(a) && edgeList.get(a).contains(b);
        }

        Set<Instruction> getNodes() {
            return edgeList.keySet();
        }

    }

    private static class BasicBlockInfo {

        int visited;
        Set<Instruction> live;

        BasicBlockInfo() {
            visited = 0;
            live = new HashSet<>();
        }

    }

    private Graph interferenceGraph;
    private Map<DominatorTreeNode, BasicBlockInfo> bbInfo;
    private Set<DominatorTreeNode> loopHeaders;
    private Map<Instruction, Integer> colors;

    public RegisterAllocation() { }

    public Map<Integer, Integer> allocate(DominatorTreeNode b) {
        interferenceGraph = new Graph();
        bbInfo = new HashMap<>();
        loopHeaders = new HashSet<>();
        colors = new HashMap<>();
        calculateLiveRange(b,null,1);
        calculateLiveRange(b,null,2);
        colorGraph();
        Map<Integer, Integer> coloredIDs = new HashMap<>();
        for (Instruction i : colors.keySet()) {
            coloredIDs.put(i.getInstructionPC(), colors.get(i));
        }
        return coloredIDs;
    }

    private void colorGraph() {
        Instruction maxDegree;
        while (true) {
            maxDegree = null;
            for (Instruction a : interferenceGraph.getNodes()) {
                if (!colors.containsKey(a) && (maxDegree == null || interferenceGraph.getDegree(a) > interferenceGraph.getDegree(maxDegree))) {
                    maxDegree = a;
                }
            }
            if (maxDegree == null) {
                return;
            }
            Set<Integer> taken = new HashSet<>();
            for (Instruction b : interferenceGraph.getNeighbors(maxDegree)) {
                if (colors.containsKey(b)) {
                    taken.add(colors.get(b));
                }
            }
            int reg = 1;
            while (taken.contains(reg)) {
                reg += 1;
            }
            colors.put(maxDegree, reg);
        }
    }

    private void saveVCGGraph(String filename) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(new File(filename)));
            out.write("graph: {\n");
            out.write("title: \"Interference Graph\"\n");
            out.write("layout algorithm: dfs\n");
            out.write("manhattan_edges: yes\n");
            out.write("s_manhattan_edges: yes\n");
            Set<Instruction> done = new HashSet<>();
            for (Instruction a : interferenceGraph.getNodes()) {
                done.add(a);
                out.write("node: {\n");
                out.write("title: \"" + a.getInstructionPC() + "\"\n");
                out.write("label: \"" + "REG: " + colors.get(a) + " ::: " + a.toString() + "\"\n");
                out.write("}\n");
                for (Instruction b : interferenceGraph.getNodes()) {
                    if (interferenceGraph.isEdge(a, b) && !done.contains(b)) {
                        out.write("edge: {\n");
                        out.write("source name: \"" + a.getInstructionPC() + "\"\n");
                        out.write("target name: \"" + b.getInstructionPC() + "\"\n");
                        out.write("color: blue\n");
                        out.write("}\n");
                    }
                }
            }
            out.write("}\n");
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Instruction> calculateLiveRange(DominatorTreeNode b, DominatorTreeNode last, int pass) {
        Set<Instruction> live = new HashSet<>();
        if (!bbInfo.containsKey(b)) {
            bbInfo.put(b, new BasicBlockInfo());
        }
        if (b != null) {
            if (bbInfo.get(b).visited >= pass) {
                live.addAll(bbInfo.get(b).live);
            }
            else {
                bbInfo.get(b).visited += 1;
                if (bbInfo.get(b).visited == 2) {
                    for (DominatorTreeNode h : loopHeaders) {
                        bbInfo.get(b).live.addAll(bbInfo.get(h).live);
                    }
                }
                int index = 0;
                for (DominatorTreeNode child : b.getChildren()) {
                    if (b.block.getType() == BlockType.WHILE_JOIN && index == 0) {
                        loopHeaders.add(b);
                    }
                    live.addAll(calculateLiveRange(child, b, pass));
                    index += 1;
                    if (b.block.getType() == BlockType.WHILE_JOIN && index == 0) {
                        loopHeaders.remove(b);
                    }
                }
                List<Instruction> reverse = new ArrayList<>(b.block.getInstructions());
                Collections.reverse(reverse);
                for (Instruction ins : reverse) {
                    if (ins.getOp() != InstructionType.PHI && (!ins.deleted) && ins.getOp()!=InstructionType.END) {
                        live.remove(ins);
                        interferenceGraph.addNode(ins);
                        for (Instruction other : live) {
                            interferenceGraph.addEdge(ins, other);
                        }
                        if (ins.getLeftResult()!= null && ins.getLeftResult().type == Result.ResultType.instruction) {
                            if (last != null && last.block.findInstruction(ins.getLeftResult().instrRef) != null) {
                                live.add(last.block.findInstruction(ins.getLeftResult().instrRef));
                            }
                            else if (b.block.findInstruction(ins.getLeftResult().instrRef) != null) {
                                live.add(b.block.findInstruction(ins.getLeftResult().instrRef));
                            }
                        }
                        if (ins.getRightResult()!= null && ins.getRightResult().type == Result.ResultType.instruction) {
                            if (last != null && last.block.findInstruction(ins.getRightResult().instrRef) != null) {
                                live.add(last.block.findInstruction(ins.getRightResult().instrRef));
                            }
                            else if (b.block.findInstruction(ins.getRightResult().instrRef) != null) {
                                live.add(b.block.findInstruction(ins.getRightResult().instrRef));
                            }
                        }
                    }
                }
                bbInfo.get(b).live = new HashSet<>();
                bbInfo.get(b).live.addAll(live);
            }
            List<Instruction> reverse = new ArrayList<>(b.block.getInstructions());
            Collections.reverse(reverse);
            for (Instruction ins : reverse) {
                if (ins.getOp() == InstructionType.PHI) {
                    live.remove(ins);
                    interferenceGraph.addNode(ins);
                    for (Instruction other : live) {
                        interferenceGraph.addEdge(ins, other);
                    }
                    if (ins.getLeftResult()!= null && ins.getLeftResult().type == Result.ResultType.instruction) {
                        if (last != null && last.block.findInstruction(ins.getLeftResult().instrRef) != null) {
                            live.add(last.block.findInstruction(ins.getLeftResult().instrRef));
                        }
                        else if (b.block.findInstruction(ins.getLeftResult().instrRef) != null) {
                            live.add(b.block.findInstruction(ins.getLeftResult().instrRef));
                        }
                    }
                    if (ins.getRightResult()!= null && ins.getRightResult().type == Result.ResultType.instruction) {
                        if (last != null && last.block.findInstruction(ins.getRightResult().instrRef) != null) {
                            live.add(last.block.findInstruction(ins.getRightResult().instrRef));
                        }
                        else if (b.block.findInstruction(ins.getRightResult().instrRef) != null) {
                            live.add(b.block.findInstruction(ins.getRightResult().instrRef));
                        }
                    }
                }
            }
        }
        return live;
    }

}
