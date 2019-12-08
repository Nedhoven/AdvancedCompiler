
package main;

import main.frontend.Parser;

import main.graph.ControlFlowGraph;
import main.graph.DominatorTreeGenerator;
import main.graph.VCGGraphGenerator;

import main.optimizer.CP;
import main.optimizer.CSE;
import main.optimizer.RegisterAllocation;

public class Main {

    public static void main(String[] args) throws Throwable {
        String testName = "test000";
        Parser p = new Parser("src/data/" + testName + ".txt");
        p.parser();
        // System.out.println(ControlFlowGraph.delUseChain.xDefUseChains);
        // System.out.println(ControlFlowGraph.delUseChain.yDefUseChains);
        // VCGGraphGenerator vcg = new VCGGraphGenerator(testName);
        // vcg.printCFG();
        DominatorTreeGenerator dt = new DominatorTreeGenerator();
        dt.buildDominatorTree(DominatorTreeGenerator.root);
        // vcg.printDominantTree();
        // VCGGraphGenerator vcg_cp = new VCGGraphGenerator(testName + "_CP");
        CP cp = new CP();
        cp.optimize(DominatorTreeGenerator.root);
        // vcg_cp.printDominantTree();
        // VCGGraphGenerator vcg_cse = new VCGGraphGenerator(testName + "_CSE");
        CSE cse = new CSE();
        cse.optimize(DominatorTreeGenerator.root);
        // vcg_cse.printDominantTree();
        RegisterAllocation ra = new RegisterAllocation();
        ra.allocate(DominatorTreeGenerator.root);
        ControlFlowGraph.printInstruction();
    }

}
