//
// DIAGNOSTIC POINT CLASS
// =======================
// This class implements a structure to hold the properties of a single diagnostic point. Each
// diagnostic point corresponds to a single DP function block instance within the application
// that is being diagnosed. 
//
// (c) AUT University - 2020
//
// Revision History
// ================
// 09.06.2020 BRD Original version

package fde;

import java.util.concurrent.TimeUnit;

import fde.DiagnosticAgent.AgentModes;
import fde.DiagnosticAgent.PacketDelimiters;

public class DiagnosticPoint {
	FunctionBlockApp fbapp = new FunctionBlockApp();
	NIOserver server = new NIOserver("", 0);
	String fbName = "";
	String fbPortName = "";
	int SIFBinstanceID = 1; 
	
	// Last values read from the diagnostic point, one for
	// each data type. These are available externally by calling
	// the value() method.
	private double lastDoubleValue = 0;
	
	
//	public DiagnosticPoint(String fbName, String fbPortName, FunctionBlockApp fbapp, NIOserver server, int SIFBinstanceID) {
//		this.fbName = fbName;
//		this.fbPortName = fbPortName;
//		this.fbapp = fbapp;
//		this.server = server;
//		this.SIFBinstanceID = SIFBinstanceID;
//	}
	
	
	
	//
	// hasData()
	// =========
	public boolean hasData() {
		return (server.inQueueSize(SIFBinstanceID) > 0);
	}
	
	//
	// read()
	// ======
	public double read() {
		NIOserverPacket packet = new NIOserverPacket();
		double value = 0;
				
		if (server.inQueueSize(SIFBinstanceID) > 0) {
			packet = server.getPacket(SIFBinstanceID);
			
			//System.out.print("read() packet.command() = [" + packet.command() + "]");
			
			if (Integer.parseInt(packet.command()) == AgentModes.SAMPLED_DATA) { 
				// RA_BRD. Should check the length supplied too..
				//         It would also be better to make the command an int rather than a string. It
				//         is constrained to being one of the enums AgentModes. (or PacketCommands?)
				try {
					value = Double.valueOf(packet.dataValue());
				} catch (NumberFormatException nfe) {
					value = 0;
				}
			}	
		}
		return value;
	}
	
	//
	// readWait()
	// ==========
	public boolean readWait(double testValue, double expectedValue, double threshold, int maxRetrys, int delayTime) {
		boolean found = false;
		double result = 0;
		lastDoubleValue = 0;
		
		for (int retry = 0; retry < maxRetrys; retry++) {
			if (hasData()) {
				result = read();
				lastDoubleValue = result;
				if (compare(result, expectedValue, threshold)) {
					found = true;
					break;	
				}
			} 
			delay(delayTime);
		}
		return found;
	}
	
	//
	// value()
	// =======
	// Returns the last value read by the readWait() method
	//
	double value() {
		return lastDoubleValue;
	}
		
	//
	// compare()
	// =========
	public boolean compare(double value, double compareValue, double threshold) {
		return (Math.abs(value - compareValue) < threshold);
	}
	
	//
	// trigger()
	// =========
	public void trigger(int data) {
		
	}
	
	//
	// trigger()
	// =========
	public boolean trigger(double data) {
		boolean wasTriggered = false;
		String packetData = "";
		String dataValue = "";
		NIOserverPacket packet = new NIOserverPacket();
		int maxRetrys = 10; //BRD parameterise this?
		
		// wait for the diagnostic point to poll for data
		for (int retry = 0; retry < maxRetrys; retry++) {
			if (server.inQueueSize(SIFBinstanceID) > 0) {
				packet = server.getPacket(SIFBinstanceID);
				//System.out.println("Packet command = [" + packet.command() + "]");
				if (Integer.parseInt(packet.command()) ==  AgentModes.POLL_AGENT) { 
					//System.out.println("Poll received. Sending " + data);
					// now send the data...
					dataValue = Double.toString(data);
					packetData = PacketDelimiters.START_OF_PACKET + AgentModes.TRIGGER_DATA_VALUE + PacketDelimiters.FIELD_SEPARATOR
							     + Integer.toString(dataValue.length()) + PacketDelimiters.FIELD_SEPARATOR  + dataValue + 
							     PacketDelimiters.FIELD_SEPARATOR + PacketDelimiters.END_OF_PACKET;
					server.sendPacket(SIFBinstanceID, packetData);
					wasTriggered = true;
					break;
				}
			} else {
				//System.out.println("No poll received ...");
			}
			delay(100);
		}
		return wasTriggered;
	}
	
	//
	// gateClose()
	// ===========
	public void gateClose() {
		String packetData = PacketDelimiters.START_OF_PACKET + AgentModes.TRIGGER_ENABLED + PacketDelimiters.FIELD_SEPARATOR + PacketDelimiters.END_OF_PACKET;
		server.sendPacket(SIFBinstanceID, packetData);
		
		// Put this in a retry loop, waiting for an acknowledgement that the gate has closed.
		// RA_BRD
		
		// flush the queue since previous readings no longer matter
		flush(100);
	}
	
	//
	// gateOpen()
	// ==========
	public void gateOpen() {
		String packetData = PacketDelimiters.START_OF_PACKET + AgentModes.PASSTHROUGH_ENABLED + PacketDelimiters.FIELD_SEPARATOR + PacketDelimiters.END_OF_PACKET;
		server.sendPacket(SIFBinstanceID, packetData);
	}
	
	//
	// flush()
	// =======
	// Flush the incoming packet queue.
	//
	@SuppressWarnings("static-access")
	public void flush(int milliseconds) {
		long startTime = System.currentTimeMillis();
		
		while((System.currentTimeMillis() - startTime) < milliseconds) {
			server.flush(SIFBinstanceID);
			Thread.currentThread().yield();
			delay(10);
		}
		
	//	delay(milliseconds);
	//	while (server.inQueueSize(SIFBinstanceID) > 0) {
	//		server.flush(SIFBinstanceID);
	//	}
	}
	
	//
	// delay()
	// =======
	void delay(int milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep((long) 500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}
	
	//
	// get fbName()
	// ==========
	public String fbName() {
		return fbName;
	}

	//
	// set fbName()
	// ============
	public void Name(String fbName) {
		this.fbName = fbName;
	}
	
	//
	// get fbPortName()
	// ================
	public String fbPortName() {
		return fbPortName;
	}

	//
	// set fbPortName()
	// ================
	public void fbPortName(String fbPortName) {
		this.fbPortName = fbPortName;
	}
	
	//
	// set fbapp()
	// ===========
	public void fbapp(FunctionBlockApp fbapp) {
		this.fbapp = fbapp;
	}
	
	//
	// set server()
	// ===========
	public void server(NIOserver server) {
		this.server = server;
	}

	//
	// get SIFBinstanceID()
	// ================
	public int SIFBinstanceID() {
		return SIFBinstanceID;
	}

	//
	// set fbPortName()
	// ================
	public void SIFBinstanceID(int SIFBinstanceID) {
		this.SIFBinstanceID = SIFBinstanceID;
	}
}
