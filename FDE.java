//
// FAULT DIAGNOSTIC ENGINE
// =======================
// This Fault Diagnostic Engine implements agent-based fault diagnostics. It
// creates agents, hosted and supported within the GORITE Multi-Agent System
// framework. Refer to the documentation in the Fault Diagnosis System 
// Software Architecture Document for a deeper explanation of how the  
// agents are constructed.
// 
// This version is a port of the original multiAgentSystem_03 prototype. It
// implements hard-coded fault-finding tasks using a single
// agent. It incorporates the libraries that allow the the agents to 
// interact with the target Function Block Application that is being
// monitored or diagnosed.
//
// AUT University - 2019-2021
//
// Revision History
// ================
// 24.04.2019 BRD Redevelopment based on the original 10.12.2017 version that
//                incorporates all of Dennis Jarvis's suggestions. 
// 23.05.2019 BRD Incorporated FORTE-specific libraries and diagnostic functions.
// 03.07.2019 BRD Reconfigured the system to support instanced agents.	
// 10.02.2020 BRD Migrated multiAgentSystem_03 into the first production Fault
//                Diagnostic Engine.
// 24.03.2021 BRD Integrate latest multi-agent extensions and upgrade to version 2.2	
//
package fde;

public class FDE {
	public static String appVersion = "2.2";
	private static boolean isSilent = false;
	
	// Runtime configuration
	// =====================
	private static String serverAddress = "127.0.0.3";
	private static int serverListenerPort = 62503;
	
	static FunctionBlockApp fbapp = new FunctionBlockApp();
	static Beliefs beliefs = new Beliefs();
	
	static DiagnosticPoints dps = new DiagnosticPoints();
	
	// Create a runnable instance of the Non-Blocking I/O server. This will
	// manage all communications from the diagnostic points in the function 
	// block application between the agents. Note that the engine only 
	// creates one server. 
	//
	// RA_BRD - how should we define the server configuration? It is 
	// part of this diagnostic engine instance (and perhaps this is
	// the server used by this team of agents, so it is perhaps higher
	// up the hierarchy?
	//
 	static NIOserver server = new NIOserver(serverAddress, serverListenerPort); 
 
	//
	// main()
	// ======
	// The engine has a primary agent who acts as the
	// team coordinator, delegating goals to individual
	// agents and managing their needs. Each agent pursues 
	// its own tasks to achieve their current goal. As each 
	// goal is completed, the team coordinator responds and 
	// then assigns new goals if they are required.
	//
    public static void main(String[] args) throws Throwable {
		say("\nFault Diagnostic Engine version " + appVersion);
		
		DiagnosticTeam diagnosticTeam = new DiagnosticTeam("diagnosticTeam",
				                                           server, dps, fbapp, beliefs);
		diagnosticTeam.manageTeam();
		
		say("Exiting Fault Diagnostic Engine\n");
		System.exit(0); 
	}
			
	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable silence
	//
	private static void say(String whatToSay){
		if(!isSilent) {
			System.out.println(whatToSay);
		}
	}	
}