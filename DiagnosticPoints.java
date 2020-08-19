//
// DIAGNOSTIC POINTS
// =================
// Manages the list of diagnostic points created during the rewiring
// of the Function Block Application. This class provides methods
// that make it easier for the test routines in the Diagnostic Packages
// to the create friendly aliases to the diagnostic point instances.
//
// AUT University - 2020
//
// Revision History
// ================
// 14.08.2020 BRD Original version. 
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
	public DiagnosticPoint map(String fbName, String fbPortName) {
		DiagnosticPoint dp = new DiagnosticPoint();
	
		for (int dpptr = 0; dpptr < dps.size(); dpptr++) {
			dp = dps.get(dpptr);
			if (dp.fbName.equals(fbName) && dp.fbPortName.equals(fbPortName)) {
				break;
			}
		}
		return dp;
	}
}
