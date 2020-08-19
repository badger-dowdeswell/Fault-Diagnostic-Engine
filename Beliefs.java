//
// AGENT BELIEFS
// =============
// This class instantiates a collection of Beliefs that the 
// an agent can hold, re-evaluate, update and present. They
// enable an agent to remember what it has done and what it
// believes about what it has observed when performing a
// fault diagnosis.
//
// AUT University - 2020
//
// Documentation
// =============
// Refer to the paper "Employing Agent Beliefs during
// Fault Diagnosis for IEC 61499 Industrial Cyber-Physical
// Systems" (Dowdeswell et al., 2020).
//
// Revision History
// ================
// 19.08.2020 BRD Original version.
//
package fde;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Beliefs {
	private List<Belief> beliefs = new ArrayList<Belief>();
	
	//
	// findBelief()
	// ============
	// Search for a belief by name.
	//
	public Belief findBelief(String name) {
		Belief belief = new Belief();
		
		for (int ptrBelief = 0; ptrBelief < beliefs.size(); ptrBelief++) {
			belief = beliefs.get(ptrBelief);
			if (belief.Name().equals(name)) {
				return belief;
			}
		}	
		return belief;
	}
	
	//
	// create()
	// ========
	// Creates a new belief in a single step
	//
	public void create(String name, int beliefType, int veracity, String description) {
		Belief belief = new Belief();
		belief.Name(name);
		belief.BeliefType(beliefType);
		belief.Veracity(veracity);
		belief.Description(description);
		beliefs.add(belief);
	}
	
	//
	// add()
	// =====
	// Creates a new belief.
	//
	public void add(Belief belief) {
		beliefs.add(belief);
	}
	
	// 
	// get Count()
	// ===========
	// Returns the number of beliefs currently held by this agent.
	//
	public int Count() {
		return beliefs.size();
	}
	
	//
	// updateBelief()
	// ==============
	// Replaces the belief in the list with the updated version.
	//
	public void updateBelief(Belief newBelief) {
		Belief belief = new Belief();
		int beliefCount = beliefs.size();
		String name = newBelief.Name();
		
		for (int ptrBelief = 0; ptrBelief < beliefs.size(); ptrBelief++) {
			belief = beliefs.get(ptrBelief);
			if (belief.Name().equals(name)) {
				belief.Name(newBelief.Name());
				belief.BeliefType(newBelief.BeliefType());
				belief.Description(newBelief.Description());
				belief.Veracity(newBelief.Veracity());
				Collections.replaceAll(beliefs, beliefs.get(ptrBelief), belief);
				break;
			}
		}	
	}
	
	//
	// displayBeliefs()
	// ================
	// Used to dump the current beliefs to the consul during
	// development.
	//
	public void displayBeliefs( ) {
		Belief belief = new Belief();
		System.out.println("\nBeliefs\n=======");
		for (int ptrBelief = 0; ptrBelief < beliefs.size(); ptrBelief++) {
			belief = beliefs.get(ptrBelief);
			System.out.printf("%-15s type:", belief.Name());
			switch (belief.BeliefType()) {
			case BeliefTypes.UNDEFINED:
				System.out.print("Undefined  ");
				break;
				
			case BeliefTypes.INTERACTION:	
				System.out.print("Interaction");
		
			case BeliefTypes.SYSTEM_UNDER_DIAGNOSIS:
				System.out.print("SuD        ");
				break;			
			
			case BeliefTypes.DYNAMIC:
				System.out.print("Dynamic    ");
				break;
			}
			System.out.print(" veracity: ");
			
			switch (belief.Veracity()) {
			case VeracityTypes.TRUE:
				System.out.print("true         ");
				break;			
				
			case VeracityTypes.FALSE:
				System.out.print("false        ");
				break;			
				
			case VeracityTypes.UNDETERMINED:
				System.out.print("undetermined ");
				break;			
			}
			System.out.println(" desc: " + belief.Description());
		}
		System.out.println("");	
	}
	

	
	
	
	
	
	
	
	
	
	
}
