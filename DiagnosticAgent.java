//
// DIAGNOSTIC AGENT
// ================
// Implements the DiagnosticAgent instance that inherits a set of
// Team goals for finding and diagnosing faults.
//
// AUT University
//
// Revision History
// ================
// 23.05.2019 BRD Original version based on the agents from the Dam Scenario and comments
//                from Dennis Jarvis.
// 03.07.2019 BRD Moved goal execute() methods into this class to allow multiple agents
//                to be instanced properly.
//
// Documentation
// =============
// The diagnostic agent is analogous to the Cell class that extends the Team class from Chapter
// Five of the GORITE book. The agent extends the GORITE Performer class,
//
// The execute() method of a Goal definition is key to an agent's ability to perform a the tasks
// required to achieve that goal. It is where the real work for a goal gets done. 
//
// When GORITE calls this method, it performs steps to try and fulfill the goal and then exits
// with a Goal state. This is used to determine if the goal has been completed or should be
// re-tried later. If it needs to be retried, GORITE will make sure it is re-scheduled.
//
package fde;

import com.intendico.gorite.*;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.IOException;

import fde.ExitCodes;
import static fde.Constants.*;

public class DiagnosticAgent extends Performer implements Runnable {
	// Used to turn off and on console messages during development.
	private boolean isSilent = false;
	
	private String agentName = "";
	
	// RA_BRD temporary values used during early diagnostic testing runs ..
	int packetCount = 0; // RA_BRD temporary
	int cycleCount = 0;
	int dataValue = 0;
	float testTemperature = (float) 30.00;
	float sampledData = 0;
	float lastSample = 9999;
	int outlierCount = 0;
		
	String packetValue = "";

	public static int MAX_COUNT = 50000;
	private boolean clientIsConnected = false;
	
	class AgentModes {
		static final int UNDEFINED = 0;
		static final int PASSTHROUGH_ENABLED = 1;
		static final int TRIGGER_ENABLED = 2;		
		static final int TRIGGER_DATA_VALUE = 3;
		static final int POLL_AGENT = 4;
		static final int SAMPLED_DATA = 5;
	}
	
	class PacketDelimiters {
		static final String START_OF_PACKET = "*";
		static final String FIELD_SEPARATOR = "|";
		static final String END_OF_PACKET = "&"; 
	}
	
	int currentAgentMode = AgentModes.PASSTHROUGH_ENABLED; 
	
	DiagnosticAgentCapabilities skills = new DiagnosticAgentCapabilities();
	FunctionBlockApp fbapp = new FunctionBlockApp();
	Beliefs beliefs = new Beliefs();
	
	DiagnosticPoints dps = new DiagnosticPoints();
	
	// Create a runnable instance of the server. This will
	// manage all communications from the function block
	// application to this agent.
	//
	// RA_BRD - how should we define the server configuration? It is 
	// part of this diagnostic engine instance (and perhaps this is
	// the server used by this team of agents, so it is perhaps higher
	// up the hierarchy?
	//
	NIOserver server = new NIOserver("127.0.0.3", 62503); 
	
	// RA_BRD temporary instantiation of the dynamic script library.
	Scripts script = new Scripts();

	// Setup the application information. <RA_BRD - this will need to come
	// from higher up the chain of command later...
	private String homeDirectory = System.getProperty("user.home");
	
	// RA_BRD - should this information come from the Team?
	String applicationPath = homeDirectory + "/Development/4diac/HVAC/";
	private String applicationName = "HVAC";

	Action diagnose = new Action(DiagnosticTeam.FIND_FAULTS) {
		public Goal.States execute(
			boolean reentry, Data.Element[] ins, Data.Element[] outs) {
			return Goal.States.PASSED;
		}
	};

	//
	// run()
	// =====
	@Override
	public void run() {
		
	}
	
	public DiagnosticAgent(String agentName) {
		super(agentName);
		this.agentName = agentName;
		say("Created DiagnosticAgent " + agentName);
		
		new Thread(server).start(); 

		// Create and add team goals to this agent. The method create() takes as its
		// arguments the names of the elements in the data context that
		// are to be used as the inputs and outputs for the goal execution.
		// Execution will not proceed until all inputs have been assigned values.
		//
		addGoal(diagnose.create(new String[] {DiagnosticTeam.FIND_FAULTS}, null));
		say("Assigned role FIND_FAULTS to " + this.agentName + "\n");
	}

	//
	// GOAL EXECUTION
	// ==============
	// configureDiagnostics()
	// ======================
	// This goal configures the function block application for diagnosis. If any part of
	// that process fails, the agent abandons this goal. All remaining goals are terminated
	// except the final reporting goal.
	//
	Goal configureDiagnostics() {
		say("Setting up goal configureDiagnostics()");
		return new Goal (CONFIGURE_DIAGNOSTICS) {
			//
			// execute()
			// =========
			public Goal.States execute(Data data) {
				int loadStatus = XMLErrorCodes.UNDEFINED;

				// Load the function block application information.  
				beliefs.create("AppName", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, applicationName);
				beliefs.create("AppPath", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, applicationPath);
				           
				loadStatus = fbapp.load(applicationPath, applicationName + ".sys");
				
				if (loadStatus != XMLErrorCodes.LOADED) {
					say("Function block application " + applicationName + ".sys could not be loaded.\n" + fbapp.lastErrorDescription);
					beliefs.create("LoadStatus", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.FALSE, 
						           "Function block application " + applicationName + ".sys could not be loaded.\n" + fbapp.lastErrorDescription);
					return Goal.States.FAILED;
					
				} else {
					beliefs.create("LoadStatus", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, "Application loaded successfully.");
					
					// Create and deploy the diagnostic test harness.
					if (skills.createHarness(fbapp, dps, applicationPath, server)) {
						beliefs.create("DeployedStatus", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, "Application deployed successfully.");
						fbapp.displayFunctionBlocks(fbapp);
						say("\nCreated diagnostic harness. Waiting for FORTE to start");
						// RA_BRD build a deployment timeout into this if we do not end up starting forte successfully.
						while (server.ConnectionCount() == 0) {
							delay(50);
						}						
						return Goal.States.PASSED;
					} else {
						fbapp.displayFunctionBlocks(fbapp);
						beliefs.create("AppDeployed", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.FALSE, 
						        	   "Could not create diagnostic harness. " + skills.lastErrorDescription());
						say("\nCould not create diagnostic harness:\n" + skills.lastErrorDescription());
						return Goal.States.FAILED;
					}
				}
			}
		};
	}

	//
	// identifyFault()
	// ===============
	// This goal performs the primary goal of watching the function block application and
	// identifying when a fault symptom appears.
	//
	// RA_BRD dynamic diagnostic monitor launch code for later
	// skills.runDiagnostic(applicationPath, "MyClass"); System.exit(0); 
	//
	Goal identifyFault() {
		say("Setting up goal identifyFault()");
		return new Goal (IDENTIFY_FAULT) {
			//
			// execute()
			// =========
			@SuppressWarnings("static-access")
			public Goal.States execute(Data data) {	
				beliefs.displayBeliefs();
				
				say(agentName + ": ready in identifyFault()\n");
				boolean status = false;
				System.out.print("Beliefs " + beliefs.Count());
				status = script.monitor(dps, server);
				if (!status) {
					return Goal.States.PASSED;
				} else {
					delay(500);
					Thread.currentThread().yield();
					return Goal.States.STOPPED;
				}	
			}	
		};		
	}			
				
	//
	// diagnoseFault()
	// ===============
	Goal diagnoseFault() {
		say("Setting up goal diagnoseFault()");
		return new Goal (DIAGNOSE_FAULT) {
			//
			// execute()
			// =========
			public Goal.States execute(Data data) {
				boolean status = false;
				say(agentName + ": ready in diagnoseFault()\n");
				
				// Use the available diagnostic resources to walk through the FBA iteratively
				// to see if the faulty component can be identified.
				status = script.F_TO_C_CONV(dps, server);
				
				
				
				// Now all the tests has completed, mark
				// the diagnoseFault() goal as passed so
				// that the FDE can present a diagnosis
				// by executing the reportFault() goal.
				return Goal.States.PASSED;
			}
		};
	}

	//
	// reportFault()
	// =============
	Goal reportFault() {
		say("Setting up goal reportFault()");
		return new Goal (REPORT_FAULT) {
			//
			// execute()
			// =========
			public Goal.States execute(Data data) {
				say(agentName + ": ready in reportFault()\n");
				
				// Present a diagnosis
		
				return Goal.States.PASSED;
			}
		};
	}

	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable isSilent
	// true.
	//
	private void say(String whatToSay){
		if(!isSilent) {
			System.out.println(whatToSay);
		}
	}

	//
	// pause()
	// =======
	@SuppressWarnings("unused")
	private void pause(String prompt) {
		String userInput = "";
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		System.out.println(prompt);
		try {
			userInput = stdIn.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//
	// delay()
	// =======
	private void delay(int milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep((long) milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}
}
	
//============================================================================================================
//RA_BRD - Code in the basement
//=============================	
	
			//	int serverStatus = ExitCodes.UNDEFINED;
			//	int count = (int) data.getValue("count");
			//	NIOserverPacket packet = new NIOserverPacket();
				
		//		DiagnosticPoint Z_TEMPERATURE_TEMP = new DiagnosticPoint("Z_TEMPERATURE", "TEMP", fbapp, server, 1);
		//		DiagnosticPoint F_TO_C_CONV_TEMP_C = new DiagnosticPoint("F_TO_C_CONV", "TEMP_C", fbapp, server, 2);
				
//				double testTemperatureF = 0;
//				double resultTemperatureC = 0;	
//				double expectedValue = 0;
//				int counter = 0;
//				
//				boolean testing = true;
//				
//				say(agentName + " identifyFault()");
//				
//				
//				while(testing) {
//					testTemperatureF = Z_TEMPERATURE_TEMP.read();
//					System.out.println("Z_TEMPERATURE TEMP = " + testTemperatureF + "\n");
//					
//					resultTemperatureC = F_TO_C_CONV_TEMP_C.read();
//					System.out.println("F_TO_C_CONV TEMP_C = " + resultTemperatureC + "\n");
//					
//					counter++;
//					if (counter > 10) {
//						// Close the gate on DP_1
//						say("gateClose()");
//						Z_TEMPERATURE_TEMP.gateClose();						
//						
//						// Starting range for test temperatures
//						testTemperatureF = -32;
//						
//						for (int test = 0; test < 25; test++) {
//							expectedValue = (testTemperatureF - 32) / 1.8;
//							if (Z_TEMPERATURE_TEMP.trigger(testTemperatureF)) {
//								if (F_TO_C_CONV_TEMP_C.readWait(testTemperatureF, expectedValue, .0001, 10, 100)) {
//									System.out.println("Passed "+ testTemperatureF + " " + F_TO_C_CONV_TEMP_C.value());
//									testTemperatureF = testTemperatureF + 15.0;
//								} else {
//									System.out.println("Failed " + testTemperatureF + " " + F_TO_C_CONV_TEMP_C.value());
//								}	
//							}	
//						}		
//								
								
//								System.out.println("Triggered " + testTemperatureF); 
//								
//								boolean found = false;
//								for (int wait = 0; wait < 10; wait++) {
//									if (F_TO_C_CONV_TEMP_C.hasData()) {
//										resultTemperatureC = F_TO_C_CONV_TEMP_C.read();
//										System.out.println(" ==> F_TO_C_CONV TEMP_C = " + resultTemperatureC + " " + expectedValue + "\n");
//										
//										if (F_TO_C_CONV_TEMP_C.compare(resultTemperatureC, expectedValue, .0001)) {
//											System.out.println("Passed test - values compare\n");
//											found = true;
//											break;
//										} else {
//											System.out.println("Values do not compare\n");
//										}
//									} else {
//										delay(500);
//									}
//								}
//								if (!found) {
//									System.out.println("No reading returned");
//								}
								// ---
								
						//	}
						//	testTemperatureF = testTemperatureF + 15.0;
						//	delay(100);
						//}

						// Re-open the gate on DP_1
	//					Z_TEMPERATURE_TEMP.gateOpen();
		//				say("Gate re-opened...");
	//					counter = 0;
			//		}
	
				//	delay(50);
	//				Thread.currentThread().yield();
			//	}
				
				// return Goal.States.PASSED;
		//		return Goal.States.STOPPED;
		//	}	
//		};		
//	}			
		
	
// SAVED CODE FROM identifyFault()
	
	//
	// identifyFault()
	// ===============
//	Goal identifyFault() {
//		say("Setting up goal identifyFault()");
//		return new Goal (IDENTIFY_FAULT) {
			//
			// execute()
			// =========
	//		public Goal.States execute(Data data) {	
			//	int serverStatus = ExitCodes.UNDEFINED;
			//	int count = (int) data.getValue("count");
			//	NIOserverPacket packet = new NIOserverPacket();
//	
//	//
//	// Monitoring and testing the HVAC temperature subsystem
//		// =====================================================
//	int testSequence = 0;
//	String dataValue = "";
//	int command = AgentModes.UNDEFINED;
//	String dataPacket = "";	
//	float tempC = 0;
//	float tempC2 = 0;
//	float delta = 0;
//	
//					
//	final int Z_TEMPERATURE_TEMP = 1;
//	final int F_TO_C_CONV_OUT = 2;
//	
//	switch (currentAgentMode) {
//	case AgentModes.PASSTHROUGH_ENABLED:
//		while (server.inQueueSize(Z_TEMPERATURE_TEMP) > 0) {
//			packet = server.getPacket(Z_TEMPERATURE_TEMP);
//			
//			try {
//				command = Integer.valueOf(packet.command());
//			} catch (NumberFormatException nfe) {
//				command = AgentModes.UNDEFINED;
//			}
//			
//			switch (command) {
//			case AgentModes.SAMPLED_DATA:
//				say("SAMPLED DATA from AGENT_GATE_" + packet.SIFBinstanceID() + " [" 
//					+ packet.dataValue() + "] Queue size = " + server.inQueueSize(Z_TEMPERATURE_TEMP));
//				
//				sampledData = Float.valueOf(packet.dataValue());
//				
//				packetCount++;
//				if (packetCount >= 200) {
//					dataPacket = "*" + AgentModes.AGENT_ENABLED + "|&";
//					currentAgentMode = AgentModes.AGENT_ENABLED;
//					server.sendPacket(Z_TEMPERATURE_TEMP, dataPacket);
//					cycleCount = 0;
//					testTemperature = (float) 20.5;
//				}
//				
//				// Catch the result from the second agent
//				packet = server.getPacket(F_TO_C_CONV_OUT);
//				
//				try {
//					command = Integer.valueOf(packet.command());
//				} catch (NumberFormatException nfe) {
//					command = AgentModes.UNDEFINED;
//				}
//				
//				switch (command) {
//				case AgentModes.SAMPLED_DATA:
//					
//					tempC = (sampledData - (float) 32) * ((float) 5 / (float) 9);
//					tempC2 = Float.valueOf(packet.dataValue());
//					delta = skills.calcDelta(tempC2, tempC, "0.00");
//					
//					say("Z_TEMPERATURE_TEMP = " + sampledData + " F  -->  F_TO_C_CONV_OUT = " + tempC2 
//					 	+ "\n should be " + tempC + " C delta = " + delta + "\n");
//					
//					if (lastSample == 9999) {
//						// Initialisation of sampling routine.
//						lastSample = sampledData;
//					} else {
//						// Was this value an outlier?
//						if (Math.abs(lastSample - sampledData) > 9) {	
//							outlierCount++;
//							say("Outlier !!! delta = " + skills.calcDelta(lastSample, sampledData, "0.00") + " outlierCount = " + outlierCount + "\n");
//							
//						} else {
//							lastSample = sampledData;
//						}
//					}
//					break;
//				}
//				break;
//			}		
//		}
//		break;
//		
//	case AgentModes.AGENT_ENABLED:
//		while (server.inQueueSize(Z_TEMPERATURE_TEMP) > 0) {
//			packet = server.getPacket(Z_TEMPERATURE_TEMP);
//			try {
//				command = Integer.valueOf(packet.command());
//			} catch (NumberFormatException nfe) {
//				command = AgentModes.UNDEFINED;
//			}
//			
//			switch (command) {
//			case AgentModes.POLL_AGENT:
//				say("POLL_AGENT from AGENT_GATE_" + packet.SIFBinstanceID() + " [" 
//					+ packet.dataValue() + "] Queue size = " + server.inQueueSize(Z_TEMPERATURE_TEMP));
//				
//				cycleCount++;
//				if (cycleCount >= 40) {
//					// Stop triggering this agent.
//					dataPacket = PacketDelimiters.START_OF_PACKET + AgentModes.PASSTHROUGH_ENABLED + PacketDelimiters.FIELD_SEPARATOR
//								+ PacketDelimiters.END_OF_PACKET;
//					server.sendPacket(Z_TEMPERATURE_TEMP, dataPacket);
//					currentAgentMode = AgentModes.PASSTHROUGH_ENABLED;
//					packetCount = 0;
//				} else {	
//					dataValue = Double.toString(testTemperature);
//					// Send the agent a data value to inject.
//					dataPacket = PacketDelimiters.START_OF_PACKET + AgentModes.TRIGGER_DATA_VALUE + PacketDelimiters.FIELD_SEPARATOR
//								+ dataValue.length() + PacketDelimiters.FIELD_SEPARATOR + dataValue + PacketDelimiters.FIELD_SEPARATOR
//								+ PacketDelimiters.END_OF_PACKET;
//					
//					tempC = (testTemperature - (float) 32) * ((float) 5 / (float) 9);
//					
//				//	System.out.println(testTemperature + " " + tempC);
//					
//					//OUT() = ((IN()-32))*5/9;
//					
//					
//					say("Trigger value = " + dataValue + " F " + tempC + " C "
//						+ "cycleCount = " + cycleCount + "\n");
//					server.sendPacket(Z_TEMPERATURE_TEMP, dataPacket);
//					testTemperature = testTemperature + (float) 1.1;
//				
//					
//					// Catch the result from the second agent
//					packet = server.getPacket(F_TO_C_CONV_OUT);
//					
//					try {
//						command = Integer.valueOf(packet.command());
//					} catch (NumberFormatException nfe) {
//						command = AgentModes.UNDEFINED;
//					}
//					
//					switch (command) {
//					case AgentModes.SAMPLED_DATA:
//						say("F_TO_C_CONV.OUT = " + packet.dataValue() + " C");
//						break;
//					}
//				}
//			}		
//		}
//		break;
//	}	
//	
//	// Evaluate the status of this goal. Did the agent complete it?
//	count++;
//	data.setValue("count", (int) count);
//	if (outlierCount >= 3) {
//		// a fault has been found !!!
//		say("Goal passed\n");
//		data.setValue("count", (int) 0);
//		return Goal.States.PASSED;
//	} else {
//		if (count == MAX_COUNT) {
//			say("Goal passed\n");
//			data.setValue("count", (int) 0);
//			return Goal.States.PASSED;
//		} else {
//			say("Goal stopped");
//			try {
//				TimeUnit.MILLISECONDS.sleep((long) 500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			return Goal.States.STOPPED;
//		}	
//	}
//}	
//};

					


	
	// 
	// rewireApp()
	// ===========
//	public void rewireApp(FunctionBlockApp fbapp) {
//		int requestID = 1;
//		
//		FunctionBlock fb = new FunctionBlock();
//		FunctionBlockEvent fbevent = new FunctionBlockEvent();
//		FunctionBlockParameter fbparameter = new FunctionBlockParameter();
//		FunctionBlockConnection fbconnection = new FunctionBlockConnection();
//		
//		String FORTE_PATH = System.getProperty("user.home") + "/4diac-ide/forte/src/";
//		FileIOstatus IOstatus = FileIOstatus.UNDEFINED;
//		FileIO bootfile = new FileIO();
//		
//		IOstatus = bootfile.createFile(FORTE_PATH, "forte.fboot");
//		if (IOstatus == FileIOstatus.FILE_CREATED) {
//			bootfile.write(";<Request ID=\"" + requestID++ + "\" Action=\"CREATE\"><FB Name=\"EMB_RES\" Type=\"EMB_RES\" /></Request>\n");
//			
//			for (int ptr = 0; ptr < fbapp.fbCount(); ptr++) {
//				fb = fbapp.getfb(ptr);
//				
//				if (fb.Name() != "START") {
//					bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"CREATE\">" +
//					      "<FB Name=\"" + fb.Name() + "\" Type=\"" + fb.Type() + "\" /></Request>\n");
//					
//					// Configure the parameters for this function block. 
//					for (int ptrParameter = 0; ptrParameter < fb.ParameterCount(); ptrParameter++) {
//						fbparameter = fb.Parameter(ptrParameter);
//						
//						bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"WRITE\">" +
//								       "<Connection Source=\"" + fbparameter.Value() + "\" " + 
//								       "Destination=\"" + fb.Name() + "." + fbparameter.Name() + "\" /></Request>\n");
//					}
//				}	
//			}	
//			
//			// Create the connections between the function blocks.
//			for (int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
//				fbconnection = fbapp.Connection(ptrConnection);
//				bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"CREATE\">" +
//					       "<Connection Source=\"" + fbconnection.SourceFB() + "." + fbconnection.SourceName() + "\" " + 
//					       "Destination=\"" + fbconnection.DestinationFB() + "." + fbconnection.DestinationName()  + "\"" + "/></Request>\n");
//			}
//			
//			// Add the application start command. 
//			bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"START\"/>\n");
//			bootfile.close();
//		} else {
//			System.err.println("Could not create forte.fboot." + bootfile.errorDescription());
//		}
//	}





//private void displayFunctionBlocks(FunctionBlockApp fbapp) {
//FunctionBlock fb = new FunctionBlock();
//FunctionBlockEvent fbevent = new FunctionBlockEvent();
//FunctionBlockParameter fbparameter = new FunctionBlockParameter();
//FunctionBlockConnection fbconn = new FunctionBlockConnection();
//
//System.out.println("____________________________________________________________");
//System.out.println("\nFunction Blocks: " + "[" + fbapp.fbCount() + "]\n");
//
//for (int ptr = 0; ptr < fbapp.fbCount(); ptr++) {
//	fb = fbapp.getfb(ptr);
//	System.out.printf("%-10s %d\n",  "instance :", ptr);
//	System.out.printf("%-10s %s\n",  "Name     :", fb.Name());     
//	System.out.printf("%-10s %s\n",  "Type     :", fb.Type());
//	System.out.printf("%-10s %s\n",  "Comment  :", fb.Comment());
//	System.out.println();
//	
//	System.out.println("Events [" + fb.eventCount() + "]");
//	for (int ptrEvent = 0; ptrEvent < fb.eventCount(); ptrEvent++) {
//		fbevent = fb.Event(ptrEvent);
//		System.out.printf("%15s %d\n",  "instance :", ptrEvent);
//		System.out.printf("%15s %s\n",      "Name :", fbevent.EventName());
//		
//		switch (fbevent.EventType) {
//		case EVENT_INPUT:
//			System.out.printf("%15s %s\n",  "Event Type :", "Input");
//			break;
//		
//		case EVENT_OUTPUT:
//			System.out.printf("%15s %s\n",  "Event Type :", "Output");
//			break;
//		}
//		
//		for (int ptrWithVar = 0; ptrWithVar < fbevent.WithVarCount(); ptrWithVar++) {
//			System.out.printf("%15s %s\n",  "With Var :", fbevent.WithVar(ptrWithVar));
//		}
//		System.out.println();
//	}
//	
//	// The parameter list for this function block. 
//	System.out.println("Parameters [" + fb.ParameterCount() + "]");
//	for (int ptrParameter = 0; ptrParameter < fb.ParameterCount(); ptrParameter++) {
//		fbparameter = fb.Parameter(ptrParameter);
//		System.out.printf("%15s %d\n",   "instance : ", ptrParameter);
//		System.out.printf("%15s %s\n",       "Name : ", fbparameter.Name());
//		System.out.printf("%15s %s\n\n",    "Value : ", fbparameter.Value());	
//	}
//	System.out.println();
//}
//
//System.out.println();
//System.out.println("____________________________________________________________");
//
//System.out.println("\nConnections [" + fbapp.ConnectionCount() + "]\n");
//for(int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
//	fbconn = fbapp.Connection(ptrConnection);
//	System.out.println(fbconn.SourceFB() + " " + fbconn.SourceName() 
//					   + " ---> " + 
//					   fbconn.DestinationFB() + " " + fbconn.DestinationName() );
//}
//
//System.out.println("____________________________________________________________");
//}


//	dataValue++;
//	packetValue = Integer.toString(dataValue);
					
//	dataPacket = "*" + AgentModes.TRIGGER_DATA_VALUE + "|" + packet.SIFBinstanceID() 
//	             + "|" + packetValue.length() + "|" + packetValue + "|&";
//	server.sendPacket(dataPacket);
//}
//break;
//			}	

//	if (cycleCount > 10) {
//			//dataPacket = "*PD|&";
//		dataPacket = "*1|&";
//		server.sendPacket(dataPacket);
//		packetCount = 0;
//		cycleCount = 0;
//	}



//	say("Executing identifyFault() from within agent instance " + agentName + " [" + count + "]");				
//	if (!clientIsConnected) {
//		serverStatus = client.connectToServer("127.0.0.3", 62503);
//		if (serverStatus == ExitCodes.EXIT_SUCCESS) {
//			clientIsConnected = true;
//			System.err.println("Connected to AGENT_RECV client successfully");
//		} else {
//			System.err.println("Could not connect to AGENT_RECV client...");
//		}
//	}

//	if (clientIsConnected) {
//		// Send a value
//		testSequence = (int) data.getValue("testSequence");
//		testSequence++;
//		data.setValue("testSequence", testSequence);

//		// Simple exit criteria for this goal.
//		if (testSequence > 25) {
//			return Goal.States.PASSED;	
//		}

//		testTemperature = (double) testSequence * 0.4;
//		System.out.println("Test temperature = " + testTemperature);

//		if (clientIsConnected) {
//			dataPacket = Double.toString(testTemperature);
//			System.err.println("GORITE count = " + count + " - sending value [" + dataPacket + "]");
//			client.sendPacket(dataPacket);
//		}
//	}


// RA_BRD temporary pause to start FORTE manually.
//	if (count <= 0) {
//		pause("Start FORTE now - press Enter to continue\n");
//	}

//		if (!clientIsConnected) {
//			serverStatus = client.connectToServer("127.0.0.1", 62501);
//			if (serverStatus == ExitCodes.EXIT_SUCCESS) {
//				clientIsConnected = true;
//				System.err.println("Configured client successfully");
//			} else {
//				System.err.println("Could not configure client...");
//			}
//		}

//		count++;
//		data.setValue("count", (int) count);

// Send a test value for every 1,000 GORITE execute() calls to slow
// things down a bit...
//	if ((count % 1000) == 0) {								
//		if (clientIsConnected) {
//			dataPacket = Integer.toString((count/1000) * 3);
//			System.err.println("GORITE sequence = " + count + " Sending value [" + dataPacket + "]");
//			client.sendPacket(dataPacket);
//		}
//	}	

// Read the incoming SIFB data queues until they are empty before
// releasing this GORITE execute() call.
//			while (server.getQueueSize() > 0) {
//				packet = server.getPacket();
//				System.err.println("AGENT_SIFB_" + packet.getSIFBinstanceID() + " [" + packet.getdataPacket() + "] [" + server.getQueueSize() + "]");
//			}

//			data.setValue("count", (int) count);
//			return Goal.States.STOPPED;


//
// displayFunctionBlocks()
// =======================
// Display a list of the function blocks found.
//
// <RA_BRD - Note that this method tests external access to the function block application.
// Once the class is stable, update the internal function inside the class FunctionBlockApp
// for longer-term development and debugging use.
//

