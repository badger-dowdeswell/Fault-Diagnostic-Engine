//
// DIAGNOSTIC AGENT CAPABILITIES
// =============================
// This class is a repository of domain-specific methods that provide
// capabilities for an agent. These skills include the ability to rewire
// an application, create FORTE boot files and other abilities that allow
// the agent to work with a function block applications.
//
// AUT University - 2019
//
// Revision History
// ================
// 24.10.2019 BRD Original version
//
package fde;
import static fde.Constants.NOT_FOUND;

public class DiagnosticAgentCapabilities {
	int monitorInstanceCount = 0;	// Instance counter for the sender diagnostic function blocks 
									// added while rewiring the application.
	int triggerInstanceCount = 0;   // Instance counter for the trigger diagnostic function blocks
									// added while rewiring the application.
	String lastErrorDescription;	// Description of the last error that occurred in this class.
	
	//
	// rewireApp()
	// ===========
	// Rewires to function block application to insert diagnostic function blocks.	
	//
	// server	The agent's TCP/IP server that will be used to send and receive network 
	//          messages.
	//
	// fbapp    The list of function blocks and their properties that define this function
	//	        block application. This is a structure of the type FunctionBlockApp<> which
	//          contains details of individual function blocks, all their properties and
	//		    a complete list of connections.	
	//
	// returns  One of the fbAppCodes status codes to report if the rewiring was successful.
	//
	public fbAppCodes rewireApp(NIOserver server, FunctionBlockApp fbapp) {
		// <RA_BRD Temporary specification of which function blocks to monitor.
		//         We need to generalise this later...
		//
		ErrorHandler errorHandler = new ErrorHandler();
		monitorInstanceCount = 0;
		triggerInstanceCount = 0;
		fbAppCodes status = fbAppCodes.UNDEFINED;
		
	//	status = createMonitor("T_SENSOR_01", "TEMP", server, fbapp, errorHandler);
	//	if (status != fbAppCodes.REWIRED) {
	//		System.out.println("Rewire error " + lastErrorDescription);
	//	}
		
		status = createTrigger("F_TO_C_CONV", "IN", server, fbapp, errorHandler); 
		if (status != fbAppCodes.REWIRED) {
			System.out.println("Rewire error " + lastErrorDescription);
		} else {
			status = createMonitor("F_TO_C_CONV", "OUT", server, fbapp, errorHandler);
			if (status != fbAppCodes.REWIRED) {
				System.out.println("Rewire error " + lastErrorDescription);
			}	
		}
		
		fbapp.displayFunctionBlocks(fbapp);
		createForteBootfile(fbapp);
		
		return status;
	}
	
	//
	// createMonitor()
	// ===============
	// Rewires the function block application to add in a new AGENT_SEND TCP/IP monitor function
	// block and configure its parameters.
	//
	// blockToMonitor   Instance name of the function block to be monitored.  
	//
	// outputToMonitor  The name of the data output to monitor. The function determines which event
	//                  output is triggered to signal that valid data is available. 
	//
	// fbapp    		The list of function blocks and their properties that define this function
	//	        		block application. This is a structure of the type FunctionBlockApp<> which
	//          		contains details of individual function blocks, all their properties and
	//		    		a complete list of connections.	
	//
	// server			The agent's TCP/IP server that will receive network messages from this 
	//					AGENT_SEND function block.
	//
	// errorHandler	    Used to pass errors up through the handler event chain back to the function
	//					caller.
	//
	// returns 		 	One of the FunctionBlockAppCodes to indicate the status of the rewiring
	//         			command.
	//
	private fbAppCodes createMonitor(String blockToMonitor, String outputToMonitor, NIOserver server, FunctionBlockApp fbapp, ErrorHandler errorHandler) {
		fbAppCodes status = fbAppCodes.UNDEFINED;	
		String eventName = "";
		int ptrWith = 0;
		String inputPort = "";
		int dataType = DataTypes.DATA_INT;
		FBTypeDef fbTypeDef = new FBTypeDef();
		
		FunctionBlock fb = new FunctionBlock();
		FunctionBlock fbmonitor = new FunctionBlock();
		FunctionBlockEvent fbEvent = new FunctionBlockEvent();
		FunctionBlockVariable fbVar = new FunctionBlockVariable();
				
		if (blockToMonitor == "") {
			status = fbAppCodes.INVALID_NAME;
			lastErrorDescription = "Function block name to monitor was not specified.";
		} else if (outputToMonitor == "") {
			status = fbAppCodes.INVALID_PORT_NAME;
			lastErrorDescription = "Function block output to monitor was not specified.";
		} else {	
			fb = fbapp.findfb(blockToMonitor);
			if (fb.Name() != "") {
				// Locate the event that is linked to this data output.
				for (int ptrEvent = 0; ptrEvent < fb.eventCount(); ptrEvent++) {
					fbEvent = fb.Event(ptrEvent);
					// Only output data types and events can be monitored.
					if (fbEvent.EventType == EventTypes.EVENT_OUTPUT) {
						ptrWith = fbEvent.findWithVar(outputToMonitor);
						if (ptrWith != NOT_FOUND) {
							eventName = fbEvent.EventName;
							break;
						}	
					}
				}
								
				if (eventName != "") {
					// This output can be monitored. Link a new monitor function block into the application.
					System.out.println("Monitor " + fb.Name() + " " + outputToMonitor + " " + eventName);
					System.out.println("");
					
					monitorInstanceCount++;
					fbmonitor.Name("AGENT_SEND_" + monitorInstanceCount);
					fbmonitor.Type("AGENT_SEND");
					
					// Note how the server parameters are retrieved from the current TCP/IP server instance.
					fbmonitor.addParameter("ADDRESS", server.hostName()); 
					fbmonitor.addParameter("PORT", String.valueOf(server.listenerPortNumber())); 
					fbmonitor.addParameter("INST_ID", String.valueOf(monitorInstanceCount));
					
					// Determine the data type of the output being monitored.
					fbVar = fb.Var(outputToMonitor);
					dataType = fbVar.DataType();
					inputPort = "DATA_" + fbVar.StringFromDataType(dataType);
					
					fbmonitor.addParameter("DATA_TYPE", String.valueOf(dataType));
					
					fbapp.addConnection(fb.Name(), outputToMonitor, "AGENT_SEND_" + monitorInstanceCount, inputPort, true);
					fbapp.addConnection(fb.Name(), eventName, "AGENT_SEND_" + monitorInstanceCount, "REQ", true);
					
					fbTypeDef.load(fbapp.applicationPath(), fbmonitor, errorHandler);
					fbapp.add(fbmonitor);
					status = fbAppCodes.REWIRED;
					
				} else {
					status = fbAppCodes.EVENT_UNDEFINED;
					errorHandler.lastErrorDescription("Cannot monitor " + blockToMonitor + ". Cannot find linked event for output " + outputToMonitor);
				}

			} else {	
				status = fbAppCodes.FB_NOT_FOUND;
				errorHandler.lastErrorDescription("Function block '" + blockToMonitor + "' not found in application.");
			}
		}
		return status;
	}
	
	// 
	// createTrigger()
	// ===============
	// Rewires the function block application to add in a new AGENT_SEND TCP/IP monitor function
	// block and configure its parameters.
	//
	// blockToTrigger 	Instance name of the function block whose input will be triggered by supplying
	//					it with an event and data.  
	//
	// inputToTrigger   The name of the data input to supply data to. The function determines which event
	//                  output is triggered to signal that valid data is available. 
	//
	// fbapp    		The list of function blocks and their properties that define this function
	//	        		block application. This is a structure of the type FunctionBlockApp<> which
	//          		contains details of individual function blocks, all their properties and
	//		    		a complete list of connections.	
	//
	// server			The agent's TCP/IP server that will receive network messages from this 
	//					AGENT_SEND function block. <RA_NRD
	//
	// errorHandler	    Used to pass errors up through the handler event chain back to the function
	//					caller.
	//
	// returns 		 	One of the FunctionBlockAppCodes to indicate the status of the rewiring
	//         			command.
	//
	//                  createTrigger("F_TO_C_CONV", "IN", server, fbapp, errorHandler); 
	//
	private fbAppCodes createTrigger(String blockToTrigger, String inputToTrigger, NIOserver server, FunctionBlockApp fbapp, ErrorHandler errorHandler) {
		fbAppCodes status = fbAppCodes.UNDEFINED;
		
		String eventName = "";
		int ptrWith = 0;
		String outputPort = "";
		int dataType = DataTypes.DATA_INT;
		FBTypeDef fbTypeDef = new FBTypeDef();
		
		FunctionBlock fb = new FunctionBlock();
		FunctionBlock fbtrigger = new FunctionBlock();
		FunctionBlockEvent fbEvent = new FunctionBlockEvent();
		FunctionBlockVariable fbVar = new FunctionBlockVariable();
		FunctionBlockConnection fbconn = new FunctionBlockConnection();
				
		if (blockToTrigger == "") {
			status = fbAppCodes.INVALID_NAME;
			lastErrorDescription = "Name of function block to trigger was not specified.";
		} else if (inputToTrigger == "") {
			status = fbAppCodes.INVALID_PORT_NAME;
			lastErrorDescription = "Function block input to trigger was not specified.";
		} else {	
			fb = fbapp.findfb(blockToTrigger);
			if (fb.Name() != "") {
				// Locate the event that is linked to this data input.
				for (int ptrEvent = 0; ptrEvent < fb.eventCount(); ptrEvent++) {
					fbEvent = fb.Event(ptrEvent);
					// Only input data types and events can be triggered.
					if (fbEvent.EventType == EventTypes.EVENT_INPUT) {
						ptrWith = fbEvent.findWithVar(inputToTrigger);
						if (ptrWith != NOT_FOUND) {
							eventName = fbEvent.EventName;
							break;
						}	
					}
				}
				
				if (eventName != "") {
					// This input can be triggered. Is it connected to anything else that we need
					// to disconnect?
					
					for(int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
						fbconn = fbapp.Connection(ptrConnection);
						System.out.println(fbconn.SourceFB() + " " + fbconn.SourceName() 
										   + " ---> " + 
										   fbconn.DestinationFB() + " " + fbconn.DestinationName() );
						if (fbconn.DestinationFB().equals(blockToTrigger)) {
							if (fbconn.DestinationName().equals(inputToTrigger)) {
								//System.out.println("Found connection");
								fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
							} else if (fbconn.DestinationName().equals(eventName)) {
								//System.out.println("Found event");
								fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
							}
						}
					}
					
					// Link a new trigger function block into the application.
					System.out.println("Trigger " + fb.Name() + " " + inputToTrigger + " " + eventName);
					System.out.println("");
					
					triggerInstanceCount++;
					fbtrigger.Name("AGENT_RECV_" + triggerInstanceCount);
					fbtrigger.Type("AGENT_RECV");
					
	
					// Note how the server parameters are retrieved from the current TCP/IP server instance.
					fbtrigger.addParameter("ADDRESS", "127.0.0.1"); //server.hostName()); 
					fbtrigger.addParameter("PORT", "62501"); //String.valueOf(server.listenerPortNumber())); 
		
					// Determine the data type of the input being triggered.
					fbVar = fb.Var(inputToTrigger);
					dataType = fbVar.DataType();
					outputPort = "DATA_" + fbVar.StringFromDataType(dataType);
		
					fbtrigger.addParameter("DATA_TYPE", String.valueOf(dataType));
		
					fbapp.addConnection("AGENT_RECV_" + triggerInstanceCount, outputPort, fb.Name(), inputToTrigger, true);
					fbapp.addConnection("AGENT_RECV_" + triggerInstanceCount, "REQO", fb.Name(), eventName, true);
					
					fbapp.addConnection("START", "COLD", fbtrigger.Name(), "INIT", true);
					fbapp.addConnection("START", "WARM",  fbtrigger.Name(), "INIT", true);
		
		
					fbTypeDef.load(fbapp.applicationPath(), fbtrigger, errorHandler);
					fbapp.add(fbtrigger);
					status = fbAppCodes.REWIRED;
					
				} else {
					status = fbAppCodes.EVENT_UNDEFINED;
					errorHandler.lastErrorDescription("Cannot trigger " + blockToTrigger + ". Cannot find linked event for input " + inputToTrigger);
				}

			} else {	
				status = fbAppCodes.FB_NOT_FOUND;
				errorHandler.lastErrorDescription("Function block '" + blockToTrigger + "' not found in application.");
			}
		}
		return status;
	}
	
	// 
	// createForteBootfile()
	// =====================
	// Generates a FORTE-compliant forte.fboot file that will instantiate the function block
	// application specified by the function block application definition passed in.
	//
	// fbapp    The list of function blocks and their properties that define this function
	//	        block application. This is a structure of the type FunctionBlockApp<> which
	//          contains details of individual function blocks, all their properties and
	//		    a complete list of connections.
	//
	// returns  One of the FunctionBlockAppCodes to indicate the status of the boot file creation.
	//
	public fbAppCodes createForteBootfile(FunctionBlockApp fbapp) {
		fbAppCodes status = fbAppCodes.UNDEFINED;	

		int requestID = 1;
		
		FunctionBlock fb = new FunctionBlock();
		FunctionBlockEvent fbevent = new FunctionBlockEvent();
		FunctionBlockParameter fbparameter = new FunctionBlockParameter();
		FunctionBlockConnection fbconnection = new FunctionBlockConnection();
		
		String FORTE_PATH = System.getProperty("user.home") + "/4diac-ide/forte/src/";
		FileIOstatus IOstatus = FileIOstatus.UNDEFINED;
		FileIO bootfile = new FileIO();
		
		IOstatus = bootfile.createFile(FORTE_PATH, "forte.fboot");
		if (IOstatus == FileIOstatus.FILE_CREATED) {
			bootfile.write(";<Request ID=\"" + requestID++ + "\" Action=\"CREATE\"><FB Name=\"EMB_RES\" Type=\"EMB_RES\" /></Request>\n");
			
			for (int ptr = 0; ptr < fbapp.fbCount(); ptr++) {
				fb = fbapp.getfb(ptr);
				
				if (fb.Name() != "START") {
					bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"CREATE\">" +
					      "<FB Name=\"" + fb.Name() + "\" Type=\"" + fb.Type() + "\" /></Request>\n");
					
					// Configure the parameters for this function block. 
					for (int ptrParameter = 0; ptrParameter < fb.ParameterCount(); ptrParameter++) {
						fbparameter = fb.Parameter(ptrParameter);
						
						bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"WRITE\">" +
								       "<Connection Source=\"" + fbparameter.Value() + "\" " + 
								       "Destination=\"" + fb.Name() + "." + fbparameter.Name() + "\" /></Request>\n");
					}
				}	
			}	
			
			// Create the connections between the function blocks.
			for (int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
				fbconnection = fbapp.Connection(ptrConnection);
				if (fbconnection.Enabled()) {
					bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"CREATE\">" +
									"<Connection Source=\"" + fbconnection.SourceFB() + "." + fbconnection.SourceName() + "\" " + 
									"Destination=\"" + fbconnection.DestinationFB() + "." + fbconnection.DestinationName()  + "\"" + "/></Request>\n");
				}	
			}
			
			// Add the application start command. 
		//	bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"START\"/>\n");
			bootfile.close();
		} else {
			System.err.println("Could not create forte.fboot." + bootfile.errorDescription());
		}
		
		return status;
	}
	
	//
	// LastErrorDescription()
	// ======================
	// Returns a text description of the last error that has occurred in a method within
	// this class.
	//
	public String LastErrorDescription() {
		return lastErrorDescription; 
	}
}
