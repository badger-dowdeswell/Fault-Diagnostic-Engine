//
// NON-BLOCKING IO SERVER DATA PACKET
// ==================================
// Specifies the data structure used by server packets
// used in the FIFO queue on the Non-Blocking IO server.
//
// AUT University - 2019-2020
//
// Revision History
// ================
// 05.07.2019 BRD Original version.
// 10.02.2020 BRD Renamed the parameters of the get/set functions to
//                overload the methods in a much tidier way.
//
package fde;

public class NIOserverPacket {
	private String SIFBinstanceID = "";
	String dataPacket = "";
			
	//
	// get SIFBinstanceID()
	// ====================
	public String SIFBinstanceID() {
		return this.SIFBinstanceID;
	}
	
	//
	// set SIFBinstanceID()
	// ====================
	public void SIFBinstanceID(String newSIFBinstanceID) {
		this.SIFBinstanceID = newSIFBinstanceID;
	}

	//
	// get dataPacket()
	// ================
	public String dataPacket() {
		return this.dataPacket;
	}
	
	//
	// set dataPacket()
	// ================
	public void dataPacket(String newdataPacket) {
		this.dataPacket = newdataPacket;
	}
}