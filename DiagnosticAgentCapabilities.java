//
// DIAGNOSTIC AGENT CAPABILITIES
// =============================
// This class is a repository of domain-specific methods that provide skills  
// or capabilities for an agent. These skills include the ability to rewire
// an application, create a test harness, create FORTE boot files and other 
// abilities that allow the agent to work with a function block applications.
//
// AUT University - 2019-2021
//
// Revision History
// ================
// 24.10.2019 BRD Original version
// 14.07.2020 BRD Extended the rewire() functionality to work with diagnostic packages.
// 21.07.2020 BRD Building createHarness() that inserts the required diagnostic points.
// 04.03.2021 BRD Extending the re-wiring functionality to deal with new fault-finding 
//                functions that capture more event information. The referencing of
//                a diagnostic point now centers around the event it is watching, not
//                the data input or output port that is being captured.
//
package fde;
import static fde.Constants.NOT_FOUND;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

public class DiagnosticAgentCapabilities {
	int monitorInstanceCount = 0;	  // Instance counter for the sender diagnostic function blocks 
									  // added while rewiring the application.
	int triggerInstanceCount = 0;     // Instance counter for the trigger diagnostic function blocks
									  // added while rewiring the application.
	String lastErrorDescription;	  // Description of the last error that occurred in this class.
	
	private ErrorHandler errorHandler = new ErrorHandler();
	
	private boolean isSilent = false; // Used to turn off and on console messages during development.
	
	//
	// createHarness()
	// ===============
	// Iterates the function block application loaded previously and searches for 
	// diagnostic packages for each function block. A set of diagnostic points
	// are then created that give access to inputs, outputs and events during
	// runtime. This updates the agents belief structure about the application.
	//
	// fbapp            The current system-under-diagnosis belief structure for 
	//                  the function block application.
	//
	// dps              The list of the diagnostic points for this function block
	//                  application. 
	//
	// applicationPath  Diagnostic package (.dpg) files are located in the same folder 
	//                  as the function block type definition (.fbt) files.
	//
	// server           The non-blocking I/O server that these diagnostic points use to
	//                  communicate with the agents.
	//
	public boolean createHarness(FunctionBlockApp fbapp, DiagnosticPoints dps, String applicationPath, NIOserver server) {
		boolean status = true;
		boolean foundConnection = false;
		
		int pollTime = 100; // polling time in milliseconds.
		
		FunctionBlock fb = new FunctionBlock();
		FunctionBlockConnection fbconn = new FunctionBlockConnection();
				
		Diagnostics diag = new Diagnostics();
		String diagPak = "";
		int loadStatus = XMLErrorCodes.UNDEFINED;
		int SIFBinstanceID = 0;
				
		errorHandler.clear();
		if (fbapp.fbCount() == 0) {
			errorHandler.addDescription("There are no function blocks in that application");
		} else {
			// This first pass scans all the function blocks that have a diagnostic package. Note
			// the validation that is performed to make sure that the information gleaned from
			// each package is correct. The package may be out-of-date with the function block if
			// the designer has not kept it in-sync with changes.
			for (int ptr = 0; ptr < fbapp.fbCount(); ptr++) {
				fb = fbapp.getfb(ptr);
				say(fb.Name());				 
				if (fb.Name() != "START") {
					// This next section determines if this function block connected to anything. If not, 
					// the function block is an orphan that cannot do anything: do not create a diagnostic 
					// point.
					foundConnection = false;
					for (int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
						fbconn = fbapp.Connection(ptrConnection);
						//say("--> " + fbconn.DestinationFB() + " " + fbconn.SourceFB());
						if ( (fbconn.DestinationFB().equals(fb.Name())) || (fbconn.SourceFB().equals(fb.Name())))  {
							// Yes, the function block is connected to something.
							diagPak = applicationPath + fb.Type() + ".dpg";
							loadStatus = diag.loadDiagnostics(diagPak);
							switch (loadStatus) {
							case XMLErrorCodes.LOADED:
								// A diagnostic package for this function block has been found.
								say("Found diagnostic package for " + fb.Name() + " [" + fb.Type() + "] "+ diagPak);
								for (int dpptr = 0; dpptr < diag.countDP(); dpptr++) {
									say("--> |" + diag.Event(dpptr) + "|" + diag.Port(dpptr) + "|");
									// Create the diagnostic point instance and add it into the list of
									// diagnostic points.
									SIFBinstanceID++;
									DiagnosticPoint dp = new DiagnosticPoint();
									dp.fbName = fb.Name();
									dp.fbEventName = diag.Event(dpptr);
									dp.fbPortName = diag.Port(dpptr);
									dp.SIFBinstanceID = SIFBinstanceID;
									dp.fbapp = fbapp;
									dp.server = server;	
									dps.add(dp);
									say("dps size " + dps.count());
								}
								break;
								
							case XMLErrorCodes.NOT_FOUND:
								// No diagnostic package was found for this function block; that's fine.
								break;
								
							default:
								say("Error while loading diagnostic package: " + diag.lastErrorDescription());
								errorHandler.addDescription(diag.lastErrorDescription());
								status = false;
								break;
							}
							break;
						}
					}
				}
			}
			
			if ((dps.count() > 0) && (status)) {
				// There is at least one diagnostic point to create. This next pass rewires the application to 
				// insert and wire-up the individual diagnostic points.
				DiagnosticPoint dp = new DiagnosticPoint();
				fbAppCodes dpStatus = fbAppCodes.UNDEFINED;	
				
				say("\nRewiring...");
				for (int dpptr = 0; dpptr < dps.count(); dpptr++) {
					dp = dps.get(dpptr);
					dpStatus = createDP(dp, fbapp, pollTime, server, errorHandler);
					if (dpStatus != fbAppCodes.REWIRED) {
						status = false;
						say("Problem");
					}
				}
			}
			
			if (status) {
				// RA_BRD note change to specify where the forte.fboot file is to be created.
				if (!createForteBootfile(fbapp, applicationPath + "/src/", errorHandler)) {
					say("Could not create a diagnostic harness in forte.boot. " + errorHandler.Description());
					status = false;
				}	
			}
		}
		return status;
	}
	
	//
	// createDP()
	// ==========
	// Rewires the function block application to insert a new DP function block instance and
	// configure its parameters.
	//
	// dp               Instance of the diagnostic point that is being created. This contains
	//                  most of the information about the diagnostic block needed to wire it
	//                  in.
	//
	// fbapp    		The list of function blocks and their properties that define this function
	//	        		block application. This is a structure of the type FunctionBlockApp<> which
	//          		contains details of individual function blocks, all their properties and
	//		    		a complete list of connections.	
	//
	// pollTime         The frequency of at which the diagnostic point polls the server in milliseconds.
	//                  	
	// server			The agent's TCP/IP server that will send and receive packets from the 
	//					AGENT_GATE function block instance.
	//
	// errorHandler	    Used to pass errors up through the handler event chain back to the function
	//					caller.
	//
	// returns 		 	One of the FunctionBlockAppCodes to indicate the status of the rewiring
	//         			command.	
	//
	private fbAppCodes createDP(DiagnosticPoint dp, FunctionBlockApp fbapp, int pollTime, NIOserver server, ErrorHandler errorHandler) {
		fbAppCodes status = fbAppCodes.UNDEFINED;
		String eventName = "";
		int ptrWith = 0;
		int ptrVar = 0;
		int ptrEvent = 0;
		String inputPort = "";
		String outputPort = "";
		int dataType = DataTypes.DATATYPE_INT;
		FBTypeDef fbTypeDef = new FBTypeDef();
		
		String sourceFB = "";
		String sourceName = "";
		String sourceEvent = "";
		String destinationFB = "";
		String destinationName = "";
		String destinationEvent = "";
		
		FunctionBlock fb = new FunctionBlock();
		FunctionBlock fbdp = new FunctionBlock();
		FunctionBlockEvent fbEvent = new FunctionBlockEvent();
		FunctionBlockVariable fbVar = new FunctionBlockVariable();
		FunctionBlockConnection fbconn = new FunctionBlockConnection();
		
		boolean foundConnection = false;
		
		say("\nCreating diagnostic point " + "DP_"+ dp.SIFBinstanceID() + " " + dp.fbName() 
		    + " " + dp.fbEventName() + " " + dp.fbPortName() + " " + dp.fbPortName.length());
		
		fb = fbapp.findfb(dp.fbName());
		if (fb.Name() != "") {
			if (dp.fbPortName.length() > 0) {
				// Locate the data port on this function block that this diagnostic point
				// has been assigned to monitor.
				ptrVar = fb.findVar(dp.fbPortName);
				if (ptrVar != NOT_FOUND) {
					fbVar = fb.Var(ptrVar);
					switch (fbVar.VarType()) {
					case VAR_INPUT:
						// Locate the event that is used to trigger this data input.
						ptrWith = NOT_FOUND;
						for (ptrEvent = 0; ptrEvent < fb.eventCount(); ptrEvent++) {
							fbEvent = fb.Event(ptrEvent);
							// Only input data types and events can be triggered.
							if (fbEvent.EventType == EventTypes.EVENT_INPUT) {
								ptrWith = fbEvent.findWithVar(dp.fbPortName());
								if (ptrWith != NOT_FOUND) {
									//eventName = fbEvent.EventName;
									break;
								}	
							}
						}
						if (ptrWith != NOT_FOUND) {
							say("Input port " + dp.fbPortName + " " + dp.fbEventName);
							
							fbdp.Name("DP_" + dp.SIFBinstanceID());
							fbdp.Type("DP");
							fbdp.Comment("Diagnostic point for " + fb.Name());
							
							dataType = fbVar.DataType();
							outputPort = "DATA_OUT_" + fbVar.StringFromDataType(dataType);
							fbdp.addParameter("DATA_TYPE", String.valueOf(dataType));
							
							// RA_BRD The POLL_TIME should be parameterized ... maybe in the diagnostic package?
							fbdp.addParameter("POLL_TIME", "T#"+ pollTime + "ms");  // was "T#100ms"
							fbdp.addParameter("ADDRESS", server.hostName()); 
							fbdp.addParameter("PORT", String.valueOf(server.listenerPortNumber())); 
							fbdp.addParameter("INST_ID", String.valueOf(dp.SIFBinstanceID()));
							
							// The diagnostic point is being inserted into the event and data flow. If there are
							// existing connections, break them so they can be diverted through the diagnostic point.																
							for (int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
								fbconn = fbapp.Connection(ptrConnection);
								say(fbconn.SourceFB() + " " + fbconn.SourceName() 
												   + " ---> " + 
												   fbconn.DestinationFB() + " " + fbconn.DestinationName() );
								if (fbconn.DestinationFB().equals(fb.Name())) {
									if (fbconn.DestinationName().equals(dp.fbPortName())) {
										say("Found connection to " + fbconn.SourceFB() + " " + fbconn.SourceName());
										sourceFB = fbconn.SourceFB();
										sourceName = fbconn.SourceName();
										fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
									} else if (fbconn.DestinationName().equals(dp.fbEventName)) {
										say("Found event ");
										sourceEvent = fbconn.SourceName();
										fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
									}
								}
							}
							
							// Add the new connections between the diagnostic point and the function block.
							if (sourceEvent != "") {
								fbapp.addConnection(sourceFB, sourceEvent, fbdp.Name(), "DATA_IN", true);
							} else {
								
							}
							if (sourceName != "") {
								fbapp.addConnection(sourceFB, sourceName, fbdp.Name(), "DATA_IN_" + fbVar.StringFromDataType(dataType) , true);
							}	
							
							fbapp.addConnection("DP_" + dp.SIFBinstanceID(), "DATA_OUT", fb.Name(), dp.fbEventName, true);
							fbapp.addConnection("DP_" + dp.SIFBinstanceID(), outputPort, fb.Name(), dp.fbPortName, true);
							
							fbapp.addConnection("START", "COLD", "DP_" + dp.SIFBinstanceID(), "START", true);
							fbapp.addConnection("START", "WARM", "DP_" + dp.SIFBinstanceID(), "START", true);
		
							fbapp.add(fbdp);
							status = fbAppCodes.REWIRED;
						} else {
							errorHandler.addDescription("Input port " + dp.fbPortName + " on " + dp.fbName() + " has no event assigned to trigger it.");
							status = fbAppCodes.EVENT_UNDEFINED;
						}
						break;
						
					case VAR_OUTPUT:
						// Locate the event that is used to trigger this data output. This
						// verifies that the event does exist and maps to this data port. If
						// it does not, then the diagnostic package is probably out-of-date.
						ptrWith = NOT_FOUND;
						for (ptrEvent = 0; ptrEvent < fb.eventCount(); ptrEvent++) {
							fbEvent = fb.Event(ptrEvent);
							// Only output data types and events can be triggered.
							if (fbEvent.EventType == EventTypes.EVENT_OUTPUT) {
								ptrWith = fbEvent.findWithVar(dp.fbPortName());
								if (ptrWith != NOT_FOUND) {
									//eventName = fbEvent.EventName;
									break;
								}	
							}
						}
						if (ptrWith != NOT_FOUND) {
							say("Output port " + dp.fbPortName + " " + eventName);
							fbdp.Name("DP_" + dp.SIFBinstanceID());
							fbdp.Type("DP");
							fbdp.Comment("Diagnostic point for " + fb.Name());
							
							dataType = fbVar.DataType();
							inputPort = "DATA_IN_" + fbVar.StringFromDataType(dataType);
							fbdp.addParameter("DATA_TYPE", String.valueOf(dataType));
							
							// RA_BRD The POLL_TIME should be parameterized ... maybe in the diagnostic package?
							fbdp.addParameter("POLL_TIME", "T#" + pollTime + "ms");
							fbdp.addParameter("ADDRESS", server.hostName()); 
							fbdp.addParameter("PORT", String.valueOf(server.listenerPortNumber())); 
							fbdp.addParameter("INST_ID", String.valueOf(dp.SIFBinstanceID()));	
							
							// The diagnostic point is being inserted into the event and data flow. Break the
							// previous direct connections and divert them.												
							for(int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
								fbconn = fbapp.Connection(ptrConnection);
								say(fbconn.SourceFB() + " " + fbconn.SourceName()
								    + " ---> " + 
									fbconn.DestinationFB() + " " + fbconn.DestinationName());									   
																
								if (fbconn.SourceFB().equals(fb.Name())) {
									if (fbconn.SourceName().equals(dp.fbPortName())) {
										say("Found connection to " + fbconn.SourceFB() + " " + fbconn.SourceName() + " --> " 
									        + fbconn.DestinationFB() + " " + fbconn.DestinationName());
										
										destinationFB = fbconn.DestinationFB();
										destinationName = fbconn.DestinationName();
										fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
									} else if (fbconn.SourceName().equals(eventName)) {
										say("Found connection to " + fbconn.SourceFB() + " " + fbconn.SourceName() + " --> " 
										        + fbconn.DestinationFB() + " " + fbconn.DestinationName());
										destinationEvent = fbconn.DestinationName();
										fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
									}
								}
							}
							
							if (destinationFB != "") {
								// Add the new connections between the diagnostic point and the destination function block. There must always be an
								// event to connect or else it will not work.
								fbapp.addConnection(fbdp.Name(), "DATA_OUT", destinationFB, destinationEvent, true);
								if (destinationName != "") {
									// Connect the output data port to the destination input port. Note that there must be an event, but there is not
									// always a source output port to route.
									fbapp.addConnection(fbdp.Name(), "DATA_OUT_" + fbVar.StringFromDataType(dataType), destinationFB, destinationName, true);
								}	
							}
							
							// Connect the output event to the diagnostic point.
							fbapp.addConnection(fb.Name(), dp.fbEventName, "DP_" + dp.SIFBinstanceID(), "DATA_IN", true);
							if (dp.fbPortName != "") {
								// There is an output data port, so connect it to the correct input port on the diagnostic point.
								fbapp.addConnection(fb.Name(), dp.fbPortName, "DP_" + dp.SIFBinstanceID(), "DATA_IN_" + fbVar.StringFromDataType(dataType), true);
							}
							
							// These connections activate and initialise the diagnostic point.
							fbapp.addConnection("START", "COLD", "DP_" + dp.SIFBinstanceID(), "START", true);
							fbapp.addConnection("START", "WARM", "DP_" + dp.SIFBinstanceID(), "START", true);
		
							fbapp.add(fbdp);
							status = fbAppCodes.REWIRED;
						} else {
							errorHandler.addDescription("Output port " + dp.fbPortName + " on " + dp.fbName() + " has no event " + dp.fbEventName() + 
									                    " assigned to trigger it. The diagnostic package information is out-of-date.");
							status = fbAppCodes.EVENT_UNDEFINED;
						}
						break;
					}
				}	
				
			} else {
				// This section caters for DPs that are triggering or monitoring events with no associated
				// data input or data output ports. 
				ptrEvent = fb.findEvent(dp.fbEventName);
				if (ptrEvent != NOT_FOUND) {
					// The diagnostic package specified the event, but the previous step confirmed
					// that the package information is not out-of-date; an event matching the name
					// was found in the function block event list.
					fbEvent = fb.Event(ptrEvent);
					eventName = fbEvent.EventName;
					
					switch (fbEvent.EventType()) {
					case EVENT_INPUT:
						say(fbEvent.EventType() + " Input port " + dp.fbEventName() + " " + dp.fbPortName());							
						fbdp.Name("DP_" + dp.SIFBinstanceID());
						fbdp.Type("DP");
						dataType = DataTypes.DATATYPE_EVENT;
						fbdp.addParameter("DATA_TYPE", String.valueOf(dataType));
						// RA_BRD The POLL_TIME should be parameterized ... maybe in the diagnostic package?
						fbdp.addParameter("POLL_TIME", "T#"+ pollTime + "ms");
						fbdp.addParameter("ADDRESS", server.hostName()); 
						fbdp.addParameter("PORT", String.valueOf(server.listenerPortNumber())); 
						fbdp.addParameter("INST_ID", String.valueOf(dp.SIFBinstanceID()));	
						
						// The diagnostic point is being inserted into the input event. Break the
						// previous direct connections if they exist and divert them.
						foundConnection = false;
						for (int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
							fbconn = fbapp.Connection(ptrConnection);
							say(fbconn.SourceFB() + " " + fbconn.SourceName()
							    + " ---> " + 
								fbconn.DestinationFB() + " " + fbconn.DestinationName());									   
							if (fbconn.DestinationFB().equals(fb.Name())) {
								if (fbconn.DestinationName().equals(dp.fbEventName())) {    
									foundConnection = true;
									say("Found connection to from " + fbconn.SourceFB() + " " + fbconn.SourceName() + " --> " 
									    + fbconn.DestinationFB() + " " + fbconn.DestinationName());
									sourceFB = fbconn.SourceFB();
									sourceName = fbconn.SourceName();
									destinationFB = fbconn.DestinationFB();
									destinationName = fbconn.DestinationName();
									// Break the original connection.
									fbapp.updateConnection(fbconn, sourceFB, sourceName, destinationFB, destinationName, false); 
								} else if (fbconn.SourceName().equals(eventName)) {
									say("Found connection to " + fbconn.SourceFB() + " " + fbconn.SourceName() + " --> " 
									    + fbconn.DestinationFB() + " " + fbconn.DestinationName());
									destinationEvent = fbconn.DestinationName();
									fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
								}
							}
						}
						
						// Add the new event-only connections between the diagnostic point and the function block.
						fbapp.addConnection("DP_" + dp.SIFBinstanceID(), "DATA_OUT", fb.Name(), eventName,  true);
						if (foundConnection) {
							// There is an incoming event connection from another function block to this diagnostic point.
							fbapp.addConnection(sourceFB, sourceName, fbdp.Name(), "DATA_IN", true);
						}
						
						fbapp.addConnection("START", "COLD", "DP_" + dp.SIFBinstanceID(), "START", true);
						fbapp.addConnection("START", "WARM", "DP_" + dp.SIFBinstanceID(), "START", true);

						fbapp.add(fbdp);
						status = fbAppCodes.REWIRED;						
						break;
						
					case EVENT_OUTPUT:
						say(fbEvent.EventType() + " Output port " + dp.fbPortName + " " + eventName);							
						fbdp.Name("DP_" + dp.SIFBinstanceID());
						fbdp.Type("DP");
						fbdp.Comment("Diagnostic point for " + fb.Name());
						
						dataType = DataTypes.DATATYPE_EVENT;
						// inputPort = "DATA_IN_" + fbVar.StringFromDataType(dataType); RA_BRD
						fbdp.addParameter("DATA_TYPE", String.valueOf(dataType));
						
						// RA_BRD The POLL_TIME should be parameterized ... maybe in the diagnostic package?
						fbdp.addParameter("POLL_TIME", "T#"+ pollTime + "ms");
						fbdp.addParameter("ADDRESS", server.hostName()); 
						fbdp.addParameter("PORT", String.valueOf(server.listenerPortNumber())); 
						fbdp.addParameter("INST_ID", String.valueOf(dp.SIFBinstanceID()));	
						
						// The diagnostic point is being inserted only into the output event. Break the
						// previous direct connections if they exist and divert them.	
						foundConnection = false;
						for(int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
							fbconn = fbapp.Connection(ptrConnection);
							say(fbconn.SourceFB() + " " + fbconn.SourceName()
							    + " ---> " + 
								fbconn.DestinationFB() + " " + fbconn.DestinationName());									   
							if (fbconn.SourceFB().equals(fb.Name())) {
								if (fbconn.SourceName().equals(dp.fbPortName())) {
									foundConnection = true;
									say("Found connection to " + fbconn.SourceFB() + " " + fbconn.SourceName() + " --> " 
								        + fbconn.DestinationFB() + " " + fbconn.DestinationName());									
									destinationFB = fbconn.DestinationFB();
									destinationName = fbconn.DestinationName();
									fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
								} else if (fbconn.SourceName().equals(eventName)) {
									say("Found connection to " + fbconn.SourceFB() + " " + fbconn.SourceName() + " --> " 
									    + fbconn.DestinationFB() + " " + fbconn.DestinationName());
									destinationEvent = fbconn.DestinationName();
									fbapp.updateConnection(fbconn, fbconn.SourceFB(), fbconn.SourceName(), fbconn.DestinationFB(), fbconn.DestinationName(), false); 
								}
							}
						}

						// Add the new event-only connections between the diagnostic point and the function block.
						fbapp.addConnection(fb.Name(), eventName, "DP_" + dp.SIFBinstanceID(), "DATA_IN", true);
						if (foundConnection) {
							// There is an outgoing event connection from the diagnostic point to another function block.
							fbapp.addConnection(fbdp.Name(), "DATA_OUT", destinationFB, destinationName, true);
						}
						
						fbapp.addConnection("START", "COLD", "DP_" + dp.SIFBinstanceID(), "START", true);
						fbapp.addConnection("START", "WARM", "DP_" + dp.SIFBinstanceID(), "START", true);

						fbapp.add(fbdp);
						status = fbAppCodes.REWIRED;
						break;
					}
				
				} else {
					// The diagnostic package specified the event, but the event name could not be 
					// found in the function block definition. That suggests that the diagnostic package
					// information is out-of-date.
					status = fbAppCodes.INVALID_EVENT_NAME;
				}
			}
		} else {
			status = fbAppCodes.FB_NOT_FOUND;
		}
		return status;
	}
	
	// 
	// createForteBootfile()
	// =====================
	// Generates a FORTE-compliant forte.fboot file that will instantiate the function block
	// application specified by the function block application definition passed in.
	//
	// fbapp         The list of function blocks and their properties that define this function
	//	             block application. This is a structure of the type FunctionBlockApp<> which
	//               contains details of individual function blocks, all their properties and
	//		         a complete list of connections.
	//
	// bootFilePath  The fully-qualified path to the directory where the forte.boot file is to
	//               be created.
	//
	// errorHandler  The error handling object which accumulates all the errors for the process.
	//
	// returns       boolean true if the boot file was created.
	//
	public boolean createForteBootfile(FunctionBlockApp fbapp, String bootFilePath, ErrorHandler errorHandler) {
		boolean status = false;	

		int requestID = 1;
		
		FunctionBlock fb = new FunctionBlock();
//		FunctionBlockEvent fbevent = new FunctionBlockEvent();
		FunctionBlockParameter fbparameter = new FunctionBlockParameter();
		FunctionBlockConnection fbconnection = new FunctionBlockConnection();
		
		//String FORTE_PATH = System.getProperty("user.home") + "/4diac-ide/forte/src/";
		
		FileIOstatus IOstatus = FileIOstatus.UNDEFINED;
		FileIO bootfile = new FileIO();
		
		IOstatus = bootfile.createFile(bootFilePath, "forte.fboot");
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
			bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"START\"/>\n");
			bootfile.close();
			status = true;
			
		} else {
			errorHandler.addDescription("Could not create forte.fboot." + bootfile.errorDescription());
			status = false;
		}
		
		return status;
	}
	
	//
	// createMonitor()  RA_BRD deprecated
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
		int dataType = DataTypes.DATATYPE_INT;
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
					System.out.println("Monitor " + fb.Name() + " " + outputToMonitor + " " + server.listenerPortNumber() + eventName);
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
					errorHandler.addDescription("Cannot monitor " + blockToMonitor + ". Cannot find linked event for output " + outputToMonitor);
				}

			} else {	
				status = fbAppCodes.FB_NOT_FOUND;
				errorHandler.addDescription("Function block '" + blockToMonitor + "' not found in application.");
			}
		}
		return status;
	}
	
	// 
	// createTrigger() RA_BRD deprecated
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
		int dataType = DataTypes.DATATYPE_INT;
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
					
	
					// RA_BRD This needs to come from a pool of server addresses and listener ports
					//        so we can run multiple AGENT_RECV instances.
					fbtrigger.addParameter("ADDRESS", "127.0.0.3"); 
					fbtrigger.addParameter("PORT", "62503"); 
		
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
					errorHandler.addDescription("Cannot trigger " + blockToTrigger + ". Cannot find linked event for input " + inputToTrigger);
				}

			} else {	
				status = fbAppCodes.FB_NOT_FOUND;
				errorHandler.addDescription("Function block '" + blockToTrigger + "' not found in application.");
			}
		}
		return status;
	}	
	
	
	//
	// runDiagnostic()
	// ===============
	// https://examples.javacodegeeks.com/core-java/dynamic-class-loading-example/
	// https://stackoverflow.com/questions/21544446/how-do-you-dynamically-compile-and-load-external-java-classes
	//
	public boolean runDiagnostic(String applicationPath, String diag) {
		say("Running diagnostic" + applicationPath + diag);
		
		JavaClassLoader javaClassLoader = new JavaClassLoader();
		javaClassLoader.invokeClassMethod(applicationPath + "MyClass.class", "sayHello");
		
		return true;
	}
	
	//
	// lastErrorDescription()
	// ======================
	public String lastErrorDescription() {
		return errorHandler.Description();
	}
		
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
		
		// Rewiring code for Simple2mon
		// ============================
			//	status = createMonitor("T_SENSOR_01", "TEMP", server, fbapp, errorHandler);
			//	if (status != fbAppCodes.REWIRED) {
			//		System.out.println("Rewire error " + lastErrorDescription);
			//	}
				
			//	status = createTrigger("F_TO_C_CONV", "IN", server, fbapp, errorHandler); 
			//	if (status != fbAppCodes.REWIRED) {
			//		System.out.println("Rewire error " + lastErrorDescription);
			//	} else {
			//		status = createMonitor("F_TO_C_CONV", "OUT", server, fbapp, errorHandler);
			//		if (status != fbAppCodes.REWIRED) {
			//			System.out.println("Rewire error " + lastErrorDescription);
			//		}	
			//	}
		// ====================================================================================
		
		// Rewiring code for the HVAC
		// ==========================
		// Monitor the Z_TEMPERATURE TEMP output.	
		status = createMonitor("Z_CONTROLLER", "ZONE_TEMP", server, fbapp, errorHandler);
		if (status != fbAppCodes.REWIRED) {
			System.out.println("Rewire error " + lastErrorDescription);
		}
		
		status = createTrigger("Z_CONTROLLER", "TEMP", server, fbapp, errorHandler); 
	  	if (status != fbAppCodes.REWIRED) {
	  		System.out.println("Rewire error " + lastErrorDescription);
		} 
		
		fbapp.displayFunctionBlocks(fbapp);
		//createForteBootfile(fbapp);
		
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
	
	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable silence
	//
	private void say(String whatToSay){
		if(!isSilent) {
			System.out.println(whatToSay);
		}
	}

	
	
	//
	// calcDelta()
	// ===========
	//
	public int calcDelta(float expectedValue, float sampledValue, String decimalFormat) {
		int delta = 0;
	
		DecimalFormat df = new DecimalFormat(decimalFormat);
		df.setRoundingMode(RoundingMode.UP);
		BigDecimal expected = new BigDecimal(df.format(expectedValue));
		BigDecimal sampled = new BigDecimal(df.format(sampledValue));
		
		delta = expected.compareTo(sampled);
		//System.out.println("expected = " + expected + " sampled = " + sampled + " delta = " + delta);
		if (delta != 0) {
			MathContext mc = new MathContext(decimalFormat.length() - 2);
			BigDecimal diff = expected.subtract(sampled, mc);
		//	System.out.println("diff  = " + diff);
			BigDecimal cmp = new BigDecimal(0.01);
			delta = (diff.compareTo(cmp));
			delta = Math.abs(delta);
		}
	//	System.out.println(delta);
		return delta;
	}
}		
		
	//	if (expected.compareTo(sampled) == 0) {
	//		// There is no significant difference to the precision specified.
	//		delta = 0;
	//	} else {
	//		if ( Math.abs(val1.compareTo(val2)) <= 0.0001) {
	//			say("Delta is fine\n");
	//		} else {
	//			say("WHOOPS - delta is NOT fine: " + val1.compareTo(val2) 
	//			    + " " + val1 + " " + val2 + " " + Math.abs(val1.compareTo(val2)) + "\n");
	//		}	
	//	}
		

