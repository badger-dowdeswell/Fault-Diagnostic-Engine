//
// AGENT BELIEF
// ============
// This class instantiates a single Belief that is used by
// the Beliefs class for form a collection of Beliefs.
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

//
// BeliefTypes
// ===========
class BeliefTypes {
	public static final int UNDEFINED = 0;				// Undefined. Used primarily when creating
														// a new belief.
	
	public static final int INTERACTION = 1;            // Beliefs related to the skills the agent 
	                                                    // possesses and can perform. 
	
	public static final int SYSTEM_UNDER_DIAGNOSIS = 2; // Beliefs that the agent holds about the 
	 													// the structure and configuration of the
	                                                    // function block application that it is
	                     								// has been tasked with diagnosing. See the
														// FunctionBlockApp class for more information.
	
	public static final int DYNAMIC = 3;				// Beliefs about what has happened during
	                                                    // the current fault finding session.
}

//
// VeracityTypes
// =============
class VeracityTypes {
	public static final int UNDETERMINED = 0;			// This belief has not been verified (tried, tested).
	public static final int TRUE = 1;         			// This belief has been tested and is now believed to
	         											// be true.
	public static final int FALSE = 2;					// This belief has been tested and is now believed to
														// be false.
}

public class Belief {
	private String Name = "";
	private int beliefType = BeliefTypes.UNDEFINED;
	private int veracity = VeracityTypes.UNDETERMINED;
	private ErrorHandler descriptionHandler = new ErrorHandler();
	
	//
	// get Name()
	// ==========
	public String Name() {
		return this.Name;
	}
	
	//
	// set Name()
	// ==========
	public void Name(String newName) {
		this.Name = newName;
	}
	
	// 
	// get BeliefType()
	// ================
	public int BeliefType() {
		return this.beliefType;
	}
	
	//
	// set BeliefType()
	// ================
	public void BeliefType(int newBeliefType) {
		this.beliefType = newBeliefType;
	}
	
	//
	// get Veracity()
	// ==============
	public int Veracity() {
		return this.veracity;
	}
	
	//
	// set Veracity()
	// ==============
	public void Veracity(int newVaracity) {
		this.veracity = newVaracity;
	}
	
	//
	// get Description()
	// =================
	public String Description() {
		return descriptionHandler.Description();
	}
	
	//
	// addDescription()
	// ================
	//
	public void addDescription(String beliefDescription) {
		descriptionHandler.addDescription(beliefDescription);
	}

	//
	// set Description()
	// =================
	public void Description(String newDescription) {
		descriptionHandler.Description(newDescription);
	}
}
