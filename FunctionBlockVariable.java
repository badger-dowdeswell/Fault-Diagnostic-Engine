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
// function blocks. These enumerations are also used by the AGENT_SEND and AGENT_RECV
// function blocks to signal what data type is is being sent or is being received.
//
class DataTypes {
	public static final int DATA_UNDEFINED = -1;	
	public static final int DATA_INT = 0;
	public static final int DATA_LINT = 1;
	public static final int DATA_REAL = 2;
	public static final int DATA_LREAL = 3;
	public static final int DATA_STRING = 4;
	public static final int DATA_WSTRING = 5;
	public static final int DATA_BOOL = 6;
}

public class FunctionBlockVariable {
	String Name = "";
	VarTypes VarType = VarTypes.VAR_UNDEFINED;  
	int DataType = DataTypes.DATA_INT;
	String Comment = "";
	String InitialValue = "";
			
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
		int dataType = DataTypes.DATA_UNDEFINED;
		
		switch (dataTypeDesc) {
		case "INT":
			dataType = DataTypes.DATA_INT;
			break;
				
		case "LINT":
			dataType = DataTypes.DATA_LINT;
			break;
			
		case "REAL":
			dataType = DataTypes.DATA_REAL;
			break;
			
		case "LREAL":
			dataType = DataTypes.DATA_LREAL;
			break;
			
		case "STRING":
			dataType = DataTypes.DATA_STRING;
			break;
			
		case "WSTRING":
			dataType = DataTypes.DATA_WSTRING;
			break;
			
		case "BOOL":
			dataType = DataTypes.DATA_BOOL;
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
		case DataTypes.DATA_INT:
			dataTypeDesc = "INT";
			break;
				
		case DataTypes.DATA_LINT:
			dataTypeDesc = "LINT";
			break;
			
		case DataTypes.DATA_REAL:
			dataTypeDesc = "REAL";
			break;
			
		case DataTypes.DATA_LREAL:
			dataTypeDesc = "LREAL";
			break;
			
		case DataTypes.DATA_STRING:
			dataTypeDesc = "STRING";
			break;
			
		case DataTypes.DATA_WSTRING:
			dataTypeDesc = "WSTRING";
			break;
			
		case DataTypes.DATA_BOOL:
			dataTypeDesc = "BOOL";
			break;
		}	
		return dataTypeDesc;
	}
}
