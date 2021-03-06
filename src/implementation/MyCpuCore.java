/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import baseclasses.CpuCore;
import examples.MultiStageFunctionalUnit;
import tools.InstructionSequence;
import utilitytypes.IPipeReg;
import utilitytypes.IPipeStage;
import static utilitytypes.IProperties.*;
import utilitytypes.Logger;
import voidtypes.VoidRegister;

/**
 * This is an example of a class that builds a specific CPU simulator out of
 * pipeline stages and pipeline registers.
 * 
 * @author 
 */
public class MyCpuCore extends CpuCore {
    static final String[] producer_props = {RESULT_VALUE};
        
    public void initProperties() {
        properties = new GlobalData();
    }
    
    public void loadProgram(InstructionSequence program) {
        getGlobals().loadProgram(program);
    }
    
    public void runProgram() {
        properties.setProperty("running", true);
        while (properties.getPropertyBoolean("running")) {
            Logger.out.println("## Cycle number: " + cycle_number);
            advanceClock();
        }
    }

    @Override
    public void createPipelineRegisters() {
        createPipeReg("FetchToDecode");
        createPipeReg("DecodeToExecute");
        createPipeReg("DecodeToMemory");
        createPipeReg("DecodeToIntMul");
        createPipeReg("DecodeToAddSubCmp");
        createPipeReg("DecodeToFloatMul");
        createPipeReg("DecodeToIntDiv");
        createPipeReg("DecodeToFloatDiv");
        createPipeReg("IntDivToWriteback");
        createPipeReg("FloatDivToWriteback");
        createPipeReg("ExecuteToWriteback");
    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new IntDiv(this));
        addPipeStage(new FloatDiv(this));
        addPipeStage(new AllMyStages.Execute(this));
        addPipeStage(new AllMyStages.Memory(this));
        addPipeStage(new AllMyStages.Writeback(this));
    }

    @Override
    public void createChildModules() {
        // MSFU is an example multistage functional unit.  Use this as a
        // basis for FMul, IMul, and FAddSub functional units.
        addChildUnit(new IntMul(this, "IntMul"));
        addChildUnit(new AddSubCmp(this, "AddSubCmp"));
        addChildUnit(new FloatMul(this, "FloatMul"));
         addChildUnit(new Memory(this, "MemoryUnit"));
    }

    @Override
    public void createConnections() {
        // Connect pipeline elements by name.  Notice that 
        // Decode has multiple outputs, able to send to Memory, Execute,
        // or any other compute stages or functional units.
        // Writeback also has multiple inputs, able to receive from 
        // any of the compute units.all
        // NOTE: Memory no longer connects to Execute.  It is now a fully 
        // independent functional unit, parallel to Execute.
        
        // Connect two stages through a pipelin register
        connect("Fetch", "FetchToDecode", "Decode");
        
        // Decode has multiple output registers, connecting to different
        // execute units.  
        // "MSFU" is an example multistage functional unit.  Those that
        // follow the convention of having a single input stage and single
        // output register can be connected simply my naming the functional
        // unit.  The input to MSFU is really called "MSFU.in".
        connect("Decode", "DecodeToExecute", "Execute");
        connect("Decode", "DecodeToMemory", "MemoryUnit");
        connect("Decode", "DecodeToIntMul", "IntMul");
        connect("Decode","DecodeToAddSubCmp","AddSubCmp");
        connect("Decode","DecodeToFloatMul","FloatMul");
        connect("Decode","DecodeToIntDiv","IntDiv");
        connect("Decode","DecodeToFloatDiv","FloatDiv");
        
        // Writeback has multiple input connections from different execute
        // units.  The output from MSFU is really called "MSFU.Delay.out",
        // which was aliased to "MSFU.out" so that it would be automatically
        // identified as an output from MSFU.
        connect("IntDiv","IntDivToWriteback","Writeback");
        connect("FloatDiv","FloatDivToWriteback","Writeback");
        connect("Execute","ExecuteToWriteback", "Writeback");
        connect("MemoryUnit", "Writeback");
        connect("IntMul", "Writeback");
        connect("AddSubCmp","Writeback");
        connect("FloatMul","Writeback");
    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("ExecuteToWriteback");
        addForwardingSource("IntMul.out");
        addForwardingSource("AddSubCmp.out");
        addForwardingSource("FloatMul.out");
        addForwardingSource("IntDivToWriteback");
        addForwardingSource("FloatDivToWriteback");
        addForwardingSource("MemoryUnit.out");
        
        // MSFU.specifyForwardingSources is where this forwarding source is added
        // addForwardingSource("MSFU.out");
    }

    @Override
    public void specifyForwardingTargets() {
        // Not really used for anything yet
    }

    @Override
    public IPipeStage getFirstStage() {
        // CpuCore will sort stages into an optimal ordering.  This provides
        // the starting point.
        return getPipeStage("Fetch");
    }
    
    public MyCpuCore() {
        super(null, "core");
        initModule();
        printHierarchy();
        Logger.out.println("");
    }
}
