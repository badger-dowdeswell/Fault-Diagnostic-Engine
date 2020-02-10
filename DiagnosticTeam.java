//
// DIAGNOSTIC TEAM
// ===============
// This class allows the behavior of the individual diagnostic
// agents to be modeled as a team.
//
// AUT University - 2019-2020
//
// Revision History
// ================
// 23.05.2019 BRD Original version. 
//
package fde;
import com.intendico.gorite.*;
import com.intendico.gorite.addon.Perceptor;
import static fde.Constants.*;

public class DiagnosticTeam extends Team {
	// used to turn off and on console messages during development.e
	private boolean isSilent = false;
	public static int MAX_COUNT = 5;
	public static final String FIND_FAULTS = "FIND_FAULTS";
	
	// Constructor
	public TaskTeam diagnosticTeam = new TaskTeam() {{
		// A diagnoser is a team member of the diagnosticTeam Team who had the
		// roles of configuring the diagnostic system, identifying, diagnosing
		// and reporting faults.
		say("Setting up TaskTeam diagnosticTeam.\nAdding roles for a diagnoser.");
		addRole(new Role(DIAGNOSER, new String[] {
				CONFIGURE_DIAGNOSTICS, IDENTIFY_FAULT, DIAGNOSE_FAULT, REPORT_FAULT
		}));
	}};

	//
	// DiagnosticTeam()
	// ================
	public DiagnosticTeam(String teamName) {
		super(teamName);
		say("Creating diagnostic agents");
		DiagnosticAgent marvin = new DiagnosticAgent("marvin");
		
		setTaskTeam("diagnose faults", new TaskTeam() {{
			addRole(new Role(DIAGNOSER, new String[] {FIND_FAULTS}));
		}});
		addGoal(marvin.configureDiagnostics());     
		addGoal(marvin.identifyFault());
		addGoal(marvin.diagnoseFault());
		addGoal(marvin.reportFault());
		say("Team created and ready\n");
	}
		
	//
	// findFaults()
	// ============
	// Performs a set of goals sequentially to configure the system, find faults,
	// diagnose them and then report them.
	//
	public boolean findFaults() { 	
		Data data = new Data();
		say("findFaults()");
		data.create("count");
		data.setValue("count", 0);
		data.create("testSequence");
		data.setValue("testSequence", 0);
		
     // dhj: new LoopGoal(FIND_LEAK) creates an empty loop goal called
     // find leak which, when performed, does nothing. Either pass in
     // a goal instance (ie findLeak()) or better, pass in a BDIGoal instance
     // as below. The BDIGoal is executed by looking up the name provided
     // in the team's default capability. It is found, as it has been added
     // via an addGoal() invocation in the constructor.
     //
     performGoal(new BDIGoal(CONFIGURE_DIAGNOSTICS), "CONFIGURE_DIAGNOSTICS", data);
     performGoal(new BDIGoal(IDENTIFY_FAULT), "IDENTIFY_FAULT", data);
     performGoal(new BDIGoal(DIAGNOSE_FAULT), "DIAGNOSE_FAULT", data);
     performGoal(new BDIGoal(REPORT_FAULT), "REPORT_FAULT", data);
     return true;
	}
	
	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable silence
	//
	private void say(String whatToSay){
		if(!isSilent) {
			System.err.println(whatToSay);
		}
	}	
}
