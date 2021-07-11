//
// DIAGNOSTIC SCRIPTS
// ==================
// Temporary library of diagnostic scripts. These will be
// deprecated when the diagnostic routines in the diagnostic
// packages can be loaded, compiled and executed dynamically
// at run-time.
//
// AUT University - 2020
//
// Revision History
// ================
// 06.08.2020 BRD Original version

package fde;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Scripts {
	boolean isSilent = false;
	
	//
	// alphaMonitor
	// ===========
	// This is the Monitoring script for the alpha agent. It uses this to generate test currents and
	// voltages for the system.
	//
	public Belief alphaMonitor(String agentName, DiagnosticPoints dps, NIOserver server) {
//		// =============================================================================
//		// Pre-processor generated code
		Belief belief = new Belief();
		belief.BeliefType(BeliefTypes.DYNAMIC);
		belief.Veracity(VeracityTypes.UNDETERMINED);
		
		DiagnosticPoint CURRENT_A_TO_D_REQ = dps.map("CURRENT_A_TO_D", "REQ");
	//	DiagnosticPoint IED50_51_CNF = dps.map("IED50_51", "CNF");
	//	DiagnosticPoint IED50_51_RESET_OUT = dps.map("IED50_51", "RESET_OUT");
	//	DiagnosticPoint IED50_51_CLEAR = dps.map("IED50_51", "CLEAR");
		// =============================================================================
		
		double current = 0;
		say(agentName + " monitoring");
		
		// Generating normal current readings
		current = 10.0; 
		for (int iTest = 0; iTest < 5; iTest++) {
			CURRENT_A_TO_D_REQ.trigger(current);		
			delay(5000);
			current = current + 10;
		}
		
		// Generate an overcurrent
	//	current = 30;
	//	CURRENT_A_TO_D_REQ.trigger(current);
		
		pause("paused");
		
		return belief;	
	}	
	
	//
	// betaMonitor
	// ===========
	// This is the Monitoring script for the beta agent. It uses to watch for how the IED handles 
	// overcurrent situations.
	//
	public Belief betaMonitor(String agentName, DiagnosticPoints dps, NIOserver server) {
//		// =============================================================================
//		// Pre-processor generated code
		Belief belief = new Belief();
		belief.BeliefType(BeliefTypes.DYNAMIC);
		belief.Veracity(VeracityTypes.UNDETERMINED);
		
	//	DiagnosticPoint CURRENT_A_TO_D_REQ = dps.map("CURRENT_A_TO_D", "REQ");
		DiagnosticPoint IED50_51_REQ = dps.map("IED50_51", "REQ");
		DiagnosticPoint IED50_51_CNF = dps.map("IED50_51", "CNF");
		DiagnosticPoint IED50_51_RESET_OUT = dps.map("IED50_51", "RESET_OUT");
		DiagnosticPoint IED50_51_CLEAR = dps.map("IED50_51", "CLEAR");
		// =============================================================================
		
		double current = 0;
		long start_overcurrent = 0;
		long end_overcurrent = 0;
		double tripTime = 0;
		double calcTripTime = 0;
		boolean inTrip = false;
		double Icurrent = 0.0;
		int cnt2 = 0;
	
		double accTripTime = 0;
		
		say(agentName + " monitoring");
		
		// Retrieve the IED configuration settings.
		double timeDialSetting = IED50_51_REQ.paramDbl("time_dial_setting");
		int curveType = IED50_51_REQ.paramInt("curve_type");
		double Ipickup = IED50_51_REQ.paramDbl("Ipickup_current");
		
		if (IED50_51_REQ.readEvent(0)) {
			// The IED has received a new current read event.
			Icurrent = IED50_51_REQ.value();
			say(agentName + " Icurrent = " + Icurrent + "A.");
			if (Icurrent > Ipickup) {				
	            // This should trip the IED.   
	            start_overcurrent = IED50_51_REQ.timestamp();
	            calcTripTime = calcInverseOvercurrent(curveType, Ipickup, Icurrent, timeDialSetting);
	            say("Expected trip_time = " + calcTripTime + " ms.");
	            delay((int) (calcTripTime + 10)); 
	            if (IED50_51_CNF.readEvent(IED50_51_REQ.timestamp())) {
	                say(agentName + ": " + "Overcurrent IED has tripped on Icurrent " + Icurrent + " A. (CNF event fired)\n");
	                end_overcurrent = IED50_51_CNF.timestamp();
	                tripTime = (end_overcurrent - start_overcurrent);
	                say(agentName + ":" + "Trip time " + tripTime + " ms. (Expected " + calcTripTime + " ms)\n");	
	                
	                if (Icurrent == 30) {
	                	// Simulate a fault
	                	tripTime = tripTime + 50;
	                }
	                if (Math.abs(calcTripTime - tripTime) < 3) {
	                	say("tripped correctly");
	                	belief.Veracity(VeracityTypes.TRUE);
	                	belief.add("IED50_51 tripped correctly for Icurrent " + Icurrent + "A in " + tripTime + "ms.");
	                } else {
	                	say("tripped incorrectly !!");
	                	belief.Veracity(VeracityTypes.FALSE);
	                	belief.add("IED50_51 trip incorrectly in" + tripTime + "ms for Icurrent " + Icurrent + "A.");	
	                }
	               
	            }  
	            // Reset the IED
	            IED50_51_CLEAR.trigger();
			}
			
		} else {
			delay(2000);
		}	
		return belief;	
	}	
	
	//
	// betaOvercurrent()
	// =================
	public Belief betaOvercurrent(String agentName, DiagnosticPoints dps, NIOserver server) {
		// Pre-processor generated code
		Belief belief = new Belief();
		belief.BeliefType(BeliefTypes.DYNAMIC);
		belief.Veracity(VeracityTypes.UNDETERMINED);

		
		return belief;
	}
	
	//
	// ANSI Time Overcurrent Curves
	// ============================
	// Overcurrent trip times are determined either
	// by specifying a fixed-time to trip after a 
	// overcurrent is detected, or by applying
	// an appropriate algorithm. 
	//
	// The curve is specified by setting the curve_type 
	// input parameter on the IED 50/51 function block.
	//
	class ANSI_Time_Curves {
		static final int CURVE_TYPE_SHORT_TIME = 0;      // Fixed time specified in milliseconds.		                        
		static final int CURVE_TYPE_NORMAL_INVERSE = 1;         
		static final int CURVE_TYPE_VERY_INVERSE = 2;    
		static final int CURVE_TYPE_EXTREME_INVERSE = 3; 
		static final int CURVE_TYPE_LONG_TIME_INVERSE = 4;
		static final int CURVE_TYPE_SHORT_TIME_INVERSE = 5;
		static final int CURVE_TYPE_EXTREME_INVERSE_C = 6;
	}
	
	//
	// calcInverseOvercurrent()
	// ========================
	// Calculates the trip time in milliseconds for a specified overcurrent. The
	// algorithm uses a different formulae for each type of curve based on 
	// ANSI standard for overcurrent protection.
	//
	// curveType         Specifies which ANSI-standard Time-overcurrent curves is to be used.
	//                   The value is one of the ANSI_Time_Curve constants. 
	//
	// Ipickup           The current limit in amperes that defines when there is an overcurrent
	//                   fault in-progress.
	//
	// Icurrent          The measured current in amperes. This must always be greater than Ipickup.
	//                   This algorithm will return a result of zero if Icurrent is less than 
	//                   or equal to Ipickup. 
	//
	// timeDialSetting   The ANSI time dial setting. This is a number between 0.5 and 15. Refer to
	//                   documentation for a detailed explanation of this parameter.
	// 
	private double calcInverseOvercurrent(int curveType, double Ipickup, double Icurrent, double timeDialSetting) {
		double tripTime = 0;
		
		double Mvalue = Icurrent / Ipickup;
		if (Mvalue > 0.0) {
			switch (curveType) {
			case ANSI_Time_Curves.CURVE_TYPE_NORMAL_INVERSE: 
				// U.S. Normal Inverse (U2)
				tripTime = timeDialSetting * (0.18 + (5.95 / ((Mvalue * Mvalue) - 1)) );
				break;
			
			case ANSI_Time_Curves.CURVE_TYPE_VERY_INVERSE:
				// U.S. Very Inverse (U3)
				tripTime = timeDialSetting * (0.0963 + (3.88 / ((Mvalue * Mvalue) - 1)) );
				break;
			
			case ANSI_Time_Curves.CURVE_TYPE_EXTREME_INVERSE:
				// U.S. Extreme Inverse (U4)
				tripTime = timeDialSetting * (0.0352 + (5.67 / ((Mvalue * Mvalue) - 1)) );
				break;
			
			case ANSI_Time_Curves.CURVE_TYPE_LONG_TIME_INVERSE:
				// Long-time inverse (C4)
				tripTime = timeDialSetting * ((120 / ((Mvalue * Mvalue) - 1)) );
				break;
				
			case ANSI_Time_Curves.CURVE_TYPE_SHORT_TIME_INVERSE:
				// Short-time inverse (C5)
				tripTime = timeDialSetting * ((0.05 / ((Mvalue * Mvalue) - 1)) );
				break;
			
			case ANSI_Time_Curves.CURVE_TYPE_EXTREME_INVERSE_C:
				// U.S. Extremely Inverse (C3)
				tripTime = timeDialSetting * (80.0 / ((Mvalue * Mvalue) - 1) );
				break;
			}
		}
		return tripTime * (double) 100;
	}
	
	//
	// Overcurrent
	// ===========
	public Belief Overcurrent(String agentName, DiagnosticPoints dps, NIOserver server) {
//		// =============================================================================
//		// Pre-processor generated code
		Belief belief = new Belief();
		belief.BeliefType(BeliefTypes.DYNAMIC);
		belief.Veracity(VeracityTypes.UNDETERMINED);
		
		DiagnosticPoint IED50_51_REQ = dps.map("IED50_51", "REQ");
		DiagnosticPoint IED50_51_CNF = dps.map("IED50_51", "CNF");
		DiagnosticPoint IED50_51_RESET_OUT = dps.map("IED50_51", "RESET_OUT");
		DiagnosticPoint IED50_51_CLEAR = dps.map("IED50_51", "CLEAR");
//				
//		DiagnosticPoint IED64_voltage = dps.map("IED64", "voltage");		
//		DiagnosticPoint IED64_CNF = dps.map("IED64", "CNF");
//		DiagnosticPoint IED64_RESET_OUT = dps.map("IED64", "RESET_OUT");
//		//DiagnosticPoint trip_multiplexer_CB_TRIP = dps.map("trip_multiplexer", "CB_TRIP");
//		//DiagnosticPoint trip_multiplexer_CB_RESET = dps.map("trip_multiplexer", "CB_RESET");
		
		// belief.Veracity(VeracityTypes.FALSE);
		// belief.add("Error code: " + TEMPERATURE_CNF_TEMP.value());

//		
//		// =============================================================================
		boolean status = true;
		
		//
		// Overcurrent fault diagnostics
		// =============================
		long start_overcurrent = 0;
		long end_overcurrent = 0;
		double tripTime = 0;
		boolean inTrip = false;
		double Icurrent = 0.0;
		int cnt2 = 0;
//		
//		// IED50_51 Overcurrent configuration parameters.
		int curveType = 2;
		double Ipickup = 4.5;
		double timeDialSetting = 5;
		double accTripTime = 0;
		
		IED50_51_REQ.gateClose();
//		//trip_multiplexer_CB_TRIP.gateClose();

		Icurrent = 1000;
		tripTime = calcInverseOvercurrent(curveType, Ipickup, Icurrent, timeDialSetting);
		say("Curve type = " + curveType + ". Ipickup = " + Ipickup + "A. Icurrent = " + Icurrent + "A.\n"
			+ "Expected trip_time = " + tripTime + "ms.");
		// say(agentName + ": " + "Icurrent = " + Icurrent + " A");	
		pause("press enter to start.");
		
		for (int testCnt = 1; testCnt <= 1000; testCnt++) {
			if (Icurrent > Ipickup && !inTrip) {
				// This should trip the IED	
				inTrip = true;
				tripTime = calcInverseOvercurrent(curveType, Ipickup, Icurrent, timeDialSetting);
			//	say("Expected trip_time = " + tripTime + " ms.");
				IED50_51_REQ.trigger(Icurrent);
				start_overcurrent = IED50_51_REQ.timestamp();
			
			} else if (Icurrent < Ipickup){
				// This should not trip the IED.
				IED50_51_REQ.trigger(Icurrent);
				//Icurrent = Icurrent + (double) 0.1;
			}
		
			if (IED50_51_CNF.readEvent(IED50_51_REQ.timestamp())) {
			//	say(agentName + ": " + "Overcurrent IED has tripped on Icurrent " + Icurrent + " A. (CNF event fired)\n");
				end_overcurrent = IED50_51_CNF.timestamp();
				//say(agentName + ":" + "Trip time " + (end_overcurrent - start_overcurrent) + " ms. (Expected " + tripTime + " ms)\n");
				accTripTime = accTripTime + (end_overcurrent - start_overcurrent);
				
				System.out.println(end_overcurrent - start_overcurrent);
				
				cnt2++;
				if (cnt2 > 100) {
					break;
				}
				
				//pause(agentName + ": " + "Press Enter to reset the IED:");
				IED50_51_CLEAR.trigger();
				
				// Reset for the next test using an offset.
				inTrip = false;
				//Icurrent = Icurrent + (double) 0.1;
			}	
			
			if (IED50_51_RESET_OUT.readEvent(IED50_51_CLEAR.timestamp())) {
			//	say(agentName + ": " + "Overcurrent RESET_OUT event fired");
			}	
			delay(250);
		}
		
		say("Average trip time = " + (accTripTime/100));
	
		pause(agentName + ": " + "Overcurrent diagnostic finished");

//		
//		// Earth Fault diagnostics
//		// =======================
//		double voltage = 19; // Volts
//		
//		IED64_voltage.gateClose();
//		for (int iCnt = 1; iCnt <= 100; iCnt++) {
//			if (voltage > 20) {
//				voltage = 18;
//			}
//			
//			IED64_voltage.trigger(voltage);
//		
//	//		if (IED64_trip.readBoolean()) {
//	//			say("IED64 trip event fired\n");
//	//			//say("timestamp [" + p)
//	//			voltage = 0;
//	//		} 
//			
//			if (IED64_CNF.readEvent()) {
//				say("IED64 CNF fired\n");
//			}
//			
//			if (IED64_RESET_OUT.readEvent()) {
//				say("IED64 RESET_OUT fired\n");
//				break;
//			}
//			
////			if (trip_multiplexer_CB_TRIP.readEvent()) {
////				say("trip_multiplexer CB_TRIP fired\n");
////			}
//			
////			if (trip_multiplexer_CB_RESET.readEvent()) {
////				say("trip_multiplexer CB_RESET fired\n");
////			}
//			
//			voltage++;
//			delay(50);
//		}
//		pause("finished");
		return belief;
	}	
	
	
	//
	// tripMux()
	// =========
	public boolean tripMux(String agentName, DiagnosticPoints dps, NIOserver server) {
		// =============================================================================
		// Pre-processor generated code
		DiagnosticPoint trip_multiplexer_CB_TRIP = dps.map("trip_multiplexer", "CB_TRIP");
		DiagnosticPoint trip_multiplexer_CB_RESET = dps.map("trip_multiplexer", "CB_RESET");
		
		// =============================================================================
		boolean status = true;
		
//		//
//		// trip_multiplexer fault diagnostics
//		// ==================================
//		trip_multiplexer_CB_TRIP.gateClose();
//		
//		for (int testCnt = 1; testCnt <= 1000; testCnt++) {
//			if (trip_multiplexer_CB_TRIP.readEvent()) {
//				say(agentName + ": " + "trip_multiplexer CB_TRIP event fired\n");
//			}
//			
//			if (trip_multiplexer_CB_RESET.readEvent()) {
//				say(agentName + ": " + "trip_multiplexer CB_RESET event fired\n");
//			}
//			
//			delay(100);
//		}
//		
//		pause("trip_multiplexer diagnostic finished");
		return true;
	}
	
	//
	// smartGrid()
	// ===========
	public boolean smartGrid(DiagnosticPoints dps, NIOserver server) {
		// =============================================================================
		// Pre-processor generated code
		DiagnosticPoint earth_fault_voltage = dps.map("earth_fault", "voltage");
		DiagnosticPoint earth_fault_trip = dps.map("earth_fault", "trip");	
		DiagnosticPoint earth_fault_RESET_OUT = dps.map("earth_fault", "RESET_OUT");	
		// =============================================================================
		boolean status = true;
///*		double voltage = 19;
//		
//		earth_fault_voltage.gateClose();
//		for (int iCnt = 1; iCnt <= 100; iCnt++) {
//			if (voltage > 20) {
//				voltage = 18;
//			}
//			
//			earth_fault_voltage.trigger(voltage);
//		
//			if (earth_fault_trip.readBoolean()) {
//				say("trip event fired\n");
//				//say("timestamp [" + p)
//				voltage = 0;
//			} 
//			
//			if (earth_fault_RESET_OUT.readEvent()) {
//				say("RESET_OUT fired\n");
//				break;
//			}
//			voltage++;
//			delay(100);
//		}
//		pause("finished");*/
		return status;
	}		
	
	// =======================================================================================================
	// Overcurrent calculation code
	
	//	for (Icurrent = Ipickup + 0.1	; Icurrent < 21; Icurrent = Icurrent + 0.1) {
	//		say(" " + Icurrent);
	//	}
	//	
	//	// Calculate the test curves.
	//	say("Calculating test curves");
	//	
	//	for (int curve = curveType; curve <= ANSI_Time_Curves.CURVE_TYPE_SHORT_TIME_INVERSE; curve++) {
	//		for (Icurrent = Ipickup + 0.1	; Icurrent < 21; Icurrent = Icurrent + 0.1 ) {
	//			tripTime = calcInverseOvercurrent(curve, Ipickup, Icurrent, timeDialSetting);
	//			//say(curve + " " + Ipickup + " A " + Icurrent + " A " + tripTime + " ms");
	//			say(" " + tripTime);
	//		}
	//		say("Completed curve " + curve + "\n\n");
	//	}
	//	
	//	say("Completed");
	//	
	// ================================================================================================================
	//
	// HVACrewiring()
	// ==============
	// Scripts to refine and present the rewiring for the thesis chapters
	
	public Belief Monitor2 (DiagnosticPoints dps, NIOserver server) {
		// =======================================================================
		// Compiler generated code.
		Belief belief = new Belief();
		belief.BeliefType(BeliefTypes.DYNAMIC);
		belief.Veracity(VeracityTypes.UNDETERMINED);
		
		// Map diagnostic point instances.
		DiagnosticPoint TEMPERATURE_READ = dps.map("TEMPERATURE", "READ");
		DiagnosticPoint TEMPERATURE_CNF_TEMP = dps.map("TEMPERATURE", "CNF_TEMP");
		DiagnosticPoint TEMPERATURE_ERROR = dps.map("TEMPERATURE", "ERROR");
		// =======================================================================
			
		boolean monitoring = true;
		long timestampIn = 0;
		long timestampOut = 0;
		
		say("Monitor2");
		
		while (monitoring) {
			if (TEMPERATURE_READ.readEvent(timestampIn)) {
				say("READ fired " + TEMPERATURE_READ.timestamp() + "\n");
				timestampIn = TEMPERATURE_READ.timestamp();
			}
			
			if (TEMPERATURE_CNF_TEMP.readEvent(timestampIn)) {
				say("CNF_TEMP fired " + TEMPERATURE_CNF_TEMP.timestamp() + "\n");
				timestampOut = TEMPERATURE_CNF_TEMP.timestamp();
				say("Event time " + (timestampOut - timestampIn) + "ms");
				say("temp_F " + TEMPERATURE_CNF_TEMP.value());
				// The function block is returning temperature readings.
				belief.Veracity(VeracityTypes.TRUE);
				belief.add("Temperature = " + TEMPERATURE_CNF_TEMP.value());
			}
			
			if (TEMPERATURE_ERROR.readEvent(timestampIn)) {
				// The function block is reporting an error.
				belief.Veracity(VeracityTypes.FALSE);
				belief.add("Error code: " + TEMPERATURE_CNF_TEMP.value());
				break;
			}
			delay(1000);
		}	
		return belief;
	}
	
	//
	// HVACgimbal2()
	// =============
	// Scripts to refine and present the rewiring for the thesis chapters
	
	public Belief gimbal2 (DiagnosticPoints dps, NIOserver server) {
		// =======================================================================
		// Compiler generated code.
		Belief belief = new Belief();
		belief.BeliefType(BeliefTypes.DYNAMIC);
		belief.Veracity(VeracityTypes.UNDETERMINED);
		
		// Map diagnostic point instances.
		DiagnosticPoint F_TO_C_CONV_REQ = dps.map("F_TO_C_CONV", "REQ");
		DiagnosticPoint F_TO_C_CONV_ERROR = dps.map("F_TO_C_CONV", "ERROR");
		DiagnosticPoint F_TO_C_CONV_CNF = dps.map("F_TO_C_CONV", "CNF");
		// =======================================================================
	
		final double absZero = -459.67; // Absolute zero Fahrenheit.	
		
		say("gimbal2()");
		double temperatureF = 0;
		double expectedTemperatureC = 0;
		double temperatureC = 0;
		
		F_TO_C_CONV_REQ.gateClose();
		
		temperatureF = absZero; 
		
		for (int test = 1; test < 100; test++) {
			expectedTemperatureC = (temperatureF - (float) 32.0) * (float) 0.555;
			F_TO_C_CONV_REQ.trigger(temperatureF);
			say("Trigger timestamp " + F_TO_C_CONV_REQ.timestamp());
			
			delay(1000);
			if (F_TO_C_CONV_CNF.readEvent(F_TO_C_CONV_REQ.timestamp())) {
				say("CNF");
				temperatureC = F_TO_C_CONV_CNF.value();
				say("CNF temp_F = " + temperatureF + " temp_C + " + temperatureC + " expected " + expectedTemperatureC);
			}
		}
		
		F_TO_C_CONV_REQ.gateOpen();
		
		return belief;
	}	
	
//	testTempF = (float)-10;
//	for (int i = 1; i < 25; i++) {
//		
//		expectedTempC = (testTempF - (float) 32.0) * (float) 0.555;
//		System.out.printf("expected %.2f\u00B0C\n", expectedTempC);
//		
//		// Trigger the sensor to read the temperature from
//		// the simulator.
//		sim.send("SZ1", testTempF);
//		TEMPERATURE_READ.trigger();
//		
//		tempF = TEMPERATURE_temp_F.readFloat(TEMPERATURE_READ.timestamp());
//				
//		//	tempC = F_TO_C_CONV_temp_C.readFloat();
//				
//		//	say("TEMPERATURE tempF " + tempF);	
//			System.out.printf("TEMPERATURE temp_F = %.2f\u00B0F  ", tempF);
//			System.out.printf("F_TO_C_CONV temp_C %.2f\u00B0C  ", tempC);
//			
//		//
//			testTempF = testTempF + (float) 3.12;
//
//		delay(1000);	
//	}
//	sim.send("RESTART");	
//	belief.add("Passed");
//	belief.add("Really, really passed");
		
	//
	// HVACsim()
	// =========
	// Diagnostic script created for the thesis chapter on Model-Driven Development
	// with diagnostics.
	//
	public Belief HVACsim(DiagnosticPoints dps, NIOserver server) {
		// ===================================================================
		// Pre-compiler generated code
		Belief belief = new Belief();
		belief.BeliefType(BeliefTypes.DYNAMIC);
		belief.Veracity(VeracityTypes.UNDETERMINED);
		
		DiagnosticPoint TEMPERATURE_READ = dps.map("TEMPERATURE", "READ");
		DiagnosticPoint TEMPERATURE_CNF_TEMP = dps.map("TEMPERATURE", "CNF_TEMP");
		DiagnosticPoint TEMPERATURE_temp_F = dps.map("TEMPERATURE", "temp_F");
		DiagnosticPoint TEMPERATURE_error = dps.map("TEMPERATURE", "error");
		
		DiagnosticPoint F_TO_C_CONV_temp_C = dps.map("F_TO_C_CONV", "temp_C");
		// ===================================================================
		
		float tempF = 0;
		float tempC = 0;
		float testTempF = 0;	
		float expectedTempC = 0;
		boolean testing = true;
		
		// Create a connection to the simulator.
		simbIoTe sim = new simbIoTe();
		if (!sim.connect("192.168.1.79", 62501)) {
			belief.add("Cannot connect to the simulator. " +
		               sim.lastErrorCode);	
			return belief;
		}	
		
		// Stop the simulator generating environmental changes.
		sim.send("HALT");
		TEMPERATURE_READ.gateClose();

		// Gimbal the temperature sub-system.
		testTempF = (float)-10;
		for (int i = 1; i < 25; i++) {
			
			expectedTempC = (testTempF - (float) 32.0) * (float) 0.555;
			System.out.printf("expected %.2f\u00B0C\n", expectedTempC);
			
			// Trigger the sensor to read the temperature from
			// the simulator.
			sim.send("SZ1", testTempF);
			TEMPERATURE_READ.trigger();
			
			tempF = TEMPERATURE_temp_F.readFloat(TEMPERATURE_READ.timestamp());
					
			//	tempC = F_TO_C_CONV_temp_C.readFloat();
					
			//	say("TEMPERATURE tempF " + tempF);	
				System.out.printf("TEMPERATURE temp_F = %.2f\u00B0F  ", tempF);
				System.out.printf("F_TO_C_CONV temp_C %.2f\u00B0C  ", tempC);
				
			//
				testTempF = testTempF + (float) 3.12;

			delay(1000);	
		}
		sim.send("RESTART");	
		belief.add("Passed");
		belief.add("Really, really passed");
		return belief;
	}
	
	//
	// monitor()
	// =========
	public boolean monitor(DiagnosticPoints dps, NIOserver server) {
		// ===========================================================
		// Automatic provisioning code.
		DiagnosticPoint F_TO_C_CONV_TEMP_F = dps.map("F_TO_C_CONV", "TEMP_F");
		// ==========================================================
		
		double lastTemperatureF = Double.NaN;
		double testTemperatureF = 0;
		double temperatureC = 0;
		int cntTransients = 0;
		double diff = 0;
		boolean status = true;
		
		while(status) {
			if (F_TO_C_CONV_TEMP_F.hasData()) {
				testTemperatureF = F_TO_C_CONV_TEMP_F.readDouble();
				temperatureC = (testTemperatureF - 32) / 1.8;
				System.out.printf("F_TO_C_CONV TEMP_F = %.2f\u00B0F [%.2f\u00B0C]\n", testTemperatureF, temperatureC);

				if (lastTemperatureF != Double.NaN) {
					// Is the reading within an acceptable range?
					diff = Math.abs(lastTemperatureF - testTemperatureF);
					if (diff > 2) {
						cntTransients++;						
						System.out.printf("\nTransient detected: delta = %.2f\n", diff);
						
					//	System.out.printf(", args)+ diff + " cntTransients = " + cntTransients);"
								
					}
				}
				lastTemperatureF = testTemperatureF;
				
				if (cntTransients > 5) {
					status = false;					
				}
			} else {
				delay(500);
			}
		}
		return status;
	}

	//
	// F_TO_C_CONV
	// ===========
	public boolean F_TO_C_CONV(DiagnosticPoints dps, NIOserver server) {
//		// ======================================================================
//		// Automatic provisioning code.
//		DiagnosticPoint F_TO_C_CONV_TEMP_F = dps.map("F_TO_C_CONV", "TEMP_F");
//		DiagnosticPoint F_TO_C_CONV_TEMP_C = dps.map("F_TO_C_CONV", "TEMP_C");
//		DiagnosticPoint F_TO_C_CONV_ERROR = dps.map("F_TO_C_CONV", "ERROR");
//		// ======================================================================		
		boolean status = true;
//		
//		double testTemperatureF = 0;
//		double expectedTemperatureC = 0;
//		int cntErrors = 0;
//		
//		F_TO_C_CONV_TEMP_F.gateClose();
////		F_TO_C_CONV_TEMP_C.gateClose();
//		F_TO_C_CONV_TEMP_C.flush(1000);	
//		
//		final int MAX_RETRYS = 5;
//		final int DELAY_TIME = 100; // milliseconds.
//		
//				 
//		// Absolute zero -273.15 C is -459.67 F
//		testTemperatureF = -10; 
//		for (int test = 0; test < 15; test++) {
//			expectedTemperatureC = (testTemperatureF - 32) * (5/9); /// 1.8;
//			F_TO_C_CONV_TEMP_F.trigger(testTemperatureF);
//			
//			
//			
//			if (F_TO_C_CONV_TEMP_C.readWait(testTemperatureF, expectedTemperatureC, .0001, MAX_RETRYS, DELAY_TIME)) {
//				System.out.printf("F_TO_C_CONV_1 TEMP_F = %.2f\u00B0F returned as TEMP_C = %.2f\u00B0C\n", testTemperatureF, expectedTemperatureC);
//			} else {
//				if (F_TO_C_CONV_ERROR.readEvent()) {
//					say("Error event fired correctly.");
//				} else {
//					cntErrors++;
//					say("Errors " + cntErrors);
//				}	
//			}
//			testTemperatureF = testTemperatureF + 15.0;
//		}
//		
//		F_TO_C_CONV_TEMP_F.gateOpen();
//		F_TO_C_CONV_TEMP_C.gateOpen();
//		say("\nErrors = " + cntErrors);
//		
		return status;
	}
	
	//
	// monitorAll()
	// ============
	// Monitors all diagnostic points
	//
	public boolean monitorAll(DiagnosticPoints dps, NIOserver server) {
//		// ============================================================================
//		// Automatic provisioning code.
//		DiagnosticPoint F_TO_C_CONV_TEMP_F = dps.map("F_TO_C_CONV", "TEMP_F");
//		DiagnosticPoint F_TO_C_CONV_TEMP_C = dps.map("F_TO_C_CONV", "TEMP_C");
//		DiagnosticPoint F_TO_C_CONV_ERROR = dps.map("F_TO_C_CONV", "ERROR");
//		DiagnosticPoint Z_SWITCHES_CNF_CMD_UP = dps.map("Z_SWITCHES", "CNF_CMD_UP");
//		DiagnosticPoint Z_SWITCHES_CNF_CMD_DOWN = dps.map("Z_SWITCHES", "CNF_CMD_DOWN");
//		// =============================================================================	
//		
		boolean status = true;
//		double lastTemperatureF = Double.NaN;
//		double temperatureF = 0;
//		double temperatureC = 0;
//		int cntTransients = 0;
//		double diff = 0;
//		boolean reported = false;
//		
//		say("monitorAll()\n");
//		while(status) {
//			reported = false;
//			if (F_TO_C_CONV_TEMP_F.hasData()) {
//				temperatureF = F_TO_C_CONV_TEMP_F.readDouble();
//				System.out.printf("F_TO_C_CONV TEMP_F = %.2f\u00B0C \n", temperatureF);
//				reported = true;
//			}
//			
//			if (F_TO_C_CONV_TEMP_C.hasData()) {
//				temperatureC = F_TO_C_CONV_TEMP_C.readDouble();
//				System.out.printf("F_TO_C_CONV TEMP_C = %.2f\u00B0F \n", temperatureC);
//				reported = true;
//			}
//	
//			if (F_TO_C_CONV_ERROR.readEvent()) {
//				say("F_TO_C_CONV ERROR event fired.");
//				reported = true;
//			}
//			
//			if (Z_SWITCHES_CNF_CMD_UP.readEvent()) {
//				say("Z_SWITCHES CNF_CMD_UP event fired.");
//				reported = true;
//			}
//			
//			if (Z_SWITCHES_CNF_CMD_DOWN.readEvent()) {
//				say("Z_SWITCHES CNF_CMD_DOWN event fired.");
//			}
//			
//			if (!reported) {
//				delay(100);
//			}
//		}
		return status;
	}
	
	

		
	//
	// gimbal()
	// ========
	// Gimbal test of the F_TO_C_CONV function block.
	//
	public boolean gimbal(DiagnosticPoints dps, NIOserver server) {
		// =====================================================================
		// Pre-processor generated code
		DiagnosticPoint F_TO_C_CONV_TEMP_F = dps.map("F_TO_C_CONV", "TEMP_F");
		DiagnosticPoint F_TO_C_CONV_TEMP_C = dps.map("F_TO_C_CONV", "TEMP_C");
		DiagnosticPoint F_TO_C_CONV_ERROR = dps.map("F_TO_C_CONV", "ERROR");
		// =====================================================================
		
		boolean status = false;
//		double testTemperatureF = 0;
//		double expectedTemperatureC = 0;
//		int cntErrors = 0;
//		final int MAX_RETRYS = 5;
//		final int DELAY_TIME = 100; // milliseconds.
//		
//		say("gimbal diagnostic test");
//		
//		F_TO_C_CONV_TEMP_F.gateClose();
////		F_TO_C_CONV_TEMP_C.gateClose();
//		F_TO_C_CONV_TEMP_C.flush(1000);	
//		
//		// Absolute zero -273.15 C is -459.67 F
//		testTemperatureF = -50.0;
//		for (int test = 0; test < 40; test++) {
//			expectedTemperatureC = (testTemperatureF - 32) / 1.8;
//			System.out.printf("Sent F_TO_C_CONV TEMP_F = %.2f\u00B0F. Expecting TEMP_C = %.2f\u00B0C\n", testTemperatureF, expectedTemperatureC);
//			F_TO_C_CONV_TEMP_F.trigger(testTemperatureF);
//			
//			if (F_TO_C_CONV_TEMP_C.readWait(testTemperatureF, expectedTemperatureC, .0001, MAX_RETRYS, DELAY_TIME)) {
//				System.out.printf("F_TO_C_CONV_1 TEMP_F = %.2f\u00B0F returned as TEMP_C = %.2f\u00B0C\n", testTemperatureF, expectedTemperatureC);
//			} else {
//			//	F_TO_C_CONV_TEMP_C.
//			}
//			
//			if (F_TO_C_CONV_ERROR.readEvent()) {
//				cntErrors++;
//				say("Error event fired [" + cntErrors + "]");
//			}
//			testTemperatureF = testTemperatureF + 10.0;
//		}
//		
//		F_TO_C_CONV_TEMP_F.gateOpen();
//		F_TO_C_CONV_TEMP_C.gateOpen();
//		say("\nErrors = " + cntErrors);
//		
		return status;
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
