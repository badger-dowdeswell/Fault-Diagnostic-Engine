//
// simbIoTe INTERFACE
// ==================
// Manages a connection to the simbIoTe simulator to enable the
// fault diagnostic engine to send commands that alter the
// environment or behaviour of the sensors.
//
// (c) AUT University - 2021
//
// Documentation
// =============
// Documentation for this class can be found in Appendix B of the
// PhD thesis.
//
// Revision History
// ================
// 30.04.2021 BRD Original version.
//
package fde;

public class simbIoTe {
	Client conn = new Client();
	int lastErrorCode = ExitCodes.UNDEFINED;
	
	// Data packet field separators. Please ensure
	// that any changes to these are also implemented
	// in simbIoTe.
	final static String MESSAGE_START = "*";
	final static String FIELD_SEPARATOR = "|";
	final static String END_OF_PACKET = "&"; 
	
	//
	// connect()
	// =========
	public boolean connect(String hostName, int port) {
		lastErrorCode = conn.connectToServer(hostName, port);
		if (lastErrorCode == ExitCodes.EXIT_SUCCESS) {
			return true;
		} else {
			return false;
		}	
	}
	
	//
	// send()
	// ======
	public void send(String command) {
		conn.sendPacket(MESSAGE_START + command +
				        FIELD_SEPARATOR + FIELD_SEPARATOR +
				        END_OF_PACKET);
	}
	
	//
	// send()
	// ======
	public void send(String command, String commandData) {
		conn.sendPacket(MESSAGE_START + command +
				        FIELD_SEPARATOR + commandData +
				        FIELD_SEPARATOR + END_OF_PACKET);
	}
	
	//
	// send()
	// ======
	public void send(String command, float commandData) {
		conn.sendPacket(MESSAGE_START + command +
				        FIELD_SEPARATOR + Float.toString(commandData) +
				        FIELD_SEPARATOR + END_OF_PACKET);
	}
	
	//
	// send()
	// ======
	public void send(String command, double commandData) {
		conn.sendPacket(MESSAGE_START + command +
				        FIELD_SEPARATOR + Double.toString(commandData) +
				        FIELD_SEPARATOR + END_OF_PACKET);
	}
	
	//
	// close()
	// =======
	public void close() {
	}
	
	//
	// get lastErrorCode()
	// ===================
	public int lastErrorCode() {
		return this.lastErrorCode;
	}
}
