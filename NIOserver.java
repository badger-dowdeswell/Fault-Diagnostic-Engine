//
// NON-BLOCKING IO NETWORK SERVER
// ==============================
// Implements a threaded TCP/IP server that supports multiple, non-blocking
// connections. This allows the SIFB AGENT_GATE instances to connect to the 
// Fault Diagnostic Engine system as clients.
//
// (c) AUT University - 2019-2020
//
// Documentation
// =============
// Further information and examples of the packet structures and the operation
// of the server is available in the document "Non-Blocking IO server design" 
// This is located with the other Fault Diagnostic Engine Software Architecture
// documents.
//
// This version was customised for use with the Fault Diagnostic Engine. A
// similiar version is used in symbIoTe to manage interactions between 
// function block applications and the simulated Human Machine Interface
// (HMI).
//
// This server operates on its own thread since it needs to accept incoming 
// connections and buffer incoming data packets as soon as they arrive. To
// free up the GORITE agent to do other things and only request packets when
// it is ready to, the server implements a First-In,First-Out (FIFO) packet
// queue. Each entry contains the data sent by the SIFB function block instance
// as well as an ID to show which SIFB sent the packet. 
//
// Revision History
// ================
// 05.07.2019 BRD Original version.
// 02.02.2020 BRD Added the ability to post data packets to the client.
// 05.03.2020 BRD Refactored the queuePacket() method to handle additional data 
//                fields.
// 06.03.2020 BRD Created a Unit Test for the queuePacket() method. 
// 09.03.2020 BRD Handled the exception that is raised when the client 
//                disconnects while the server has replies queued to post-back.
// 11.08.2020 BRD Added cntConnections() method to return the number of connections
//                that are currently open.
//
package fde;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import fde.ExitCodes;

public class NIOserver implements Runnable {
	//
	// Define the default input buffer size to read TCP
	// data into.
	final static int BUFFER_SIZE = 1024;
	
	// Determines the maximum number of unique agent connection client 
	// instances the server will support. Each session manages its own
	// simultaneous asynchronous in and out queues FIFO queues.
	// RA_BRD - could we make this dynamic?
	final int MAX_CLIENTS = 25;
	private int cntConnections = 0;
	private boolean isSilent = false;
	private boolean unitTesting = false;

	// Data packet field separators. Please ensure
	// that any changes to these are also implemented
	// in the FORTE DP function block.
	final static String MESSAGE_START = "*";
	final static String FIELD_SEPARATOR = "|";
	final static String END_OF_PACKET = "&"; 

	private String hostName = "";
	private int listenerPortNumber = 0;
	private int serverStatus = ExitCodes.UNDEFINED;
	private String replyPacket = "";
	
	// FIFO queue for packets
	// ======================
	Queue<NIOserverPacket>[] inFIFOqueue = new LinkedList[MAX_CLIENTS];
	
	Queue<NIOserverPacket>[] outFIFOqueue = new LinkedList[MAX_CLIENTS];
	
	//
	// NIOserver()
	// ===========
	// Provides the hostName, listener port number and the maximum number
	// of clients via the class constructor. This class implements Runnable.
	//
	public NIOserver(String hostName, int listenerPortNumber) {
		this.hostName = hostName;
		this.listenerPortNumber = listenerPortNumber;
	}

	//
	// run()
	// =====
	// Starts the server on the designated thread using:
	//    new Thread(server).start();
	//
	// The status of the server can be checked after attempting
	// to start it by using:
	//
	// 	  status = server.serverStatus();
	//
	// This returns an integer status code from ExitCodes.
	//
	public void run() {
		say("Server started" + "\n"); 
		try {
			startServer(hostName, listenerPortNumber);
		} catch (Exception e) {
			say("NIOserver exception caught on host " + hostName + " while trying to listen on port " + listenerPortNumber + ":" ); //RA_BRD
			say(e.getMessage()); 
			serverStatus = ExitCodes.EXIT_FAILURE;
			e.printStackTrace();
		}
	}

	//
	// startServer()
	// =============
	// Starts the server and makes connections available at the specified
	// named host address. All connections are initially accepted on the
	// specified listener port before being handed over to be managed by
	// to a session connection.
	//
	@SuppressWarnings("static-access")
	public int startServer(String hostName, int listenerPortNumber) throws Exception {
		int serverStatus = ExitCodes.EXIT_SUCCESS;
		int packetLength = 0;
		int SIFBinstanceID = 0;
		NIOserverPacket packet = new NIOserverPacket();
		
		// Initialise the queues
		for (int ptrQueue = 0; ptrQueue < MAX_CLIENTS; ptrQueue++) {
			inFIFOqueue[ptrQueue] = new LinkedList<NIOserverPacket>();
			outFIFOqueue[ptrQueue] = new LinkedList<NIOserverPacket>();
		}
		
		if (unitTesting) {
			queueUnitTest();
			return ExitCodes.EXIT_FAILURE;
		}	
		
		if (hostName.equals("")) {
			serverStatus = ExitCodes.INVALID_HOST_NAME;
		} else if (listenerPortNumber <= 0) {
			serverStatus = ExitCodes.INVALID_LISTENER_PORT;
		} else {
			// Resolve the host address.
			InetAddress host = InetAddress.getByName(hostName);

			Selector selector = Selector.open();

			// Open a non-blocking listener socket to accept all incoming connections.
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.bind(new InetSocketAddress(host, listenerPortNumber));
			serverSocketChannel.register(selector,  SelectionKey.OP_ACCEPT);

			// Ensure that after the channel has been opened, no channel
			// is currently accepted.
			SelectionKey key = null;
			serverStatus = ExitCodes.EXIT_SUCCESS;

			// This is the section that manages all the traffic. It only
			// exits if the server fails.
			while (true) {
				if (selector.select() > 0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectedKeys.iterator();

					while(iterator.hasNext()) {
						key = (SelectionKey) iterator.next();
						iterator.remove();

						if (key.isAcceptable()) {
							// A client is trying to connect to this server.
							// Accept the incoming connection request on this
							// listening socket.
							SocketChannel sc = serverSocketChannel.accept();
							// Set this to non-blocking mode.
							sc.configureBlocking(false);
							// Set the socket to read and write mode.
							sc.register(selector,  SelectionKey.OP_READ | SelectionKey.OP_WRITE);
							say("Connection accepted on local address " + sc.getLocalAddress() + "\n");
							cntConnections++;
						}

						if (key.isReadable()) {
							// This session socket was opened as a result of a
							// previous request for a connection on the server's
							// listener socket. It is therefore able to read data
							// sent to it. Try to read the data there into a
							// buffer.
							SocketChannel sc = (SocketChannel) key.channel();
							ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
							
							try {
								sc.read(byteBuffer);
							} catch (Exception e) {
								// The socket could not be read. The client
								// has probably disconnected so close off
								// the session.
								sc.close();
							}
							
							if (sc.isConnected()) {
								String dataPacket = new String (byteBuffer.array()).trim();
								packetLength = dataPacket.length();
								if (packetLength <= 0 ) {
									// A null packet was received indicating
									// that the server wishes to close this
									// session.
									sc.close();
									say("Connection closed");
									cntConnections--;
									if (cntConnections < 0) {
										cntConnections = 0;
									}
								} else {
									SIFBinstanceID = queuePacket(dataPacket);
								}	
						
								// Is there data to send to this client?
								if (SIFBinstanceID > 0) {
									if (outQueueSize(SIFBinstanceID) > 0) {
										if (key.isWritable()) {
											packet = getQueuedPacket(SIFBinstanceID);
											ByteBuffer byteBuffer2 = ByteBuffer.wrap(packet.dataValue().getBytes());
											sc.write(byteBuffer2);
											byteBuffer2.clear();
										}
									}	
								}
							}
						}
					}	
				}
				Thread.currentThread().yield();
			}
		}
		return serverStatus;
	}

	//
	// queuePacket()
	// =============
	// Splits up the data packet received into separate messages from each
	// Agent Service Interface Function Block and queues them into the inbound
	// FIFO message buffer for that connection.
	//
	// Packet structure
	// ================
	// Example packet containing two data message packets together:
	//
	//   *2|1|9|57.002834|&*1|2|6|-34.45
	//
	// 	 Start of packet character - currently character *
	//	 Command string. Identifies the purpose of the data packet. Always one of the AgentModes.
	//   Field separator - currently "|"
	// 	 SIFB instance ID - Integer.
	// 	 Field separator 
	//   Data field length - Integer
	//   Field separator 
	//   Data field - string
	//   End of packet marker - currently "&"
	//
	// Further information and examples of the packet structures is available in the document
	// "Non-Blocking IO server design documentation" located with the other Fault Diagnostic 
	// Engine Software Architecture documents.
	// 
	private int queuePacket(String dataPacket) {
		int SIFBinstanceID = 0;
		int ptrStart = 0;
		int ptrEnd = 0;
		
		String command = "";
		int fieldLen = 0;
		String dataValue = "";
		String packet = "";
		
		while (dataPacket.length() > 0) {
			ptrStart = dataPacket.indexOf(MESSAGE_START, 0);
			if (ptrStart == -1) {
				break;
			}
			
			ptrEnd = dataPacket.indexOf(END_OF_PACKET, 0);
			if (ptrEnd == -1) {
				break;
			}
			
			// There is a potential packet in the current buffer.
			// Extract the message and clean-up the buffer.
			packet = dataPacket.substring(ptrStart, ptrEnd);
			dataPacket = dataPacket.substring(ptrEnd + 1);
			command = "";
			dataValue = "";		
		
			ptrEnd = packet.indexOf(FIELD_SEPARATOR, 1);
			if (ptrEnd > 0) {
				command = packet.substring(1, ptrEnd);
				
				ptrStart = ptrEnd + 1;
				ptrEnd = packet.indexOf(FIELD_SEPARATOR, ptrStart);
				if (ptrEnd > 0) {
					try {
						SIFBinstanceID = Integer.valueOf(packet.substring(ptrStart, ptrEnd));
					} catch (NumberFormatException nfe) {
						SIFBinstanceID = 0;
					}	
													
					ptrStart = ptrEnd + 1;
					ptrEnd = packet.indexOf(FIELD_SEPARATOR, ptrStart);
					if (ptrEnd > 0) {
						try {
							fieldLen = Integer.valueOf(packet.substring(ptrStart, ptrEnd));
							if (fieldLen > 0) {
								ptrStart = packet.indexOf(FIELD_SEPARATOR, ptrEnd) + 1;
								dataValue = packet.substring(ptrStart, ptrStart + fieldLen);
							}
						} catch (NumberFormatException nfe) {
						}
					}
				}
				
				if (command.length() > 0) {
					NIOserverPacket newPacket = new NIOserverPacket();
					newPacket.command(command);
					newPacket.SIFBinstanceID(SIFBinstanceID);
					newPacket.dataValue(dataValue);
					// Packet queue entries are indexed on the instance ID
					// of the function block agent.
					inFIFOqueue[SIFBinstanceID].add(newPacket);
				}
			}	
		}
		return SIFBinstanceID;
	}	
			
	//
	// queueUnitTest()
	// ===============
	// Unit test that exercises the queuePacket() method with representative test packets.
	// 
	// Activate this in the class definition section by setting unitTesting = true;
	// Note that the server instance terminates at the end of the unit tests.
	//
	public void queueUnitTest() {
		System.out.println("Unit Test: packet queue methods");
		String testPacket = "";
		
		testPacket = "+++*4|1|7|47.5998|&+++*4|2|15|123456789012.96|&+++*2|2|&__&";
		
		queuePacket(testPacket);
		
		NIOserverPacket rpacket = new NIOserverPacket();
		for (int ptrQueue = 1; ptrQueue < MAX_CLIENTS; ptrQueue++) {
			System.out.println("\nInitial Queue " + ptrQueue + " size " + inQueueSize(ptrQueue));
			while (inQueueSize(ptrQueue) > 0) {
				rpacket = getPacket(ptrQueue);
				System.out.println(rpacket.command() + " " + rpacket.SIFBinstanceID() + " " +  rpacket.dataValue());
			}
		}
	}

	//
	// get MaxClients
	// ==============
	
	public int maxClients() {
		return this.MAX_CLIENTS;
	}
	
	//
	// get hostName
	// ============
	public String hostName() {
		return this.hostName;
	}
	
	//
	// get listenerPortNumber
	// ======================
	public int listenerPortNumber() {
		return this.listenerPortNumber;
	}	
		
	//
	// get inQueueSize()
	// =================
	public int inQueueSize(int ptrQueue) {
		return inFIFOqueue[ptrQueue].size();
	}
	
	//
	// getPacket()
	// ===========
	// Returns the next packet received from the function block
	// application that is in the inbound FIFO queue.
	//
	public NIOserverPacket getPacket(int SIFBinstanceID) {		
		NIOserverPacket packet = new NIOserverPacket();
		if (inFIFOqueue[SIFBinstanceID].size() > 0) {
			packet = inFIFOqueue[SIFBinstanceID].poll();
		}	
		return packet;
	}
	
	// 
	// flush()
	// =======
	// Flushes a single inbound queue. This is commonly used when the 
	// buffered data is no longer needed after a change of operating
	// mode.
	//
	public void flush(int ptrQueue) {
		inFIFOqueue[ptrQueue].clear();
	}
	
	//
	// get outQueueSize()
	// ==================
	public int outQueueSize(int ptrQueue) {
		return outFIFOqueue[ptrQueue].size();
	}
	
	//
	// ConnectionCount()
	// =================
	public int ConnectionCount() {
		return cntConnections;
	}
	
	//
	// sendPacket()
	// ============
	// Used by the Fault Diagnostic Engine to add (i.e. buffer) an 
	// outgoing packet into the queue. When the SIFB agent client 
	// function block next polls the server, any queued outgoing 
	// packets will be sent out.
	
	public void sendPacket(int SIFBinstanceID, String packetData) {
		NIOserverPacket newPacket = new NIOserverPacket();
		if (SIFBinstanceID <= MAX_CLIENTS) {
			newPacket.SIFBinstanceID(SIFBinstanceID);
			newPacket.dataValue(packetData);
			outFIFOqueue[SIFBinstanceID].add(newPacket);
		}
	}
	
	//
	// getQueuedPacket()
	// =================
	private NIOserverPacket getQueuedPacket(int SIFBinstanceID) {		
		NIOserverPacket packet = new NIOserverPacket();
		if (outFIFOqueue[SIFBinstanceID].size() > 0) {
			packet = outFIFOqueue[SIFBinstanceID].poll();
		}	
		return packet;
	}
	
	//
	// say()
	// =====
	// Output a console message for use during debugging. This
	// can be turned off by setting the private variable silence
	//
	private void say(String whatToSay){
		if(!isSilent) {
			System.out.println(whatToSay);
		}
	}
}