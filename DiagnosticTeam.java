//
// DIAGNOSTIC TEAM
// ===============
// This class allows the behavior of the individual diagnostic
// agents to be modeled as a team.
//
// AUT University - 2019-2021
//
// Revision History
// ================
// 23.05.2019 BRD Original version. 
// 24.03.2021 BRD Brought in the manage team capabilities from
//                The Dam Scenario. This completes the multi-
//                threading of the agents.
//
package fde;

import static fde.Constants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.intendico.gorite.BDIGoal;
import com.intendico.gorite.Data;
import com.intendico.gorite.Goal;
import com.intendico.gorite.Team;

public class DiagnosticTeam extends Team {
	// used to turn off and on console messages during development.
	public static final int MAX_AGENTS = 5;
	private boolean isSilent = false;
	
	private String homeDirectory = System.getProperty("user.home");
	private String applicationPath = "";
	private String applicationName = "";
	
	NIOserver server;
    DiagnosticPoints dps;
    FunctionBlockApp fbapp;
    Beliefs beliefs;
    
	DiagnosticAgentCapabilities skills = new DiagnosticAgentCapabilities();
	
	DiagnosticAgent[] agent = new DiagnosticAgent[MAX_AGENTS];

	//
	// Constructor
	// ==========
	public TaskTeam diagnosticTeam = new TaskTeam() {{
		// An agent is a team member of the diagnosticTeam Team who
		// has the assigned roles of configuring the diagnostic system,
		// identifying, diagnosing, and reporting faults.
		//
		addRole(new Role(MANAGER, new String[] {
			MANAGE_TEAM
		}));
	}};
	
	//
	// DiagnosticTeam()
	// ================
	// Constructs the team, launches each agent and then manages them.
	// Initially each agent is awake, but idle, until the engine invokes
	// the manageAgent() goals for the team coordinator.
	//
	public DiagnosticTeam(String teamName, NIOserver server, DiagnosticPoints dps, FunctionBlockApp fbapp, Beliefs beliefs) {
		super(teamName);
		
		this.server = server;
		this.dps = dps;
		this.fbapp = fbapp;
		this.beliefs = beliefs;
		
		new Thread(this.server).start(); 
		
		// RA_BRD should instantiate MAX_AGENTS.
		agent[0] = new DiagnosticAgent("alpha", server, dps);
		agent[1] = new DiagnosticAgent("beta", server, dps);
	
	//	agent[0] = new DiagnosticAgent("marvin", server, dps);

		// RA_BRD should instantiate MAX_AGENTS
		new Thread(agent[0]).start();
		new Thread(agent[1]).start();
		
		addGoal(manageAgent0());
		addGoal(manageAgent1());
	}	
	
	//
	// manageTeam()
	// ============
	// Management is performed by the main or coordinating agent by 
	// executing this loop. It executes a local set of tasks, with one 
	// GORITE performGoal() for each agent. 
	// 
	// Basically, the coordinator looks in on each agent sequentially, 
	// assigning tasks to them when they are found to be idle. The
	// coordinator then leaves them to get on with it. Like all good managers, 
	// it performs a duty-of-care without micromanaging everything the
	// agent does. This is the primary mechanism that allows the engine
	// to support multiple agents, each performing its own set of tasks
	// to work towards completing its goals.
	//
	public boolean manageTeam() {
		boolean isManaging = true;
		Data data0 = new Data();
		Data data1 = new Data();
		String currentGoal = "";
		
		data0.setValue("currentGoal", UNDEFINED_GOAL);
		data1.setValue("currentGoal", UNDEFINED_GOAL);
		
		//currentGoal = data0.getValue("currentGoal").toString();
		//say(currentGoal);	
		
		applicationPath = homeDirectory + "/Development/4diac/Smart_Grid_02/";
		applicationName = "Smart_Grid_02";
		
	//	applicationPath = homeDirectory + "/Development/4diac/HVACsim/";
	//	applicationName = "HVACsim";
		
	//	applicationPath = homeDirectory + "/Development/4diac/HVACrewiring/";
	//	applicationName = "HVACrewiring";
	
		if (configureDiagnostics(applicationPath, applicationName, true)) {
			say("rewired");
		}
		
		say("Managing team.");
		// RA_BRD should manage MAX_AGENTS.
		while (isManaging) {
			performGoal(new BDIGoal(MANAGE_AGENT_0), "MANAGE_AGENT_0",
					    data0);
			performGoal(new BDIGoal(MANAGE_AGENT_1), "MANAGE_AGENT_1",
						data1);
		}
		return true;
	}
	
	//
	// configureDiagnostics()
	// ======================
	private boolean configureDiagnostics(String applicationPath, String applicationName, boolean showLoadStatus) {
		int loadStatus = XMLErrorCodes.UNDEFINED;
		boolean status = false;

		// Load the function block application information.  
		beliefs.create("AppName", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, applicationName);
		beliefs.create("AppPath", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, applicationPath);
			           
		loadStatus = fbapp.load(applicationPath, applicationName + ".sys");
			
		if (loadStatus != XMLErrorCodes.LOADED) {
			if (showLoadStatus) {
				System.out.println("Function block application " + applicationName + ".sys could not be loaded.\n" + fbapp.lastErrorDescription);
			}	
			beliefs.create("LoadStatus", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.FALSE, 
				           "Function block application " + applicationName + ".sys could not be loaded.\n" + fbapp.lastErrorDescription);
		
		} else {
			beliefs.create("LoadStatus", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, "Application loaded successfully.");
			
			// Create and deploy the diagnostic test harness.
			if (skills.createHarness(fbapp, dps, applicationPath, server)) {
				beliefs.create("DeployedStatus", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.TRUE, "Application deployed successfully.");
				fbapp.displayFunctionBlocks(fbapp);
								
				if (dps.count() > 0) {
					// Diagnostic points were deployed in the harness. Wait for the function block
					// application to start before starting other fault-finding goals.
					if (dps.count() == 1) {
						say("\nCreated a diagnostic harness with one diagnostic point.");
					} else {
						say("\nCreated a diagnostic harness with " + dps.count() + " diagnostic points."); 
					}
					say(" Waiting for FORTE to start\n");
					
					// RA_BRD build a deployment timeout into this if forte does not launch successfully.
					while (server.ConnectionCount() == 0) {
						delay(100);
					}
				} else {
					say("\nCreated diagnostic harness");
				}
				status = true;

			} else {
				fbapp.displayFunctionBlocks(fbapp);
				beliefs.create("AppDeployed", BeliefTypes.SYSTEM_UNDER_DIAGNOSIS, VeracityTypes.FALSE, 
				        	   "Could not create diagnostic harness. " + skills.lastErrorDescription());
				if (showLoadStatus) {
					System.out.println("\nCould not create diagnostic harness:\n" + skills.lastErrorDescription());
				}	
			}
		}
		return status;
	}
	
	//
	// manageAgent0()
	// ==============
	Goal manageAgent0() {
		return new Goal (MANAGE_AGENT_0) {
			//
			// execute()
			// =========
			public Goal.States execute(Data data) {
				Goal.States goalState = Goal.States.PASSED;
				String currentGoal = data.getValue("currentGoal").toString();
				
				if (agent[0].AgentState() == AgentStates.EXECUTING) {
					// Leave the agent to get on with the task; it is busy.
					// Ask GORITE to come back to this goal later.
					sleep(1000);
					goalState = Goal.States.PASSED;
					
				} else if (agent[0].AgentState() == AgentStates.IDLE) {
					if (agent[0].GoalName() != UNDEFINED_GOAL) {
						// The agent had a goal which has now been either completed or stopped.
						// so decide what to do next.
						//taskPointer[0]++;
						//switch (taskPointer[0]) {
						//case 1:
						//agent[0].GoalName(DIAGNOSE_FAULTS);
						agent[0].GoalName(WATCH_FOR_FAULTS);
						goalState = agent[0].GoalState();
						
					} else {
						if (agent[0].GoalState() == Goal.States.FAILED) {
							agent[0].GoalName(DIAGNOSE_FAULTS);					
							agent[0].AgentState(AgentStates.EXECUTING);
						} else {
							// The agent is awaiting a new goal to execute. 
							agent[0].GoalName(WATCH_FOR_FAULTS);					
							agent[0].AgentState(AgentStates.EXECUTING);
						}	
		     			goalState = Goal.States.PASSED;
					}	
				}
				return goalState;
			}
		};
	}	
	
	//
	// manageAgent1()
	// ==============
	Goal manageAgent1() {
		return new Goal (MANAGE_AGENT_1) {
			//
			// execute()
			// =========
			public Goal.States execute(Data data) {
				Goal.States goalState = Goal.States.PASSED;
				String currentGoal = data.getValue("currentGoal").toString();
				
				if (agent[1].AgentState() == AgentStates.EXECUTING) {
					// Leave the agent to get on with the task; it is busy.
					// Ask GORITE to come back to this goal later.
					sleep(1000);
					goalState = Goal.States.PASSED;
					
				} else if (agent[1].AgentState() == AgentStates.IDLE) {
					if (agent[1].GoalName() != UNDEFINED_GOAL) {
						// The agent had a goal which has now been either completed or stopped.
						// so decide what to do next.
						if (agent[1].GoalState() == Goal.States.FAILED) {
							agent[1].GoalName(DIAGNOSE_FAULTS);					
							agent[1].AgentState(AgentStates.EXECUTING);
						} else {
							// The agent is awaiting a new goal to execute. 
							agent[1].GoalName(WATCH_FOR_FAULTS);					
							agent[1].AgentState(AgentStates.EXECUTING);
						}	
						goalState = Goal.States.PASSED;
						
						//agent[1].GoalName(WATCH_FOR_FAULTS);
						//goalState = agent[0].GoalState();
						
					} else {
						if (agent[1].GoalState() == Goal.States.FAILED) {
							agent[1].GoalName(DIAGNOSE_FAULTS);					
							agent[1].AgentState(AgentStates.EXECUTING);
						} else {
							// The agent is awaiting a new goal to execute. 
							agent[1].GoalName(WATCH_FOR_FAULTS);					
							agent[1].AgentState(AgentStates.EXECUTING);
						}	
		     			goalState = Goal.States.PASSED;
					}	
				}
				return goalState;
			}
		};
	}	
	
		
		//say("Setting up TaskTeam diagnosticTeam.\nAdding roles for a diagnoser.");
		//addRole(new Role(DIAGNOSER, new String[] {
		//		CONFIGURE_DIAGNOSTICS, IDENTIFY_FAULT, DIAGNOSE_FAULT, REPORT_FAULT

	
		
		// ==================
		
	//	DiagnosticAgent marvin = new DiagnosticAgent("marvin");
	//	new Thread(marvin).start();
		
	//	DiagnosticAgent jane = new DiagnosticAgent("jane");
	//	new Thread(jane).start();
	
	//	setTaskTeam("diagnose faults", new TaskTeam() {{
	//		addRole(new Role(DIAGNOSER, new String[] {FIND_FAULTS}));
	//	}});
	//	addGoal(marvin.configureDiagnostics());     
	//	addGoal(marvin.identifyFault());
	//	addGoal(marvin.diagnoseFault());
	//	addGoal(marvin.reportFault());
		
	//	addGoal(jane.configureDiagnostics());



	//
	// findFaults()
	// ============
	// Performs a set of goals sequentially to configure the system, find faults,
	// diagnose them and then report them.
	//
	public boolean findFaults() { 
		boolean goalStatus = false;
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
	 goalStatus = performGoal(new BDIGoal(CONFIGURE_DIAGNOSTICS), "CONFIGURE_DIAGNOSTICS", data);
     if (goalStatus) {
    	 // The configuration worked, so start monitoring and perhaps diagnosis.
    	 performGoal(new BDIGoal(IDENTIFY_FAULT), "IDENTIFY_FAULT", data);
    	 performGoal(new BDIGoal(DIAGNOSE_FAULT), "DIAGNOSE_FAULT", data);
     }	 
     performGoal(new BDIGoal(REPORT_FAULT), "REPORT_FAULT", data);
     return true;
	}
	
	// 
	// pause()
	// =======
	// Outputs a console message and waits for the user to press any key
	// before continuing. 
	//
	@SuppressWarnings("unused")
	private void pause(String prompt) {
		String userInput = "";
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		System.out.println(prompt + " press a key to continue.");
		try {
			userInput = stdIn.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// 
	// sleep()
	// =======
	// Put this thread to sleep for a specified number of milliseconds.
	//
	private void sleep(int sleepTime) {
		try {			
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
		}
	}
	
	//
	// delay()
	// =======
	// Delay for a specified number of milliseconds. This is an
	// alternative to the sleep() method.
	//
	@SuppressWarnings("unused")
	private void delay(int milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep((long) milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}
	
	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable isSilent.
	//
	private void say(String whatToSay){
		if(!isSilent) {
			System.out.println(whatToSay);
		}
	}	
}
