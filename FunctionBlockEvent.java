//
// FUNCTION BLOCK EVENT CLASS
// ==========================
// This class implements a structure to hold the properties of a single function block event.  
// It is used within the class FunctionBlock() to manage a list of all the events specified 
// for that instance. 
//
// (c) AUT University - 2019-2020
//
// Revision History
// ================
// 17.10.2019 BRD Original version.
//
package fde;

import static fde.Constants.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

enum EventTypes {
	EVENT_UNDEFINED,
	EVENT_INPUT,
	EVENT_OUTPUT
}

public class FunctionBlockEvent {
	String EventName = "";
	EventTypes EventType = EventTypes.EVENT_UNDEFINED;  
	String Comment = "";
	
	List<String> fbWithVars = new ArrayList<String>();
		
	//
	// Get EventName()
	// ===============
	public String EventName() {
		return this.EventName;
	}
	
	//
	// Set EventName()
	// ===============
	public void EventName(String newEventName) {
		this.EventName = newEventName;
	}
	
	//
	// Get EventType()
	// ===============
	public EventTypes EventType() {
		return this.EventType;
	}
	
	//
	// Set EventType()
	// ===============
	public void EventType(EventTypes newEventType) {
		this.EventType = newEventType;
	}
	
	//
	// Set Comment()
	// =============
	public void Comment(String newComment) {
		this.Comment = newComment;
	}
	
	//
	// Get Comment()
	// =============
	public String Comment() {
		return this.Comment;
	}
	
	//
	// WithVarCount()
	// ==============
	public int WithVarCount() {
		return fbWithVars.size();
	}
	
	//
	// findWithVar()
	// =============
	public int findWithVar(String WithVarName) {
		int ptr = NOT_FOUND;
		int count = fbWithVars.size();
		for (int pos = 0 ; pos < count;  pos++) {
			if (fbWithVars.get(pos).equals(WithVarName)) {
				ptr = pos;
				break;
			}
		}	
		return ptr;
	}
	
	//
	// addWithVar()
	// ============
	public void addWithVar(String WithVarName) {
		fbWithVars.add(WithVarName);
	}
	
	//
	// get WithVar()
	// =============
	public String WithVar(int ptrWithVar) {
		String WithVar = "";
		if (ptrWithVar < fbWithVars.size() ) {
			WithVar = fbWithVars.get(ptrWithVar);
		}
		return WithVar;
	}
}

