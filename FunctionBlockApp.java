//
// FUNCTION BLOCK APPLICATION
// ==========================
// Loads and parses the XML-format function block application definition file,
// extracting the properties of all components into a hierarchical data
// structure.
//
// (c) AUT University - 2019-2020
//
// Documentation
// =============
// Full documentation for this class is contained in the Fault Diagnosis System
// Software Architecture document.
//
// See also https://examples.javacodegeeks.com/core-java/xml/java-xml-parser-tutorial/ 
//
// Revision History
//================
// 04.09.2019 BRD Original version.
// 23.10.2019 BRD Rationalised property names to bring them in-line with Java
//  	          naming conventions.
// 08.11.2019 BRD Added error handler to cache error messages.
//
package fde;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static fde.Constants.*;
//
// SAX XML Parser support packages
// ===============================
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

enum fbAppCodes {
	UNDEFINED,			// Default undefined state.
	INVALID_NAME,       // Blank or invalid function block name was specified.
	INVALID_PORT_NAME,  // Name of the function block port was blank or invalid.
	FB_NOT_FOUND,		// Function block specified in the command could not be found.
	EVENT_UNDEFINED,    // No event could be found that would trigger the output data.
	REWIRE_FAILED,		// Could not rewire the function block specified. 
	REWIRED				// Application was rewired successfully.
}

//
// FunctionBlockApp
// ================
public class FunctionBlockApp {
	String applicationPath = "";       	// The path to the directory where the function block application
 								   	// XML-format <application name>.sys file is located.
	String applicationFileName = "";   	// The name of the function block application qualified with a 
 								   	// .sys file extension.
	String applicationName = "";        // Main application name.
	String FORTEruntimeDirectory = ""; 	// The path to the FORTE runtime folder. The FORTE runtime checks
 								   	// for the presence of a forte.boot file in this folder when it
	   									// starts.
	String lastErrorDescription = "";   // The text explanation of what went wrong if the application 
	                                    // file could not be loaded and parsed successfully. Returned
										// by getLastErrorDescription().

										// Structure to hold a list of all the function blocks
 									// found within the application.
	private List<FunctionBlock> fbs = new ArrayList<FunctionBlock>(); 
										// Structure to hold a list of all the connections
										// between the function blocks.
	private List<FunctionBlockConnection> fbc = new ArrayList<FunctionBlockConnection>();
	
	private ErrorHandler errorHandler = new ErrorHandler();

	//
	// load()
	// ======
	// Loads and parses the named function block application .sys file.
	//
	// applicationPath      The fully-qualified path to the directory where the function block application
	//                      XML-format <application name>.sys file is located.
	//
	// applicationFileName	The name of the file that holds the application. This is a proprietary
	//      				XML format based on the IEC 61499 Function Block standard.
	//
	// returns           	Returns an enumerated type from the XMLErrorCodes list that reports if the 
	//                      file could be loaded and parsed successfully. If an error code is returned,
	//                      getLastErrorDescription() returns information about what went wrong.
	//
	public int load(String applicationPath, String applicationFileName) {
		int loadStatus = XMLErrorCodes.UNDEFINED;
		errorHandler.clear();

		this.applicationPath = applicationPath;
		this.applicationFileName = applicationFileName;	

		if (applicationPath == "") {
			loadStatus = XMLErrorCodes.UNDEFINED_FILE_PATH;		
			errorHandler.addDescription("Path to the application file has not been specified");			

		} else if (applicationFileName == "") {
			loadStatus = XMLErrorCodes.UNDEFINED_FILE_NAME;		
			errorHandler.addDescription("Application file name has not been specified");

		} else {
			try {
				// The directory path object used to access the XML file that contains the application.
				File fbapp = new File(applicationPath + "/" + applicationFileName);  
				
				// Create and configure a SAX XML parser 
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				// The dtd for this file is not available so ensure that the parser does
				// not return an error.
				parserFactory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
		        javax.xml.parsers.SAXParser parser = parserFactory.newSAXParser();
		        SAXParser handler = new SAXParser(applicationPath, fbs, fbc, errorHandler);
		                
		        // Pass the function block application file handle to the SAX parser and
		        // start it parsing. It will call its handler functions to give us access 
		        // to the data each time it has unpacked an element, returning back here
		        // when it finishes.
		        parser.parse(fbapp, handler);
		        
		        if (errorHandler.Description() != "") {
		        	System.out.println(errorHandler.Description());
		        	loadStatus = XMLErrorCodes.UNEXPECTED_ERROR;
		        } else {
		        	checkStartFB(fbs, fbc);
		        	loadStatus = XMLErrorCodes.LOADED;
		        }
		      
			} catch (Exception e) {
				loadStatus = XMLErrorCodes.UNEXPECTED_ERROR;
				lastErrorDescription = e.getMessage();
			}
		}	
		return loadStatus;
	}
	
	//
	// checkStartFB()
	// ==============
	// The presence of a generic START function block in a function block application definition
	// is signalled by the presence of a connection to a function block called START. However, that
	// function block is not defined anywhere else in the .sys file. This method scans the list
	// of connections and creates a START function block if it is needed.
	//
	private void checkStartFB(List<FunctionBlock> fbs, List<FunctionBlockConnection> fbc) {
		FunctionBlock fb = new FunctionBlock();
		FunctionBlockConnection fbconnection = new FunctionBlockConnection();
		
		for(int ptrConnection = 0; ptrConnection < ConnectionCount(); ptrConnection++) {
			fbconnection = fbc.get(ptrConnection);
			if (fbconnection.SourceFB().equals("START")) {
				// A START function block is being referenced
				// so create it.
				fb.Name("START");
				fb.Type("START");
				fb.HasTypeDef(true);
				fbs.add(fb);
				break;
			}
		}	
	}	
		
	//
	// get applicationPath()
	// =====================
	public String applicationPath() {
		return this.applicationPath;       	// The path to the directory where the function block application
	}
	
	//
	// get applicationFileName()
	// =========================
	public String applicationFileName() {
		return this.applicationFileName;
	}
	
	//
	// get applicationName()
	// =====================
	public String applicationName() {
		return this.applicationFileName;   	
	}
	
	//
	// get fbCount()
	// =============
	// Returns the number of function blocks in this application
	//
	public int fbCount() {
		return fbs.size();
	}
	
	//
	// get fb()
	// ========
	// Returns a copy of the function block by specifying
	// the index to the list entry.
	//
	public FunctionBlock getfb(int ptrfb) {
		FunctionBlock fb = new FunctionBlock();
		if (ptrfb < fbs.size()) {
			fb = fbs.get(ptrfb);
		}
		return fb;
	}
	
	//
	// findfb()
	// ========
	// Search for an return a function block given its name.
	//
	public FunctionBlock findfb(String name) {
		FunctionBlock fb = new FunctionBlock();
		boolean found = false;
		
		for (int ptrfb = 0; ptrfb < fbs.size(); ptrfb++) {
			fb = fbs.get(ptrfb);
			if (fb.Name().equals(name)) {
				return fb;
			}
		}	

		fb.Name("");
		return fb;
	}
	
	//
	// add()
	// =====
	// Add a new function block to the current list. Note that
	// this function is public. If it is called and the function
	// blocks type definition file has not been loaded and parsed,
	// this function attempts to retrieve the missing data.
	//
	public void add(FunctionBlock fb) {
		fbs.add(fb);
	}
	
	//
	// get ConnectionCount()
	// =====================
	// Returns the number of connections between the function block
	// in this application.
	//
	public int ConnectionCount() {
		return fbc.size();
	}
	
	//
	// get Connection()
	// ================
	public FunctionBlockConnection Connection(int ptrConnection) {
		FunctionBlockConnection fbconnection = new FunctionBlockConnection();
		if (ptrConnection < fbc.size()) {
			fbconnection = fbc.get(ptrConnection);
		}	
		return fbconnection;
	}
	
	//
	// addConnection()
	// ===============
	public void addConnection(FunctionBlockConnection fbConnection) {
		fbc.add(fbConnection);
	}
	
	//
	// addConnection()
	// ===============
	public void addConnection(String SourceFB, String SourceName, String DestinationFB, String DestinationName, boolean Enabled) {
		FunctionBlockConnection fbConnection = new FunctionBlockConnection();
		fbConnection.SourceFB(SourceFB);
		fbConnection.SourceName(SourceName);
		fbConnection.DestinationFB(DestinationFB);
		fbConnection.DestinationName(DestinationName);
		fbConnection.Enabled(Enabled);
		fbc.add(fbConnection);
	}
	
	//
	// updateConnection()
	// ==================
	public void updateConnection(FunctionBlockConnection fbconnection, String newSourceFB, String newSourceName, String newDestinationFB, String newDestinationName, boolean newEnabled) {
		int ptr = NOT_FOUND;
		int connectionCount = fbc.size();
		FunctionBlockConnection newfbconn = new FunctionBlockConnection();
		
		if (connectionCount > 0) {
			for (int pos = 0; pos <connectionCount; pos++) {
				if(fbconnection.SourceFB().equals(fbc.get(pos).SourceFB())) {
					if(fbconnection.SourceName().equals(fbc.get(pos).SourceName())) {
						if(fbconnection.DestinationFB().equals(fbc.get(pos).DestinationFB())) {
							if(fbconnection.DestinationName().equals(fbc.get(pos).DestinationName())) {
								newfbconn.SourceFB(newSourceFB);
								newfbconn.SourceName(newSourceName);
								newfbconn.DestinationFB(newDestinationFB);
								newfbconn.DestinationName(newDestinationName);
								newfbconn.Enabled(newEnabled);
								Collections.replaceAll(fbc, fbc.get(pos), newfbconn);
								break;
							}
						}
					}
				}
			}
		}
	}	
		
	//
	// get ParameterCount()
	// ====================
	public int ParameterCount(int ptrfb) {
		if(ptrfb <= fbs.size()) {
			return fbs.get(ptrfb).fbParameters.size();
		} else {
			return 0;
		}	
	}
	
	//
	// displayFunctionBlocks()
	// =======================
	// Display a list of the function blocks currently loaded into this structure.
	//
	public void displayFunctionBlocks(FunctionBlockApp fbapp) {
		FunctionBlock fb = new FunctionBlock();
		FunctionBlockEvent fbevent = new FunctionBlockEvent();
		FunctionBlockParameter fbparameter = new FunctionBlockParameter();
		FunctionBlockConnection fbconn = new FunctionBlockConnection();
		FunctionBlockVariable fbVariable  = new FunctionBlockVariable();
		
		System.out.println("____________________________________________________________");
		System.out.println("FUNCTION BLOCK APPLICATION");
		System.out.println("Application name   :" + fbapp.applicationName());
		System.out.println("Path and file name :" + fbapp.applicationPath + fbapp.applicationFileName());
		System.out.println("Functions blocks   :" + fbapp.fbCount());
		
		for (int ptr = 0; ptr < fbapp.fbCount(); ptr++) {
			System.out.println("____________________________________________________________");
			fb = fbapp.getfb(ptr);
			System.out.println("FUNCTION BLOCK");
			System.out.printf("%16s %d\n",  "instance :", ptr);
			System.out.printf("%16s %s\n",  "Name     :", fb.Name()); 
			System.out.printf("%16s %s",    "Type     :", fb.Type());
			if (fb.HasTypeDef()) {
				System.out.println("  (loaded)");
			} else {
				System.out.println("  (not loaded)");
			}
			System.out.printf("%16s %s\n",  "Comment  :", fb.Comment());
			System.out.println();
			
			System.out.println("EVENTS [" + fb.eventCount() + "]");
			for (int ptrEvent = 0; ptrEvent < fb.eventCount(); ptrEvent++) {
				fbevent = fb.Event(ptrEvent);
				System.out.printf("%16s %d\n",  "instance :", ptrEvent);
				System.out.printf("%16s %s\n",      "Name :", fbevent.EventName());
				
				switch (fbevent.EventType) {
				case EVENT_INPUT:
					System.out.printf("%16s %s\n",  "Event Type :", "Input");
					break;
				
				case EVENT_OUTPUT:
					System.out.printf("%16s %s\n",  "Event Type :", "Output");
					break;
				}
				
				for (int ptrWithVar = 0; ptrWithVar < fbevent.WithVarCount(); ptrWithVar++) {
					System.out.printf("%16s %s\n",  "With Var :", fbevent.WithVar(ptrWithVar));
				}
				System.out.println();
			}
			
			// The input and output variables (ports) defined for this function block
			System.out.println("VARIABLES (input and output) [" + fb.VarCount() + "]");
			for (int ptrVar = 0; ptrVar < fb.VarCount(); ptrVar++) {
				fbVariable = fb.Var(ptrVar);
				System.out.printf("%16s %d\n",       "instance : ", ptrVar);
				System.out.printf("%16s %s\n",           "Name : ", fbVariable.Name());
				if (fbVariable.VarType() == VarTypes.VAR_INPUT) {
					System.out.printf("%16s %s\n",       "Type : ", "Input"); 
					System.out.printf("%16s %s\n",  "Initial Value : ", fbVariable.InitialValue());
				} else {
					System.out.printf("%16s %s\n",           "Type : ", "Output"); 
					System.out.printf("%15s %s\n",  "Initial Value : ", "undefined until runtime");	
				}
				System.out.printf("%16s %s\n",        "Data Type : ", fbVariable.StringFromDataType(fbVariable.DataType()));	
				
				System.out.printf("%16s %s\n\n",      "Comment : ", fbVariable.Comment());	
				
			}
			
			// The parameter list for this function block. 
			System.out.println("PARAMETERS [" + fb.ParameterCount() + "]");
			for (int ptrParameter = 0; ptrParameter < fb.ParameterCount(); ptrParameter++) {
				fbparameter = fb.Parameter(ptrParameter);
				System.out.printf("%16s %d\n",   "instance : ", ptrParameter);
				System.out.printf("%16s %s\n",       "Name : ", fbparameter.Name());
				System.out.printf("%16s %s\n\n",    "Value : ", fbparameter.Value());	
			}
		}
		
		System.out.println("____________________________________________________________");

		System.out.println("\nCONNECTIONS [" + fbapp.ConnectionCount() + "]\n");
		for(int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
			fbconn = fbapp.Connection(ptrConnection);
			System.out.print(fbconn.SourceFB() + " " + fbconn.SourceName() 
							   + " ---> " + 
							   fbconn.DestinationFB() + " " + fbconn.DestinationName() );
			if (fbconn.Enabled()) {
				System.out.println(" [enabled]");
			} else {
				System.out.println(" [disabled]");
			}
		}
		System.out.println("____________________________________________________________");
	}
		
	//
	// get LastErrorDescription()
	// ==========================
	// Returns a text description of the last error that has occurred in a method within
	// this class.
	//
	public String LastErrorDescription() {
		return lastErrorDescription; 
	}
}	
	
//
//	SAXParser
//	=========
//	SAX is an event-based sequential access parser API that provides a mechanism for reading data from an XML document. It
//	is an alternative to that provided by a DOM parser. A SAX parser only needs to report each parsing event as it happens
//	and the minimum memory required for a SAX parser is proportional to the maximum depth of the XML file. One major 
//	advantage of the SAX parser is that when retrieving various data elements from a node, the endElement() function is 
//	called when the end of a node is reached. This is a simple way of determining that there are no more elements in the
//	current node that is a lot more difficult to do with the DOM parser.
//
//	Note how the List to be filled with function blocks is passed in via the constructor when the handler is created:
//
//	SAXParser handler = new SAXParser(fbs);
//
//	The SAX parser handler extends the DefaultHandler class, providing the following callback functions:
//
//	startElement()    This event is triggered when a start tag is encountered.
//
//	endElement()      This event is triggered when an end tag is encountered.
//
//	characters()      This event is triggered when text data is encountered in elements such as 
//                    <tag> data </tag>
//
class SAXParser extends DefaultHandler {
	List<FunctionBlock> fbs;
	List<FunctionBlockConnection> fbc;
	String applicationPath = "";
	FBTypeDef fbTypeDef = new FBTypeDef();
	ErrorHandler errorHandler = new ErrorHandler();
	
	String nodeName = "";
	String parentNode = "";	
	String fbName = "";
	String currentNode = "";
	public final int NOT_FOUND = -1;
	
	// 
	// Constructor
	// ===========
	// Note how the List to be filled with function blocks is passed in by reference via the constructor when the 
	// handler is created:
	//
	//     SAXParser handler = new SAXParser(fbs);
	//
	SAXParser(String applicationPath, List<FunctionBlock> fbs, List<FunctionBlockConnection> fbc, ErrorHandler errorHandler) {
		this.applicationPath = applicationPath;
		this.fbs = fbs;
		this.fbc = fbc;
		this.errorHandler = errorHandler;
	} 
	
	//
	// startElement()
	// ==============
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		int ptr = 0;
		String value = "";
		
		int ptrfb = 0;
		int ptrParameter = 0;
		String fbType = "";
		String fbComment = "";
		String parameterName = "";
		String parameterValue = "";
		String applicationName = "";
		
		//System.err.println("startElement: " + localName + " " + qName + " currentNode = " + currentNode);

		switch (qName) {
		case "System":
			// Primary system characteristics.
			applicationName = attributes.getValue("Name");
			break;
		
		case "FB":
			// Function block definition.
			currentNode = "FB";
			fbName = attributes.getValue("Name");
			//fbType = attributes.getValue("Type");
			//fbComment = attributes.getValue("Comment");
			
			ptrfb = findfb(fbName);
			if (ptrfb == NOT_FOUND) {
				// This is a new function block we have not encountered before.
				FunctionBlock fb = new FunctionBlock();				
				fb.Name(fbName);
				fb.Type(attributes.getValue("Type"));
				fb.Comment(attributes.getValue("Comment"));
				
				// Load and extract information from the function block
				if (fbTypeDef.load(applicationPath, fb, errorHandler) == XMLErrorCodes.LOADED) {
					fb.HasTypeDef(true);
					fbs.add(fb);
				//	System.out.println(fbs.size() + " " + fb.Name() + " " + fb.eventCount());
				} else {
					errorHandler.addDescription("Could not load function block type definition '" + fbName + "'");
				}
			}
			break;
			
		case "Parameter":
			switch (currentNode) {
			case "FB":
				parameterName = attributes.getValue("Name");
				ptrfb = findfb(fbName);
				if (ptrfb == NOT_FOUND) {
					// Unrecognised function block <RA_BRD Return this sort of error properly.
					errorHandler.addDescription("Unrecognised function block " + fbName + " found while processing Parameter " + parameterName);
				} else {	
					ptrParameter = fbs.get(ptrfb).findParameter(parameterName);
					if (ptrParameter == NOT_FOUND) {
						fbs.get(ptrfb).addParameter(parameterName, attributes.getValue("Value"));
					}					
				}
				break;
			}
			break;
		
		case "EventConnections":
			currentNode = "EventConnections";
			break;
			
		case "DataConnections":	
			currentNode = "DataConnections";
			break;
			
		case "Connection":	
			switch (currentNode) {
			case "EventConnections":
			case "DataConnections":	
				FunctionBlockConnection fbconnection = new FunctionBlockConnection();
				fbconnection.Comment(attributes.getValue("Comment"));
				
				value = attributes.getValue("Source");
				ptr = value.indexOf(".");
				if (ptr > 0) {
					fbconnection.SourceFB(value.substring(0, ptr));
					fbconnection.SourceName(value.substring(ptr + 1));
				}
				
				value = attributes.getValue("Destination");
				ptr = value.indexOf(".");
				if (ptr > 0) {
					fbconnection.DestinationFB(value.substring(0, ptr));
					fbconnection.DestinationName(value.substring(ptr + 1));
				}
				fbconnection.Enabled(true);
				
				//System.out.println(fbconnection.SourceFB() + " " +
				//		           fbconnection.SourceName() + " ---> " + fbconnection.DestinationFB() + " " +
				//		           fbconnection.DestinationName() );
				
				ptr = findConnection(fbconnection);
				if (ptr == NOT_FOUND) {
					fbc.add(fbconnection);
				}				
				break;
			}
			break;
		}
	}
		
	//
	// endElement()
	// ============
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		//System.err.println("endElement: " + qName);
		
		switch (qName) {
		case "FB":
			// End of the current function block definition.
			currentNode = "";
			fbName = "";
			break;
			
		case "EventConnections":
		case "DataConnections":	
			currentNode = "";
			break;
		}
	}	
	
	//
	// characters()
	// ============
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
     //  System.err.println("characters: " + String.copyValueOf(ch, start, length).trim());
 }
	
	
	//
	// findfb()
	// ========
	// Returns a pointer to a named instance of a function block in the current list.
	//
	// fbName   Unique instance name of the function block in
	//          this application.
	//
	// returns  Integer pointer to the function block in the
	//          list or -1 if it does not exist.
	//
	public int findfb(String fbName) {
		int ptrfb = NOT_FOUND;
		int fbCount = fbs.size();
		String name = "";
		
		if (fbCount > 0 ) {
			for (int pos = 0 ; pos < fbCount;  pos++) {
				name = fbs.get(pos).Name();
				if (name.compareTo(fbName) == 0) {
					ptrfb = pos;
					break;
				}
			}	
		}	
		return ptrfb;
	}
	
	//
	// findConnection()
	// ================
	// Returns a pointer to a connection in the current list.
	//
	// fbconnection    	A function block connection object with its
	//					parameters set. Connections do not have a 
	//					unique ID, so all parameters need to be matched
	//					up to determine if it exists in the list. 
	//
	// returns			Integer pointer to the connection in the list
	//              	of -1 if it does not exist. 
	//
	public int findConnection(FunctionBlockConnection fbconnection) {
		int ptr = NOT_FOUND;
		int connectionCount = fbc.size();
		
		if (connectionCount > 0) {
			for (int pos = 0; pos <connectionCount; pos++) {
				if(fbconnection.SourceFB().equals(fbc.get(pos).SourceFB())) {
					if(fbconnection.SourceName().equals(fbc.get(pos).SourceName())) {
						if(fbconnection.DestinationFB().equals(fbc.get(pos).DestinationFB())) {
							if(fbconnection.DestinationName().equals(fbc.get(pos).DestinationName())) {
								ptr = pos;
								break;
							}
						}
					}
				}
			}
		}
		return ptr;
	}
}

//
// FBTypeDef
// =========
// Loads and parses a function block type definition file with the extension .fbt to extract
// the properties of the function block. 
//
class FBTypeDef {
	String lastErrorDescription = "";
	String applicationPath = "";
	String functionBlockTypeFileName = "";
	
	//
	// load()
	// ======
	// Loads and parses the named function block application .sys file.
	//
	// libraryPath		      		The fully-qualified path to the directory where the type definition
	//								is stored.
	// 
	// FunctionBlockTypeFileName 	The name of the file that holds the XML-format <function block 
	//								type name>.fbt function block type definition file. This is a
	//								proprietary XML format based on the IEC 61499 Function Block standard.
	//
	// returns           			Returns an enumerated type from the XMLErrorCodes list that reports 
	//							    if the file could be loaded and parsed successfully. If an error code
	//								is returned getLastErrorDescription() returns information about what 
	//								went wrong.
	//
	public int load(String applicationPath, FunctionBlock fb, ErrorHandler errorHandler) {
		int loadStatus = XMLErrorCodes.UNDEFINED;
		String functionBlockType = fb.Type();
		fb.HasTypeDef(false);

		if (applicationPath == "") {
			loadStatus = XMLErrorCodes.UNDEFINED_FILE_PATH;		
			errorHandler.addDescription("Path to the function block type definition file has not been specified.");			
	
		} else if (functionBlockType == "") {
			loadStatus = XMLErrorCodes.UNDEFINED_FILE_NAME;		
			errorHandler.addDescription("Function block type name has not been specified.");
		} else {
			this.applicationPath = applicationPath;
			this.functionBlockTypeFileName = functionBlockType + ".fbt";
			try {
				// The directory path object used to access the XML file that contains the application.
				File fbTypeDef = new File(applicationPath + "/" + functionBlockTypeFileName);  
				
				// Create and configure a SAX XML parser 
				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				// The dtd for this file is not available so ensure that the parser does
				// not return an error.
				parserFactory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
		        javax.xml.parsers.SAXParser parser = parserFactory.newSAXParser();
		        SAXFBParser handler = new SAXFBParser(fb, errorHandler);
		                
		        // Pass the function block type definition application file handle to the 
		        // SAX parser handler and start it parsing. It will call its handler functions
		        // to give us access to the data each time it has unpacked an element, returning back here
		        // when it finishes.
		        parser.parse(fbTypeDef, handler);
		        fb.HasTypeDef(true);
		        loadStatus = XMLErrorCodes.LOADED;
		      
			} catch (Exception e) {
				loadStatus = XMLErrorCodes.UNEXPECTED_ERROR;
				errorHandler.addDescription(e.getMessage());
			}
		}	
		return loadStatus;
	}
}

//
// SAXFBParser()
// =============
class SAXFBParser extends DefaultHandler {
	String currentNode = "";
	String currentEventName = "";
	FunctionBlock fb = new FunctionBlock();
	ErrorHandler errorHandler = new ErrorHandler();	
	// 
	// Constructor
	// ===========
	// Note how the function block that is being extended with new data is passed in
	// via the constructor when the handler is created.
	//
	//     SAXParser handler = new SAXParser(fb);
	//
	SAXFBParser(FunctionBlock fb, ErrorHandler errorHandler){
		this.fb = fb;
		this.errorHandler = errorHandler;
	} 

	//
	// startElement()
	// ==============
	// This method is called when entering a nested group of XML elements within a 
	// structure such as:
	//
	//  <node> ------------------> startElement() invoked.
	//      <tag> data </tag> ---> startElement() invoked.
	//  </node>   
	//
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		int ptr = 0;
		int ptrEvent = 0;
		String eventName = "";
		String WithVarName = "";
		
		//System.err.println("FBTypeDefn startElement: " + localName + " " + qName);
		switch (currentNode) {
		case "EventInputs":
			switch (qName) {
			case "Event":
				//System.err.println("Event" + localName + " " + qName);
				eventName = attributes.getValue("Name");
				currentEventName = eventName;
				ptr = fb.findEvent(eventName);
				if (ptr == NOT_FOUND) {
					fb.addEvent(eventName, EventTypes.EVENT_INPUT);
				}
				break;
				

			case "With":
				WithVarName = attributes.getValue("Var");
				ptrEvent = fb.findEvent(currentEventName);
				if (ptrEvent == NOT_FOUND) {
					// <RA_BRD need to report this somehow.
				} else {
					FunctionBlockEvent fbEvent = fb.Event(ptrEvent);
					//System.out.println(fbEvent.EventName);
					fbEvent.addWithVar(WithVarName);
				}	
				break;
			}
			break;
			
		case "EventOutputs":
			switch (qName) {
			case "Event":
				//System.err.println("Event" + localName + " " + qName);
				eventName = attributes.getValue("Name");
				currentEventName = eventName;
				ptr = fb.findEvent(eventName);
				if (ptr == NOT_FOUND) {
					fb.addEvent(eventName, EventTypes.EVENT_OUTPUT);
					//System.out.println(fb.eventCount());
				}
				break;				
				
			case "With":
				WithVarName = attributes.getValue("Var");
				ptrEvent = fb.findEvent(currentEventName);
				if (ptrEvent == NOT_FOUND) {
					// <RA_BRD need to report this somehow.
				} else {
					FunctionBlockEvent fbEvent = fb.Event(ptrEvent);
					//System.out.println(fbEvent.EventName);
					fbEvent.addWithVar(WithVarName);
				}	
				break;
			}
			break;
			
		case "InputVars":
			switch (qName) {
			case "VarDeclaration":
				FunctionBlockVariable fbVariable = new FunctionBlockVariable();
				VarTypes varType = VarTypes.VAR_INPUT;
				String varName = attributes.getValue("Name");
				int dataType = fbVariable.DataTypeFromString(attributes.getValue("Type"));
				String comment = attributes.getValue("Comment");
				String initialValue = attributes.getValue("InitialValue");
				fb.addVar(varName, varType, dataType, comment, initialValue);
				break;
			}
			break;
			
		case "OutputVars":
			switch (qName) {
			case "VarDeclaration":
				FunctionBlockVariable fbVariable = new FunctionBlockVariable();
				VarTypes varType = VarTypes.VAR_OUTPUT;
				String varName = attributes.getValue("Name");
				int dataType = fbVariable.DataTypeFromString(attributes.getValue("Type"));
				String comment = attributes.getValue("Comment");
				String initialValue = attributes.getValue("InitialValue");
				fb.addVar(varName, varType, dataType, comment, initialValue);
				break;
			}
			break;
			
		default:	
			switch (qName) {
			case "EventInputs":
			case "EventOutputs":
			case "InputVars":
			case "OutputVars":
				currentNode = qName;				
				break;
			}
		}	
	}
	
	//
	// endElement()
	// ============
	// This method is called when exiting from a nested group of XML elements within a 
	// structure such as:
	//
	//  <node>
	//      <tag> data </tag>
	//  </node>   -------------> endElement() invoked.
	//
	// In this class, it is most often used to clean up cached values when leaving a section
	// of the document.
	//
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		// System.err.println("FBTypeDefn endElement: " + qName);
		
		switch (qName) {
		case "EventInputs":
		case "EventOutputs":
		case "InputVars":
		case "OutputVars":
			currentNode = "";
			currentEventName = "";
			break;
		}
	}	
	
	//
	// characters()
	// ============
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
       //System.err.println("FBTypeDefn characters: " + String.copyValueOf(ch, start, length).trim());
 }
}

//
// ErrorHandler
// ============
// Allows deeply-nested handler to log an error that is able to be
// passed up to higher-level processes.
//
//class ErrorHandler {
//	
//	String lastErrorDescription = "";
//	
//	//
//	// addErrorDescription()
//	// =====================
//	//
//	public void addErrorDescription(String errorDescription) {
//		if (lastErrorDescription != "") {
//			lastErrorDescription = lastErrorDescription + "\n" + errorDescription;
//		} else {
//			lastErrorDescription = errorDescription;
//		}
//	}
//	
//	// set lastErrorDescription()
//	// ==========================
//	//
//	public void lastErrorDescription(String errorDescription) {
//		lastErrorDescription = errorDescription;
//	}
//	
//	//
//	// get lastErrorDescription()
//	// ==========================
//	public String lastErrorDescription() {
//		return lastErrorDescription;
//	}
//}

