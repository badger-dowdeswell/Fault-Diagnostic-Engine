//
// FUNCTION BLOCK CLASS
// ====================
// This class implements a structure to hold the properties of a single function block. It is used
// within classes such as FunctionBlockApp to store a list of each function blocks for an application. 
//
// (c) AUT University - 2019-2020
//
// Revision History
// ================
// 15.01.2019 BRD Original version based on the Torus version 1.5, 17.05.2016.
// 16.01.2019 BRD Added support for lists of function block instance parameters and connections.
// 17.01.2019 BRD Needs work to improve sub-List management. See all RA_BRD markers.
// 06.09.2019 BRD Refactored for use with the new FunctionBlockApp class.
//
package fde;
//
// List support packages
// =====================
import java.util.List;
import java.util.ArrayList;
import static fde.Constants.*;

public class FunctionBlock {
	private String fbID = "";     
	private String fbName = "";
	private String fbType = "";	
	private String fbComment = "";
	private String fbRequID = "";       // The primary requirement ID assigned to this function 
	                                    // block instance. Used by Torus in other applications
										// to facilitate traceability.
	private boolean HasTypeDef = false; // Set true if the function blocks type definition file
										// has been found, loaded and parsed.
	
	List<FunctionBlockAlgorithm> fbAlgorithms = new ArrayList<FunctionBlockAlgorithm>();
	List<FunctionBlockEvent> fbEvents = new ArrayList<FunctionBlockEvent>();
	
	List<FunctionBlockParameter> fbParameters = new ArrayList<FunctionBlockParameter>();
	
	List<FunctionBlockVariable> fbVariables = new ArrayList<FunctionBlockVariable>();
	
	//
	// get Name()
	// ==========
	public String Name() {
		return fbName;
	}

	//
	// set Name()
	// ==========
	public void Name(String fbName) {
		this.fbName = fbName;
	}

	//
	// get Type()
	// ==========
	public String Type() {
		return fbType;
	}
	
	//
	// set Type()
	// ==========
	public void Type(String fbType) {
		this.fbType = fbType;
	}
	
	//
	// get Comment()
	// =============
	public String Comment() {
		return fbComment;
	}
	
	//
	// set Comment()
	// ==============
	public void Comment(String fbComment) {
		this.fbComment = fbComment;
	}
	
	//
	// get HasTypeDef()
	// ==========
	public boolean HasTypeDef() {
		return HasTypeDef;
	}

	//
	// set HasTypeDef()
	// ==========
	public void HasTypeDef(boolean fbHasTypeDef) {
		this.HasTypeDef = fbHasTypeDef;
	}
	
	//
	// get Parameter()
	// ===============
	public FunctionBlockParameter Parameter(int ptr) {
		FunctionBlockParameter fbParameter = new FunctionBlockParameter() ;
	
		if (ptr < fbParameters.size()) {
			fbParameter = fbParameters.get(ptr);
		}	
		return fbParameter;
	}
	
	//
	// get Parameter()
	// ===============
	// <RA_BRD
	//
	public FunctionBlockParameter Parameter(String parameterName) {
		FunctionBlockParameter fbParameter = new FunctionBlockParameter();
	//	int ptr = findEvent(eventName);
	//	if (ptr != NOT_FOUND) {
	//		fbEvent = fbEvents.get(ptr); 
	//	}
		return fbParameter;
	}	
	
	//
	// eventCount()
	// ============
	public int eventCount() {
		return fbEvents.size();
	}
	
	//
	// findEvent()
	// ===========
	public int findEvent(String eventName) {
		int ptr = NOT_FOUND;
		int count = fbEvents.size();
		for (int pos = 0 ; pos < count;  pos++) {
			FunctionBlockEvent fbEvent = fbEvents.get(pos);
			if (fbEvent.EventName.equals(eventName)) {
				ptr = pos;
				break;
			}
		}	
		return ptr;
	}
	
	//
	// addEvent()
	// ==========
	public void addEvent(String eventName, EventTypes eventType) {
		FunctionBlockEvent fbEvent = new FunctionBlockEvent();
		fbEvent.EventName= eventName;
		fbEvent.EventType = eventType;
		fbEvents.add(fbEvent);
	}
	
	//
	// get Event()
	// ===========
	public FunctionBlockEvent Event(int ptr) {
		FunctionBlockEvent fbEvent = new FunctionBlockEvent();
	
		if (ptr < fbEvents.size()) {
			fbEvent = fbEvents.get(ptr);
		}	
		return fbEvent;
	}
	
	//
	// get Event()
	// ===========
	public FunctionBlockEvent Event(String eventName) {
		FunctionBlockEvent fbEvent = new FunctionBlockEvent();
		int ptr = findEvent(eventName);
		if (ptr != NOT_FOUND) {
			fbEvent = fbEvents.get(ptr); 
		}
		return fbEvent;
	}
	
	//
	// VarCount()
	// ==========
	public int VarCount() {
		return fbVariables.size();
	}
	
	//
	// findVar()
	// =========
	public int findVar(String varName) {
		int ptr = NOT_FOUND;
		int count = fbVariables.size();
		for (int pos = 0 ; pos < count;  pos++) {
			FunctionBlockVariable fbVariable = fbVariables.get(pos);
			if (fbVariable.Name.equals(varName)) {
				ptr = pos;
				break;
			}
		}	
		return ptr;
	}
	
	//
	// addVar()
	// ========
	public void addVar(String varName, VarTypes varType, int dataType, String comment, String initialValue) {
		FunctionBlockVariable fbVariable = new FunctionBlockVariable();
		fbVariable.Name = varName;
		fbVariable.VarType = varType;
		fbVariable.DataType = dataType;
		fbVariable.Comment(comment);
		fbVariable.InitialValue(initialValue);
		fbVariables.add(fbVariable);
	}
	
	//
	// get Var()
	// =========
	public FunctionBlockVariable Var(String varName) {
		FunctionBlockVariable fbVariable = new FunctionBlockVariable();
		int ptrVar = findVar(varName);
		
		if (ptrVar != NOT_FOUND) {
			fbVariable = fbVariables.get(ptrVar);
			return fbVariable;			
		}
		
		fbVariable.Name = "";
		return fbVariable;
	}
	
	//
	// get Var()
	// =========
	public FunctionBlockVariable Var(int ptrVar) {
		FunctionBlockVariable fbVariable = new FunctionBlockVariable();
		if (ptrVar < VarCount()) {
			fbVariable = fbVariables.get(ptrVar);
		}
		return fbVariable;
	}
	
	//
	// ParameterCount()
	// ================
	public int ParameterCount() {
		return fbParameters.size();
	}
	
	//
	// findParameter()
	// ===============
	public int findParameter(String parameterName) {
	//	FunctionBlockParameter fbParameter = new FunctionBlockParameter();
		int ptrParameter = NOT_FOUND;
		int parameterCount = fbParameters.size();
		for (int pos = 0 ; pos < parameterCount;  pos++) {
			if (fbParameters.get(pos).Name().equals(parameterName)) {				
				ptrParameter = pos;
				break;
			}
		}	
		return ptrParameter;
	}
	
	// 
	// addParameter()
	// ==============
	public void addParameter(String name, String value) {
		FunctionBlockParameter fbParameter = new FunctionBlockParameter();
		fbParameter.Name(name);
		fbParameter.Value(value);
		fbParameters.add(fbParameter);			
	}
}	

	

