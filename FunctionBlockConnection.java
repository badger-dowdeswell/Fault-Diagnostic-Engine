//
// FUNCTION BLOCK CONNECTIONS
// ==========================
// This class implements a structure to hold the properties of a connection that a function
// block instance needs to establish with another function block. It is used within
// the class FunctionBlockApp() to manage a list of all the connections specified for the
// application. 
//
// (c) AUT University - 2019-2020
//
// Revision History
// ================
// 15.01.2019 BRD Original version.
// 25.09.2019 BRD Separated the function block instance name from the input or output port name.
// 23.10.2019 BRD Rationalised property names to bring them in-line with Java
//                naming conventions.
// 15.11.2019 BRD Added the ability to enable and disable connections.
//
package fde;

public class FunctionBlockConnection {
	private String SourceFB = "";				// The source function block for this connection.
	private String SourceName = "";				// The output connection on the source function block
												// that is sending the data. 

	private String DestinationFB = "";          // The destination function block for this connection.
	private String DestinationName = "";  		// The input connection on the destination function 
												// block for this that is receiving this data.
	
	private String Comment = "";                // Documentation comment for this connection.
	boolean Enabled = false;					// Signals if a connection is active or disable. Used 
												// by the re-wiring function to disconnect blocks.
	
	//
	// get SourceFB()
	// ==============
	public String SourceFB() {
		return this.SourceFB;
	}
	
	//
	// set SourceFB()
	// ==============
	public void SourceFB(String SourceFB) {
		this.SourceFB = SourceFB;
	}
	
	//
	// get SourceName()
	// ================
	public String SourceName() {
		return this.SourceName;
	}
	
	//
	// set SourceName()
	// ================
	public void SourceName(String SourceName) {
		this.SourceName = SourceName;
	}
	
	//
	// get DestinationFB()
	// ===================
	public String DestinationFB() {
		return this.DestinationFB;
	}
	
	//
	// set DestinationFB()
	// ===================
	public void DestinationFB(String DestinationFB) {
		this.DestinationFB = DestinationFB;
	}
	
	//
	// get DestinationName()
	// =====================
	public String DestinationName() {
		return this.DestinationName;
	}
	
	//
	// set DestinationName()
	// =====================
	public void DestinationName(String DestinationName) {
		this.DestinationName = DestinationName;
	}
	
	//
	// get Comment()
	// =============
	public String Comment() {
		return this.Comment;
	}
	
	//
	// set Comment()
	// =============
	public void Comment(String Comment) {
		this.Comment = Comment;
	}
	
	//
	// get Enabled()
	// =============
	public boolean Enabled() {
		return this.Enabled;
	}
	
	//
	// set Enabled()
	// =============
	public void Enabled(boolean Enabled) {
		this.Enabled = Enabled;
	}
}
