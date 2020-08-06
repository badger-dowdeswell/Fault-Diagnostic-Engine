//
// ErrorHandler
// ============
// Implements an error handler that can be deeply-nested in classes to capture
// descriptions of a list of error descriptions. It allows parser methods to log 
// an error that is able to be passed back up to higher-levels in another class.
//
// (c) AUT University - 2020
// 
// Revision history
// ================
// 14.07.2020 BRD Converted into a universal class that can be shared across all 
//                classes that need it.

package fde;

class ErrorHandler {
	String lastErrorDescription = "";
	
	//
	// addDescription()
	// ================
	public void addDescription(String errorDescription) {
		if (lastErrorDescription != "") {
			lastErrorDescription = lastErrorDescription + "\n" + errorDescription;
		} else {
			lastErrorDescription = errorDescription;
		}
	}
	
	// 
	// clear()
	// =======
	public void clear() {
		lastErrorDescription = "";
	}
	
	//
	// set Description()
	// =================
	public void Description(String errorDescription) {
		lastErrorDescription = errorDescription;
	}
	
	//
	// get Description()
	// =================
	public String Description() {
		return lastErrorDescription;
	}
}