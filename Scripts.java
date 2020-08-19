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
	// monitor()
	// =========
	public boolean monitor(DiagnosticPoints dps, NIOserver server) {
		//===========================================================
		// Automatic provisioning code.
		DiagnosticPoint F_TO_C_CONV_1_TEMP_F = dps.map("F_TO_C_CONV_1", "TEMP_F");
		// ==========================================================
		
		double lastTemperatureF = Double.NaN;
		double testTemperatureF = 0;
		double temperatureC = 0;
		int cntTransients = 0;
		double diff = 0;
		boolean status = true;
		
		while(status) {
			if (F_TO_C_CONV_1_TEMP_F.hasData()) {
				testTemperatureF = F_TO_C_CONV_1_TEMP_F.read();
				temperatureC = (testTemperatureF - 32) / 1.8;
				System.out.printf("F_TO_C_CONV_1 TEMP_F = %.2f\u00B0F [%.2f\u00B0C]\n", testTemperatureF, temperatureC);

				if (lastTemperatureF != Double.NaN) {
					// Is the reading within an acceptable range?
					diff = Math.abs(lastTemperatureF - testTemperatureF);
					if (diff > 2) {
						cntTransients++;
						say("Diff = " + diff + " cntTransients = " + cntTransients);
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
		// ======================================================================
		// Automatic provisioning code.
		DiagnosticPoint F_TO_C_CONV_1_TEMP_F = dps.map("F_TO_C_CONV_1", "TEMP_F");
		DiagnosticPoint F_TO_C_CONV_1_TEMP_C = dps.map("F_TO_C_CONV_1", "TEMP_C");
		// ======================================================================		
		boolean status = true;
		
		double testTemperatureF = 0;
		double expectedTemperatureC = 0;
		int cntErrors = 0;
		
		F_TO_C_CONV_1_TEMP_F.gateClose();
//		F_TO_C_CONV_1_TEMP_C.gateClose();
		F_TO_C_CONV_1_TEMP_C.flush(1000);	
		
		final int MAX_RETRYS = 10;
		final int DELAY_TIME = 100; // milliseconds.
		 
		// Absolute zero -273.15 C is -459.67 F
		testTemperatureF = -100; 
		for (int test = 0; test < 20; test++) {
			expectedTemperatureC = (testTemperatureF - 32) / 1.8;
			F_TO_C_CONV_1_TEMP_F.trigger(testTemperatureF);
			
			if (F_TO_C_CONV_1_TEMP_C.readWait(testTemperatureF, expectedTemperatureC, .0001, MAX_RETRYS, DELAY_TIME)) {
				System.out.printf("F_TO_C_CONV_1 TEMP_F = %.2f\u00B0F returned as TEMP_C = %.2f\u00B0C\n", testTemperatureF, expectedTemperatureC);
			} else {
				cntErrors++;
				say("Errors " + cntErrors);
			}
			testTemperatureF = testTemperatureF + 15.0;
		}
		
		F_TO_C_CONV_1_TEMP_F.gateOpen();
		F_TO_C_CONV_1_TEMP_C.gateOpen();
		pause("\nErrors = " + cntErrors);
		
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
