//
// FUNCTION BLOCK VARIABLE CLASS
// =============================
// This class implements a structure to hold the properties of a single function block.  
// variable. It is used within the class FunctionBlock() to manage a list of all the 
// variables defined for that instance. 
//
// (c) AUT University - 2019-2020
//
// Revision History
// ================
// 18.10.2019 BRD Original version.
// 05.11.2019 BRD Extended to manage data types for input and output ports.
// 15.08.2020 BRD Refactored the data types used by the diagnostic function 
//                blocks.

package fde;

enum VarTypes {
	VAR_UNDEFINED,
	VAR_INPUT,
	VAR_OUTPUT
}

//
// DATA TYPES
// ==========
// The data types used by 4DIAC and FORTE to define input and output data types for
// function blocks. These enumerations are also used by the AGENT_GATE in the DP
// composite function block to signal what data type is is being sent or is being 
// received.
//
class DataTypes {
	public static final int DATATYPE_UNDEFINED = -1;
	public static final int DATATYPE_EVENT = 0;
	public static final int DATATYPE_INT = 1;
	public static final int DATATYPE_LINT = 2;
	public static final int DATATYPE_REAL = 3;
	public static final int DATATYPE_LREAL = 4;
	public static final int DATATYPE_STRING = 5;
	public static final int DATATYPE_WSTRING = 6;
	public static final int DATATYPE_BOOL = 7;
}

public class FunctionBlockVariable {
	private String Name = "";
	private VarTypes VarType = VarTypes.VAR_UNDEFINED;  
	private int DataType = DataTypes.DATATYPE_INT;
	private String Comment = "";
	private String InitialValue = "";
			
	//
	// Get Name()
	// ==========
	public String Name() {
		return this.Name;
	}
	
	//
	// Set Name()
	// ==========
	public void Name(String newName) {
		this.Name = newName;
	}
	
	//
	// Get VarType()
	// =============
	public VarTypes VarType() {
		return this.VarType;
	}
	
	//
	// Set VarType()
	// =============
	public void VarType(VarTypes newVarType) {
		this.VarType = newVarType;
	}
	
	//
	// Get DataType()
	// =============
	public int DataType() {
		return this.DataType;
	}
	
	//
	// Set DataType()
	// =============
	public void DataType(int newDataType) {
		this.DataType = newDataType;
	}
	
	//
	// Get Comment()
	// =============
	public String Comment() {
		return this.Comment;
	}
	
	//
	// Set Comment()
	// =============
	public void Comment(String newComment) {
		this.Comment = newComment;
	}
	
	//
	// Get InitialValue()
	// ==================
	public String InitialValue() {
		return this.InitialValue;
	}
	
	//
	// Set InitialValue()
	// ==================
	public void InitialValue(String newInitialValue) {
		this.InitialValue = newInitialValue;
	}
	
	//
	// DataTypeFromString()
	// ====================
	// Given a description of a function block data type such as
	// "REAL", this function returns the correct integer enumerated 
	// data type for that variable.
	//
	public int DataTypeFromString(String dataTypeDesc) {
		int dataType = DataTypes.DATATYPE_UNDEFINED;
		
		switch (dataTypeDesc) {
		case "INT":
			dataType = DataTypes.DATATYPE_INT;
			break;
				
		case "LINT":
			dataType = DataTypes.DATATYPE_LINT;
			break;
			
		case "REAL":
			dataType = DataTypes.DATATYPE_REAL;
			break;
			
		case "LREAL":
			dataType = DataTypes.DATATYPE_LREAL;
			break;
			
		case "STRING":
			dataType = DataTypes.DATATYPE_STRING;
			break;
			
		case "WSTRING":
			dataType = DataTypes.DATATYPE_WSTRING;
			break;
			
		case "BOOL":
			dataType = DataTypes.DATATYPE_BOOL;
			break;
		}
		return dataType;
	}
	
	
	//
	// StringFromDataType()
	// ====================
	public String StringFromDataType(int dataType) {
		String dataTypeDesc = "";
		
		switch (dataType) {
		case DataTypes.DATATYPE_INT:
			dataTypeDesc = "INT";
			break;
				
		case DataTypes.DATATYPE_LINT:
			dataTypeDesc = "LINT";
			break;
			
		case DataTypes.DATATYPE_REAL:
			dataTypeDesc = "REAL";
			break;
			
		case DataTypes.DATATYPE_LREAL:
			dataTypeDesc = "LREAL";
			break;
			
		case DataTypes.DATATYPE_STRING:
			dataTypeDesc = "STRING";
			break;
			
		case DataTypes.DATATYPE_WSTRING:
			dataTypeDesc = "WSTRING";
			break;
			
		case DataTypes.DATATYPE_BOOL:
			dataTypeDesc = "BOOL";
			break;
		}	
		return dataTypeDesc;
	}
}
