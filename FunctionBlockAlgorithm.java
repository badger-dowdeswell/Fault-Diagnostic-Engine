//
// FUNCTION BLOCK ALGORITHM CLASS
// ==============================
// This class implements a structure to hold the properties of a single function block algorithm. 
// It is used within classes such as FunctionBlockApp to store a list of each function block  
// algorithm available within that function block type. 
//
//(c) AUT University - 2019-2020
//
// Revision History
// ================
// 15.01.2019 BRD Original version based on the Torus version 1.5, 18.06.2016. 
// 06.09.2019 BRD Refactored for use with FunctionBlockApp.
//
package fde;

public class FunctionBlockAlgorithm {
	private String algorithmName = "";			// The name of the algorithm defined by 4DIAC.
	private String algorithmComment = "";   	// The free-form comment line that accompanies this
												// algorithm.
	private String algorithmRequID = "";		// The requirement ID that this algorithm has been 
												// tagged with by either the developer or TORUS.
	private String algorithmCode = "";			// The free-form text for the source code of the algorithm.
	
	//
	// getAlgorithmName()
	// ==================
	public String getAlgorithmName() {
		return algorithmName;
	}
	
	//
	// setAlgorithmName()
	// ==================
	public void setAlgorithmName(String algorithmName) {
		this.algorithmName = algorithmName;
	}
	
	//
	// getAlgorithmComment()
	// =====================
	public String getAlgorithmComment() {
		return algorithmComment;
	}
	
	//
	// setAlgorithmComment()
	// =====================
	public void setAlgorithmComment(String algorithmComment) {
		this.algorithmComment = algorithmComment;
	}
	
	//
	// getAlgorithmRequID()
	// ====================
	public String getAlgorithmRequID() {
		return algorithmRequID;
	}
	
	//
	// setAlgorithmRequID()
	// ====================
	public void setAlgorithmRequID(String algorithmRequID) {
		this.algorithmRequID = algorithmRequID;
	}
	
	//
	// getAlgorithmCode()
	// ==================
	public String getAlgorithmCode() {
		return algorithmCode;
	}
	
	//
	// setAlgorithmCode()
	// ==================
	public void setAlgorithmCode(String algorithmCode) {
		this.algorithmCode = algorithmCode;
	}
}

