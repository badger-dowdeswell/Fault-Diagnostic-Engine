//
// DIAGNOSTIC POINTS
// =================
// Manages the list of diagnostic points created during the rewiring
// of the Function Block Application. This class provides methods
// that make it easier for the test routines in the Diagnostic Packages
// to the create friendly aliases to the diagnostic point instances.
//
// AUT University - 2020-2021
//
// Revision History
// ================
// 14.08.2020 BRD Original version. 
// 26.05.2021 BRD The map() method now matches the diagnostic point up 
//                by name and event, not the port.
//
package fde;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticPoints {
	private List<DiagnosticPoint> dps = new ArrayList<DiagnosticPoint>(); 

	//
	// add()
	// =====
	public void add(DiagnosticPoint dp) {
		dps.add(dp);
	}
	
	//
	// get()
	// =====
	public DiagnosticPoint get(int ptr) {
		DiagnosticPoint dp = new DiagnosticPoint();
		if (ptr < dps.size()) {
			dp = dps.get(ptr);
		}
		return dp;
	}
	
	//
	// count()
	// =======
	//
	public int count() {
		return dps.size();
	}
	
	//
	// map()
	// =====
	// Used in the diagnostic scripts to map an existing diagnostic point
	// to a friendly name:
	//
	//
	//
	public DiagnosticPoint map(String fbName, String fbEventName) {
		DiagnosticPoint dp = new DiagnosticPoint();
		boolean found = false;
	
		for (int dpptr = 0; dpptr < dps.size(); dpptr++) {
			dp = dps.get(dpptr);
			if (dp.fbName.equals(fbName) && dp.fbEventName.equals(fbEventName)) {
				found = true;
				break;
			}
		}
		if (!found) {
			// RA_BRD This is just a safety net for the moment. It can be removed when the pre-compiler
			//        picks up errors like this earlier in the process.
			System.err.println("Diagnostic Point " + fbName + "_" + fbEventName + " is not defined.");
			System.exit(0); 
		}
		return dp;
	}
}
