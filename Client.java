//
// NETWORK CLIENT
// ==============
// Implements a non-blocking TCP client that can establish a connection
// to an SIFB listener that is operating within the function block
// application.
//
// (c) AUT University - 2019-2020
//
// Documentation
// =============
// Full documentation for this class is contained in the Fault Diagnosis System
// Software Architecture document.
//
// Revision History
// ================
// 10.06.2019 BRD Original version.
// 12.08.2019 BRD Modified to make it work within GORITE.
// 21.08.2019 BRD Implemented a better approach to managing non-blocking sockets.
//				  This made it easier to separate the creation of the client
//                connection from the function to send a data packet.
//
package fde;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import fde.ExitCodes;

public class Client {
	String hostName = "";
	int listenerPortNumber = 0;
	int clientStatus = ExitCodes.UNDEFINED;
	private SocketChannel socket;

	//
	// connectToServer()
	// =================
	// hostName           URL or TCP address of the remote server that is
	//                    operating within the function block application.
	//
	// listenerPortNumber The TCP port that the remote server is listening on.
	//
	// returns            One of the ExitCodes status codes to report if the
	//                    connection was established correctly.
	//

	//@SuppressWarnings("static-access")
	public int connectToServer(String hostName, int listenerPortNumber) {
		this.hostName = hostName;
		this.listenerPortNumber = listenerPortNumber;

		if (hostName.length() == 0) {
			clientStatus = ExitCodes.INVALID_HOST_NAME;
		} else if (listenerPortNumber <= 0) {
			clientStatus = ExitCodes.INVALID_LISTENER_PORT;
		} else {
			try {
				// Resolve the address of the host.
				InetSocketAddress address = new InetSocketAddress(
					InetAddress.getByName(hostName), listenerPortNumber);				
				//SocketChannel socket = SocketChannel.open();
				socket = SocketChannel.open();
				socket.configureBlocking(false);
				socket.connect(address);
				if (socket.isConnectionPending()) {
					socket.finishConnect();
				}
				clientStatus = ExitCodes.EXIT_SUCCESS;

			} catch	(UnknownHostException e) {
				System.out.println("Unknown host " + hostName + "\n");
				System.exit(ExitCodes.EXIT_FAILURE);

			} catch (IOException e) {
				System.err.println("Could not stream I/O for the connection " + hostName + " " + listenerPortNumber + ":" );
				System.err.println(e.getMessage());
				System.exit(ExitCodes.EXIT_FAILURE);
			}
		}
		return clientStatus;
	}

	//
	// sendPacket()
	// ============
	public void sendPacket(String packet) { 

		packet = packet + "  ";
		try {
			if (socket.isConnected()) {
				ByteBuffer bb = ByteBuffer.wrap(packet.getBytes());
				socket.write(bb);
				bb.clear();
			} else {
				System.err.println("Cannot send data in Client sendPacket() - no connection was established");
			}	
			
		} catch	(UnknownHostException e) {
			System.out.println("UnknownHostException caught in Client sendPacket()\n");
			System.err.println(e.getMessage());

		} catch (IOException e) {
			System.err.println("IOException caught in Client sendPacket()\n");
			System.err.println(e.getMessage());
		}
	}
}