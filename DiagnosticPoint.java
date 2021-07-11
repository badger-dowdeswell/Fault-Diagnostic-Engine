//
// DIAGNOSTIC POINT CLASS
// =======================
// This class implements a structure to hold the properties of a single diagnostic point. Each
// diagnostic point corresponds to a single DP function block instance within the application
// that is being diagnosed. 
//
// (c) AUT University - 2020-2021
//
// Revision History
// ================
// 09.06.2020 BRD Original version
// 18.03.2021 BRD Implemented timestamps back from the function block
//                diagnostic point instances.
// 25.05.2021 BRD The diagnostic point now provides the name of the event
//                which will trigger the input or output event for a 
//                function block. Note that an event may or may not
//                be attached to a port. Under those circumstances
//                the event itself is the data since no value is
//                implied.
// 08.07.2021 BRD Added methods to return static function block Parameter
//                values. These are useful to the agents who need to retrieve
//                settings that cannot be captured from events or other port
//                reading methods.
//
package fde;

import java.util.concurrent.TimeUnit;

import fde.DiagnosticAgent.AgentModes;
import fde.DiagnosticAgent.PacketDelimiters;

public class DiagnosticPoint {
	FunctionBlockApp fbapp = new FunctionBlockApp();
	NIOserver server = new NIOserver("", 0);
	String fbName = "";
	String fbEventName = "";
	String fbPortName = "";
	int SIFBinstanceID = 1; 
	
	// Last values read from the diagnostic point, one for
	// each data type. These are available externally by calling
	// the value() method.
	private double lastDoubleValue = 0;
	private long lastTimestamp = 0;
	
	//
	// hasData()
	// =========
	public boolean hasData() {
		return (server.inQueueSize(SIFBinstanceID) > 0);
	}
	
	//
	// paramInt()
	// ==========
	public int paramInt(String parameterName) {
		int value = 0;
		int ptrParam = 0;
		FunctionBlock fb = new FunctionBlock();
		FunctionBlockParameter fbParameter = new FunctionBlockParameter();
		
		fb = fbapp.findfb(fbName);
		if (fb.Name().equals(fbName)) {
			ptrParam = fb.findParameter(parameterName);
			if (ptrParam != -1) {
				fbParameter = fb.Parameter(ptrParam);
				value = Integer.parseInt(fbParameter.Value());
			}	
		}
		return value;
	}
	
	//
	// paramDbl()
	// ==========
	public double paramDbl(String parameterName) {
		double value = 0;
		int ptrParam = 0;
		FunctionBlock fb = new FunctionBlock();
		FunctionBlockParameter fbParameter = new FunctionBlockParameter();
		
		fb = fbapp.findfb(fbName);
		if (fb.Name().equals(fbName)) {
			ptrParam = fb.findParameter(parameterName);
			if (ptrParam != -1) {
				fbParameter = fb.Parameter(ptrParam);
				value = Double.parseDouble(fbParameter.Value());
			}	
		}
		return value;
	}
	
	//
	// readInt()
	// =========
	public int readInt() {
		NIOserverPacket packet = new NIOserverPacket();
		int value = 0;
				
		if (server.inQueueSize(SIFBinstanceID) > 0) {
			packet = server.getPacket(SIFBinstanceID);
			
			//System.out.print("read() packet.command() = [" + packet.command() + "]");
			
			if (Integer.parseInt(packet.command()) == AgentModes.SAMPLED_DATA) { 
				// RA_BRD. Should check the length supplied too..
				//         It would also be better to make the command an int rather than a string. It
				//         is constrained to being one of the enums AgentModes. (or PacketCommands?)
				try {
					value = Integer.valueOf(packet.dataValue());
				} catch (NumberFormatException nfe) {
					value = 0;
				}
				lastTimestamp = packet.timeStamp();
			}	
		}
		return value;	
	}
	
	//
	// readDouble()
	// ============
	public double readDouble() {
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
				lastTimestamp = packet.timeStamp();
			}	
		}
		return value;
	}
	
	//
	// readFloat()
	// ============
	public float readFloat(long triggerTimestamp) {
		NIOserverPacket packet = new NIOserverPacket();
		float value = 0;
		boolean found = false;
		
		if (server.inQueueSize(SIFBinstanceID) > 0) {
			while (!found) {
				packet = server.getPacket(SIFBinstanceID);
				if (Integer.parseInt(packet.command()) == AgentModes.SAMPLED_DATA) { 
					if (packet.timeStamp() >= triggerTimestamp) {
						try {
							value = Float.valueOf(packet.dataValue());
						} catch (NumberFormatException nfe) {
							value = 0;							
						}
						lastTimestamp = packet.timeStamp();
					}
				}
			}
		}	
		return value;
	}
			//System.out.print("read() packet.command() = [" + packet.command() + "]");
				// RA_BRD. Should check the length supplied too..
				//         It would also be better to make the command an int rather than a string. It
				//         is constrained to being one of the enums AgentModes. (or PacketCommands?)

	//
	// readBoolean()
	// =============
	public boolean readBoolean() {
		NIOserverPacket packet = new NIOserverPacket();
		boolean value = false;
				
		if (server.inQueueSize(SIFBinstanceID) > 0) {
			packet = server.getPacket(SIFBinstanceID);
			
			//System.out.print("read() packet.command() = [" + packet.command() + "] data = [" + packet.dataValue() + "]");
			
			if (Integer.parseInt(packet.command()) == AgentModes.SAMPLED_DATA) { 
			//	System.out.println("readBoolean() [" + packet.dataValue());
				value = (packet.dataValue.equals("T"));
			}
			lastTimestamp = packet.timeStamp();
		}
		return value;
	}
	
	//
	// readEvent()
	// ===========
	public boolean readEvent(long triggerTimestamp) {
		NIOserverPacket packet = new NIOserverPacket();
		boolean value = false;
		lastTimestamp = 0;
		
		while (server.inQueueSize(SIFBinstanceID) > 0) {
			packet = server.getPacket(SIFBinstanceID);
			//System.out.println("Packet timestamp " + packet.timeStamp() + " " + triggerTimestamp);
			if (packet.timeStamp() >= triggerTimestamp) {
				if (fbPortName != "") {
					// This diagnostic point is returning a data value rather
					// than true or false.
					
					// RA_BRD Need to switch this on the correct data type being returned.
					try {
						//System.out.println(packet.dataValue() + " " + Double.valueOf(packet.dataValue()));
						lastDoubleValue = Double.valueOf(packet.dataValue());
					} catch (NumberFormatException nfe) {
						lastDoubleValue = 0;
					}
					lastTimestamp = packet.timeStamp();
					value = true;
				} else {
					// This diagnostic point is only capturing an event.
					if (packet.dataValue().equals("T")) {
						value = true;
					}	
				}
				break;
			}
		}	
		return value;
	}
	
	//
	// readWait()
	// ==========
	// RA_BRD Need to test this to set the correct timestamp for a reading.
	//
	public boolean readWait(double testValue, double expectedValue, double threshold, int maxRetrys, int delayTime) {
		boolean found = false;
		double result = 0;
		lastDoubleValue = 0;
		
		if (compare(expectedValue, expectedValue, .0001)) {
			//System.out.println("Fine");
		}
		
		for (int retry = 0; retry < maxRetrys; retry++) {
			if (hasData()) {
				result = readDouble();
				lastDoubleValue = result;
				if (compare(result, expectedValue, threshold)) {
					found = true;
					break;	
				} else {
					System.out.println("--> readWait() no match to expectedValue [" + expectedValue + "] result [" + result + "].");
				}
			} 
			delay(delayTime);
		}
		return found;
	}
	
	//
	// readWait()
	// ==========
	// RA_BRD Need to test this to set the correct timestamp for a reading.
	//
	public boolean readWait(int testValue, int expectedValue, double threshold, int maxRetrys, int delayTime) {
		boolean found = false;
		int result = 0;
		lastDoubleValue = 0;
		
		for (int retry = 0; retry < maxRetrys; retry++) {
			if (hasData()) {
				result = readInt();
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
		//System.out.println("--> compare() " + Math.abs(value - compareValue) + " " + threshold);
		return (Math.abs(value - compareValue) < threshold);
	}
	
	//
	// trigger()
	// =========
	// RA_BRD Need to test this to set the correct timestamp for a reading.
	//
	public boolean trigger(int data) {
		boolean wasTriggered = false;
		return wasTriggered;
	}
	
	//
	// trigger()
	// =========
	public boolean trigger(double data) {
		boolean wasTriggered = false;
		String packetData = "";
		String dataValue = "";
		NIOserverPacket packet = new NIOserverPacket();
		int maxRetrys = 10; //RA_BRD parameterise this?
		
		dataValue = Double.toString(data);
		packetData = PacketDelimiters.START_OF_PACKET + AgentModes.TRIGGER_DATA_VALUE + PacketDelimiters.FIELD_SEPARATOR
				     + Integer.toString(dataValue.length()) + PacketDelimiters.FIELD_SEPARATOR  + dataValue + 
				     PacketDelimiters.FIELD_SEPARATOR + PacketDelimiters.END_OF_PACKET;
		server.sendPacket(SIFBinstanceID, packetData);
		
		// Calculate the current epoch time. Any timestamp packet that comes back must be later a than time one.
		long timestamp = System.currentTimeMillis();
		//System.out.println("trigger time " + timestamp);
					
		// Receive the timestamp back from the diagnostic point for this trigger event.
		for (int retry1 = 0; retry1 < maxRetrys; retry1++) {
			if (server.inQueueSize(SIFBinstanceID) > 0) {
				packet = server.getPacket(SIFBinstanceID);
				if (Integer.parseInt(packet.command()) == AgentModes.TIMESTAMP) {
					if (packet.timeStamp >= timestamp) {
						this.lastTimestamp = packet.timeStamp;
						//System.out.println("Timestamp back from trigger() " + lastTimestamp);
						wasTriggered = true;
						break;
					}	
				}	
			}
			delay(25); // RA_BRD parameterise this?
		}
		return wasTriggered;
	}

	//
	// trigger()
	// =========
	// Triggers an event without a corresponding data input.
	//
	public boolean trigger() {
		boolean wasTriggered = false;
		String packetData = "";
		String dataValue = "";
		NIOserverPacket packet = new NIOserverPacket();
		int maxRetrys = 10; //BRD parameterise this?	
		
		packetData = PacketDelimiters.START_OF_PACKET + AgentModes.TRIGGER_EVENT + PacketDelimiters.FIELD_SEPARATOR
			         + Integer.toString(0) + PacketDelimiters.FIELD_SEPARATOR  + "" + 
			         PacketDelimiters.FIELD_SEPARATOR + PacketDelimiters.END_OF_PACKET;
		server.sendPacket(SIFBinstanceID, packetData);
	    
		// Receive the timestamp back from the diagnostic point for this trigger event.
		for (int retry1 = 0; retry1 < maxRetrys; retry1++) {
			if (server.inQueueSize(SIFBinstanceID) > 0) {
				packet = server.getPacket(SIFBinstanceID);
				if (Integer.parseInt(packet.command()) == AgentModes.TIMESTAMP) {
					this.lastTimestamp = packet.timeStamp;
					//System.out.println("Timestamp back from event trigger() " + packet.timeStamp);
					wasTriggered = true;
					break;
				}	
			}
			delay(25); // RA_BRD parameterise this?
		}
		return wasTriggered;
	}
	
	//
	// gateClose()
	// ===========
	public void gateClose() {
		String packetData = PacketDelimiters.START_OF_PACKET + AgentModes.TRIGGER_ENABLED + PacketDelimiters.FIELD_SEPARATOR + PacketDelimiters.END_OF_PACKET;
		server.sendPacket(SIFBinstanceID, packetData);
		
		// RA_BRD Put this in a retry loop, waiting for an acknowledgement that the gate has closed?
		// RA_BRD Flush the queue since previous readings no longer matter?
		//flush(100);
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
		//	System.out.println("flush() queue size " + server.inQueueSize(SIFBinstanceID));
		}
	}
	
	//
	// delay()
	// =======
	void delay(int milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep((long) milliseconds);  
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}
	
	//
	// get fbName()
	// ============
	// This is the instance name of the diagnostic point function
	// block. Examples include DP_1, DP_5.
	//
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
	// get fbEventName()
	// =================
	public String fbEventName() {
		return fbEventName;
	}

	//
	// set fbPortName()
	// ================
	public void fbEventName(String fbEventName) {
		this.fbEventName = fbEventName;
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
	
	//
	// get timestamp()
	// ===============
	public long timestamp() {
		return lastTimestamp;
	}	
}
