//
// DIAGNOSTIC AGENT
// ================
// Implements the DiagnosticAgent instance that inherits a set of
// Team goals for finding and diagnosing faults.
//
// This is analogous to the Cell class that extends the Team class from Chapter
// Five of the GORITE book.
//
// Barry Dowdeswell - 2017-2020
//
// Revision History
// ================
// 23.05.2019 BRD Original version based on the agents from the Dam Scenario and comments
//                from Dennis Jarvis.
// 03.07.2019 BRD Moved goal execute() methods into this class to allow multiple agents
//                to be instanced properly.
//
package fde;

import com.intendico.gorite.*;

import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.IOException;

import fde.ExitCodes;
import static fde.Constants.*;

public class DiagnosticAgent extends Performer {
	private String TODO_GROUP = "diagnostic agent Job List";
	// used to turn off and on console messages during development.
	private boolean silence = false;
	private String agentName = "";
	
	int packetCount = 0; // RA_BRD temporary

	public static int MAX_COUNT = 50000;
	private boolean clientIsConnected = false;
	
	DiagnosticAgentCapabilities skills = new DiagnosticAgentCapabilities();
	FunctionBlockApp fbapp = new FunctionBlockApp();
	
	// Create a runnable instance of the server. This will
	// manage all communications from the function block
	// application to this agent.
	//
	// <RA_BRD how should we define the server configuration? It is 
	// part of this diagnostic engine instance (and perhaps this is
	// the server used by this team of agents, so it is perhaps higher
	// up the heirarchy?
	//
	NIOserver server = new NIOserver("127.0.0.3", 62503);

	Client client = new Client();

	// Setup the application information. <RA_BRD - this will need to come
	// from higher up the chain of command later...
	private String homeDirectory = System.getProperty("user.home");
	//private String applicationPath = homeDirectory + "/Development/4diac/Simple2/";
	//String applicationPath = homeDirectory + "/Development/4diac/Simple2mon/";
	//private String applicationName = "Simple2";

	String applicationPath = homeDirectory + "/Development/4diac/HVAC/";
	
	private String applicationName = "HVAC";

	Action diagnose = new Action(DiagnosticTeam.FIND_FAULTS) {
		public Goal.States execute(
			boolean reentry, Data.Element[] ins, Data.Element[] outs) {
			return Goal.States.PASSED;
		}
	};

	public DiagnosticAgent(String agentName) {
		super(agentName);
		this.agentName = agentName;
		
		// Start this agent's server.
		new Thread(server).start();

		// Create and add team goals to this agent. Create takes as its
		// arguments the names of the elements in the data context that
		// are to be used as the inputs and outputs for the goal execution.
		// Execution will not proceed until all inputs have been assigned values.
		//
		say("Created DiagnosticAgent " + agentName);
		addGoal(diagnose.create(new String[] {DiagnosticTeam.FIND_FAULTS}, null));
		say("Assigned role FIND_FAULTS to " + this.agentName + "\n");
		
	}

	//
	// configureDiagnostics()
	// ======================
	Goal configureDiagnostics() {
		say("Setting up goal configureDiagnostics()");
		return new Goal (CONFIGURE_DIAGNOSTICS) {
			//
			// execute()
			// =========
			// This is where the real work for a goal gets done. GORITE
			// calls this method, it performs steps to try and fulfill the
			// goal and then exits with a Goal state. This is used to
			// determine if the goal has been completed or should be re-tried
			// later. If it needs to be retried, GORITE will make sure it
			// is re-scheduled.
			//
			public Goal.States execute(Data data) {
				int count = (int) data.getValue("count");
				int loadStatus = XMLErrorCodes.UNDEFINED;
								
				// Load the function block application information.  
				loadStatus = fbapp.load(applicationPath, applicationName + ".sys");
				if (loadStatus != XMLErrorCodes.LOADED) {
					System.err.println("Not loaded - " + fbapp.lastErrorDescription);
					count++;
					data.setValue("count", (int) count);
					return Goal.States.FAILED;
				} else {
					count = 0;
					data.setValue("count", (int) count);
					// Reinitialise the test sequence counter
					data.setValue("testSequence", 0);
					
				//	skills.rewireApp(server, fbapp);
					
				//	pause("Start FORTE now - press Enter to continue\n");
					
					return Goal.States.PASSED;
				}
			}
		};
	}

	//
	// identifyFault()
	// ===============
	Goal identifyFault() {
		say("Setting up goal identifyFault()");
		return new Goal (IDENTIFY_FAULT) {
			//
			// execute()
			// =========
			public Goal.States execute(Data data) {	
				int serverStatus = ExitCodes.UNDEFINED;
				int count = (int) data.getValue("count");
				NIOserverPacket packet = new NIOserverPacket();
				String dataPacket = "";	
				double testTemperature = 0.00;
				int testSequence = 0;
				
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
				
				// Monitor the network. Read the incoming SIFB data queues until they are empty before
				// releasing this GORITE execute() call.
				while (server.getQueueSize() > 0) {
					packet = server.getPacket();
					// RA_BRD
					System.err.println("AGENT_GATE_" + packet.SIFBinstanceID() + " [" + packet.dataPacket() + "] [" + server.getQueueSize() + "]");
					packetCount++;
					if (packetCount > 10) {
						dataPacket = "hello, agent..\n";
						server.sendPacket(dataPacket);
					}
				}
				
				count++;
				data.setValue("count", (int) count);
				if (count == MAX_COUNT) {
					//say("Goal passed\n");
					data.setValue("count", (int) 0);
					return Goal.States.PASSED;
				} else {
					//say("Goal stopped");
					try {
						TimeUnit.SECONDS.sleep((long) 1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return Goal.States.STOPPED;
				}
			}
		};
	}							
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
	private void displayFunctionBlocks(FunctionBlockApp fbapp) {
		FunctionBlock fb = new FunctionBlock();
		FunctionBlockEvent fbevent = new FunctionBlockEvent();
		FunctionBlockParameter fbparameter = new FunctionBlockParameter();
		FunctionBlockConnection fbconn = new FunctionBlockConnection();
		
		System.out.println("____________________________________________________________");
		System.out.println("\nFunction Blocks: " + "[" + fbapp.fbCount() + "]\n");
		
		for (int ptr = 0; ptr < fbapp.fbCount(); ptr++) {
			fb = fbapp.getfb(ptr);
			System.out.printf("%-10s %d\n",  "instance :", ptr);
			System.out.printf("%-10s %s\n",  "Name     :", fb.Name());     
			System.out.printf("%-10s %s\n",  "Type     :", fb.Type());
			System.out.printf("%-10s %s\n",  "Comment  :", fb.Comment());
			System.out.println();
			
			System.out.println("Events [" + fb.eventCount() + "]");
			for (int ptrEvent = 0; ptrEvent < fb.eventCount(); ptrEvent++) {
				fbevent = fb.Event(ptrEvent);
				System.out.printf("%15s %d\n",  "instance :", ptrEvent);
				System.out.printf("%15s %s\n",      "Name :", fbevent.EventName());
				
				switch (fbevent.EventType) {
				case EVENT_INPUT:
					System.out.printf("%15s %s\n",  "Event Type :", "Input");
					break;
				
				case EVENT_OUTPUT:
					System.out.printf("%15s %s\n",  "Event Type :", "Output");
					break;
				}
				
				for (int ptrWithVar = 0; ptrWithVar < fbevent.WithVarCount(); ptrWithVar++) {
					System.out.printf("%15s %s\n",  "With Var :", fbevent.WithVar(ptrWithVar));
				}
				System.out.println();
			}
			
			// The parameter list for this function block. 
			System.out.println("Parameters [" + fb.ParameterCount() + "]");
			for (int ptrParameter = 0; ptrParameter < fb.ParameterCount(); ptrParameter++) {
				fbparameter = fb.Parameter(ptrParameter);
				System.out.printf("%15s %d\n",   "instance : ", ptrParameter);
				System.out.printf("%15s %s\n",       "Name : ", fbparameter.Name());
				System.out.printf("%15s %s\n\n",    "Value : ", fbparameter.Value());	
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("____________________________________________________________");

		System.out.println("\nConnections [" + fbapp.ConnectionCount() + "]\n");
		for(int ptrConnection = 0; ptrConnection < fbapp.ConnectionCount(); ptrConnection++) {
			fbconn = fbapp.Connection(ptrConnection);
			System.out.println(fbconn.SourceFB() + " " + fbconn.SourceName() 
							   + " ---> " + 
							   fbconn.DestinationFB() + " " + fbconn.DestinationName() );
		}
		
		System.out.println("____________________________________________________________");
	}
	
	// 
	// rewireApp()
	// ===========
	public void rewireApp(FunctionBlockApp fbapp) {
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
				bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"CREATE\">" +
					       "<Connection Source=\"" + fbconnection.SourceFB() + "." + fbconnection.SourceName() + "\" " + 
					       "Destination=\"" + fbconnection.DestinationFB() + "." + fbconnection.DestinationName()  + "\"" + "/></Request>\n");
			}
			
			// Add the application start command. 
			bootfile.write("EMB_RES;<Request ID=\"" + requestID++ + "\" Action=\"START\"/>\n");
			bootfile.close();
		} else {
			System.err.println("Could not create forte.fboot." + bootfile.errorDescription());
		}
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
				int count = (int) data.getValue("count");
				say("Executing diagnoseFault() from within agent instance " + agentName + " [" + count + "]");
				
				// RA_BRD - temporary exit
				return Goal.States.PASSED;
				
			//	count++;
			//	data.setValue("count", (int) count);
			//	if (count == MAX_COUNT) {
			//		say("Goal passed\n");
			//		data.setValue("count", (int) 0);
			//		return Goal.States.PASSED;
			//	} else {
			//		say("Goal stopped");
					
					// RA_BRD - temporary exit
					
			//		return Goal.States.STOPPED;
			//	}
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
				int count = (int) data.getValue("count");
				say("Executing reportFault() from within agent instance " + agentName + " [" + count + "]");
				
				// RA_BRD - temporary exit
				return Goal.States.PASSED;
				
			//	count++;
			//	data.setValue("count", (int) count);
			//	if (count == MAX_COUNT) {
			//		say("Goal passed\n");
			//		data.setValue("count", (int) 0);
			//		return Goal.States.PASSED;
			//	} else {
			//		say("Goal stopped");
			//		return Goal.States.STOPPED;
			//	}
			}
		};
	}

	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable silence
	//
	private void say(String whatToSay){
		if(!silence) {
			System.err.println(whatToSay);
		}
	}

	//
	// pause()
	// =======
	private void pause(String prompt) {
		String userInput = "";
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		System.err.println(prompt);
		try {
			userInput = stdIn.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

