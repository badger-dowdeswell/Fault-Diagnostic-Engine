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
// 05.03.2020 BRD Added new extended packet fields for use by the agents.
// 11.03.2020 BRD Changed the SIFB instance ID to be an integer. It is 
//                now being used to identify queues in the NIOserver.
//
package fde;

public class NIOserverPacket {
	private String command = "";
	private int SIFBinstanceID = 0;
	String dataValue = "";

	//
	// get command()
	// =============
	public String command() {
		return this.command;
	}
	
	//
	// set command()
	// =============
	public void command(String command) {
		this.command = command;
	}
	
	//
	// get SIFBinstanceID()
	// ====================
	public int SIFBinstanceID() {
		return this.SIFBinstanceID;
	}
	
	//
	// set SIFBinstanceID()
	// ====================
	public void SIFBinstanceID(int SIFBinstanceID) {
		this.SIFBinstanceID = SIFBinstanceID;
	}

	//
	// get dataValue()
	// ===============
	public String dataValue() {
		return this.dataValue;
	}
	
	//
	// set dataValue()
	// ===============
	public void dataValue(String dataValue) {
		this.dataValue = dataValue;
	}
}