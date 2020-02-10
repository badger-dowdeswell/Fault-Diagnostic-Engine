//
// FUNCTION BLOCK PARAMETER CLASS
// ==============================
// This class implements a structure to hold the properties of a single function block parameter. 
// It is used within the class FunctionBlock() to manage a list of all the connections specified 
// for that instance. 
//
// (c) AUT University - 2019-2020
//
// Revision History
// ================
// 15.01.2019 BRD Original version based on the Torus version 1.5, 18.06.2016. 
// 06.09.2019 BRD Refactored for use with FunctionBlockApp.
// 22.10.2019 BRD Rationalised property names to bring them in-line with Java
//                naming conventions.
//
//
package fde;

public class FunctionBlockParameter {
	private String parameterName = "";			// The name of the parameter defined by 4DIAC.
	private String parameterValue = "";			// The value assigned to this parameter.
	private String parameterComment = "";   	// The free-form comment line that accompanies this
												// parameter.
	
	//
	// get Name()
	// ==========
	public String Name() {
		return this.parameterName;
	}
	
	//
	// set Name()
	// ==========
	public void Name(String parameterName) {
		this.parameterName = parameterName;
	}
	
	//
	// get Value()
	// ===========
	public String Value() {
		return this.parameterValue;
	}
	
	//
	// set Value()
	// ===========
	public void Value(String parameterValue) {
		this.parameterValue = parameterValue;
	}
	
	//
	// get Comment()
	// =============
	public String Comment() {
		return this.parameterComment;
	}
	
	//
	// set Comment()
	// =============
	public void Comment(String parameterComment) {
		this.parameterComment = parameterComment;
	}
}
