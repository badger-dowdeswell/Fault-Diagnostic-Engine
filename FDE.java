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
// AUT University - Barry Dowdeswell - 2019-2020
//
// Revision History
// ================
// 24.04.2019 BRD Redevelopment based on the original 10.12.2017 version that
//                incorporates all of Dennis Jarvis's suggestions. 
// 23.05.2019 BRD Incorporated FORTE-specific libraries and diagnostic functions.
// 03.07.2019 BRD Reconfigured the system to support instanced agents.	
// 10.02.2020 BRD Migrated multiAgentSystem_03 into the first production Fault
//                Diagnostic Engine.
//
package fde;

import com.intendico.gorite.*;
import com.intendico.gorite.addon.*;
import static fde.Constants.*;
	
//
// main()
// ======
public class FDE {
	public static String appVersion = "2.0";
	private static boolean isSilent = false;

	//
	// main()
	// ======
    public static void main(String[] args) throws Throwable {
		say("\nFAULT DIAGNOSIS ENGINE version " + appVersion);
				
		// Start building our teams in GORITE
		say("\nStarting GORITE");
		DiagnosticTeam diagnosticTeam = new DiagnosticTeam("diagnosticTeam");
	
		// Start the team of agents hunting 
		// for and diagnosing faults.
		diagnosticTeam.findFaults();
		say("Exiting Fault Diagnostic Engine\n");
	}
			
	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable silence
	//
	private static void say(String whatToSay){
		if(!isSilent) {
			System.err.println(whatToSay);
		}
	}	
}