package com.csl.org.rfid.tagserver;

import java.io.*;
import java.lang.Thread.State;
import java.net.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 *
 * @author Carlson Lam - Convergence Systems Ltd
 *
 * CS468/CS203 Low-level API demo code in Java
 * Please make sure that you firmware version  on the reader is at least:
 * TCP firmware: 2.18.11
 * RFID firmware: 1.3.99
 *
 * Here is the flow of the program
 *
 1) Open socket connection with CS203 on port 1515 (iport), 1516 (cport) and 3041 (uport)
 2) Turn on RFID board - send to cport (80000001) and wait 2 seconds
 3) Set Low Level API Mode - send to uport (80XXXXXXXX010C) - where XXXXXXXX is the ip address of the CS203 in hex
 4) Close socket with CS203
 5) Wait for 2 seconds
 6) Open socket connection with CS203 on port 1515 (iport), 1516 (cport) and 3041 (uport)
 7) Turn on RFID board - send to cport (80000001) and wait 2 seconds

 8) Enable TCP notification - send to cport (8000011701)
 9) Send AbortCmd to iport (4003000000000000)
 10) select antenna port 1 ANT_PORT_SEL - send to iport (70010107000000)
 11) Set RF power 30dBm - send to iport (700106072C010000)
 12) Set channel to iport (optional)
 13) set link profile 2 - send to iport (7001600b02000000)
 14) Send HST_CMD to iport (700100f019000000)
 15) Send ANT_CYCLES (set continuous mode) to iport (70010007ffff0000)
 16) Send QUERY_CFG to iport (7001000900000000)
 17) Set DynamicQ algorithm (INV_SEL) - send to iport (7001020901000000)
 18) Set DynamicQ values (INV_ALG_PARM_0) - send to iport (70010309f7005003)
 19) Send INV_CFG to iport (7001010901000000)
 20) start inventory - send (HST_CMD) to iport (700100f00f000000)
 21) stop inventory - send (ABORT) to iport (4003000000000000)
 22) Go back to high-level mode (80XXXXXXXX010D)
 22) Turn off RFID board - send to cport (80000002)
 23) close socket connection
 */

public class CslRfidTagServer {

	private Socket TCPCtrlSocket, TCPDataSocket = null;
	
	private DataOutputStream TCPCtrlOut, TCPDataOut = null;
	private DataInputStream TCPCtrlIn, TCPDataIn = null;
	
	private DatagramSocket clientSocket = null;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	private Date date = null;
	private Queue<TagInfo> TagBuffer;

	private byte[] inData = new byte[4096];
	private int len = 0;
	private Boolean inventoryStarted = false;
	private Boolean tcpClientStarted = false;
	
	private Thread tcpClientThread = new Thread();
	private Thread inventoryThread = new Thread();
	
	private static final Log logger = LogFactory.getLog(CslRfidTagServer.class);

	public String IPAddress;
	public int readerPower;
	public int TagServerPort;
	public int populaton;
	public String connString;
	
	public ReaderState state = ReaderState.NOT_CONNECTED;

	public CslRfidTagServer(String IPAddress, int readerPower,
			int TagServerPort, int population, String connString) {
		this.IPAddress = IPAddress;
		this.readerPower = readerPower;
		this.TagServerPort = TagServerPort;
		this.populaton = population;
		this.connString = connString;
		this.TagBuffer = new LinkedList<TagInfo>();
	}

	public void StartInventoryAsync() {

		if ((inventoryThread.getState() != State.TERMINATED && inventoryThread
				.getState() != State.NEW)
				|| (tcpClientThread.getState() != State.TERMINATED && tcpClientThread
						.getState() != State.NEW)) {
			return;
		}

		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					StartInventory();
				} catch (Exception ex) {
					// handle error which cannot be thrown back
					System.out.println(ex.getMessage());
				}
			}
		};
		Runnable task2 = new Runnable() {

			@Override
			public void run() {
				try {
					StartTCPClient();
				} catch (Exception ex) {
					// handle error which cannot be thrown back
					System.out.println(ex.getMessage());
				}
			}
		};
		tcpClientThread = new Thread(task2, "TCPClientThread");
		tcpClientThread.setDaemon(true);
		tcpClientThread.start();

		inventoryThread = new Thread(task, "InventoryThread");
		inventoryThread.setDaemon(true);
		inventoryThread.start();

		long timer = System.currentTimeMillis();
		while (System.currentTimeMillis() - timer < 10000) {
			if (state == ReaderState.BUSY)
				break;
		}
	}

	public boolean StopInventory() {
		long timer = System.currentTimeMillis();
		inventoryStarted = false;
		tcpClientStarted = false;
		while (System.currentTimeMillis() - timer < 10000) // time out in
															// 10seconds
		{
			if (state != ReaderState.BUSY)
				return true;
		}
		return false;
	}

	public int GetBufferCondition() {
		synchronized (TagBuffer) {
			return TagBuffer.size();
		}
	}

	private void StartTCPClient() {
		synchronized (tcpClientStarted) {
			if (tcpClientStarted)
				return;
			else
				tcpClientStarted = true;
		}

		while (true) {
			try {
				String data;
				// "localhost"
				// String ip = this.IPAddress ;
				Socket tagServerSocket = new Socket("localhost",
						this.TagServerPort);
				DataOutputStream outToServer = new DataOutputStream(
						tagServerSocket.getOutputStream());

				while (true) {
					synchronized (TagBuffer) {
						if (TagBuffer.size() != 0) {
							data = "";
							TagInfo tag = TagBuffer.peek();
							data += "ID:" + tag.epc;
							data += "|Antenna:" + tag.antennaPort;
							data += "|Timestamp:" + tag.timestamp;
							data += "|PC:" + tag.pc;
							data += "|RSSI:" + tag.rssi + "\n";
							outToServer.writeBytes(data);
							TagBuffer.remove();
						}
					}
					synchronized (tcpClientStarted) {
						if (!tcpClientStarted)
							break;
					}
				}
				tagServerSocket.close();
				tagServerSocket = null;

			} catch (UnknownHostException e) {
				logger.error("Unable to connect to port " + this.TagServerPort);
			} catch (IOException e) {
				logger.error("Unable to send tag data to server");
			}
			synchronized (tcpClientStarted) {
				if (!tcpClientStarted)
					break;
			}
		}

	}

	void StartInventory() {
		while (true) {
			try {
				synchronized (inventoryStarted) {
					if (inventoryStarted)
						return;
					else
						inventoryStarted = true;
				}

				String IPAddress = this.IPAddress;

				// open cport (1516)
				TCPCtrlSocket = new Socket(IPAddress, 1516);
				TCPCtrlOut = new DataOutputStream(
						TCPCtrlSocket.getOutputStream());
				TCPCtrlIn = new DataInputStream(new BufferedInputStream(
						TCPCtrlSocket.getInputStream()));

				// open iport (1515)
				TCPDataSocket = new Socket(IPAddress, 1515);
				TCPDataOut = new DataOutputStream(
						TCPDataSocket.getOutputStream());
				TCPDataIn = new DataInputStream(new BufferedInputStream(
						TCPDataSocket.getInputStream()));

				// open uport (3041)
				clientSocket = new DatagramSocket();
				String returnString = "";

				// Power up RFID module
				logger.info("Power up RFID module with command 0x80000001");
				returnString = CslRfid_SendCtrlCMD("80000001", 5, 2000);
				logger.info(String.format("Return: %s \n", returnString));
				Thread.sleep(2000);

				ReaderMode mode = checkMode();
				if (mode != ReaderMode.lowLevel) {
					// change to low-level
					setMode(ReaderMode.lowLevel);
					// System.out.println("Could not change reader to low-level mode.  Operation abort");

					// reconnect
					TCPCtrlSocket.close();
					TCPDataSocket.close();
					clientSocket.close();
					Thread.sleep(2000);
					// open cport (1516)
					TCPCtrlSocket = new Socket(IPAddress, 1516);
					TCPCtrlOut = new DataOutputStream(
							TCPCtrlSocket.getOutputStream());
					TCPCtrlIn = new DataInputStream(new BufferedInputStream(
							TCPCtrlSocket.getInputStream()));
					// open iport (1515)
					TCPDataSocket = new Socket(IPAddress, 1515);
					TCPDataOut = new DataOutputStream(
							TCPDataSocket.getOutputStream());
					TCPDataIn = new DataInputStream(new BufferedInputStream(
							TCPDataSocket.getInputStream()));
					// open uport (3041)
					clientSocket = new DatagramSocket();

					logger.info("Power up RFID module with command 0x80000001");
					returnString = CslRfid_SendCtrlCMD("80000001", 5, 2000);
					logger.info(String.format("Return: %s \n", returnString));
					mode = checkMode();
					if (mode != ReaderMode.lowLevel) {
						TCPCtrlSocket.close();
						TCPDataSocket.close();
						clientSocket.close();
						Thread.sleep(2000);
						inventoryStarted = false;
						return;
					}
					Thread.sleep(2000);
				}

				// Enable TCP Notifications
				logger.info("Enable TCP Notifications with command 0x8000011701");
				returnString = CslRfid_SendCtrlCMD("8000011701", 5, 2000);
				logger.info(String.format("Return: %s \n", returnString));
				Thread.sleep(500);

				// Send Abort command
				clearReadBuffer(TCPDataIn);
				logger.info("Send Abort command 0x4003000000000000");
				returnString = CslRfid_SendDataCMD("4003000000000000", 8, 2000);
				logger.info(String.format("Return: %s \n", returnString));

				this.state = ReaderState.CONNECTED;

				// Select Antenna port 1 ANT_PORT_SEL
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("7001010700000000"));
				logger.info("Send ANT_PORT_SEL command 0x7001010700000000");

				Thread.sleep(5);
				// Select RF power 30dBm
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("700106072C010000"));
				logger.info("Send RF Power command 700106072C010000");

				Thread.sleep(5);
				// Set Link Profile 2
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("7001600b02000000"));
				logger.info("Set Link Profile 2 command 7001600b02000000");

				Thread.sleep(5);
				// HST Command
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("700100f019000000"));
				logger.info("HST_CMD command 700100f019000000");

				Thread.sleep(5);
				// QUERY_CFG Command for continuous inventory
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("70010007ffff0000"));
				logger.info("QUERY_CFG (continuous mode) command 70010007ffff0000");

				Thread.sleep(5);
				// Set DynamicQ algorithm (INV_SEL)
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("70010309f7005003"));
				logger.info("Set DynamicQ algorithm (INV_SEL) command 70010309f7005003");

				Thread.sleep(5);
				// Send INV_CFG
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("7001010901000000"));
				logger.info("Send INV_CFG command 7001010901000000");

				// Start inventory - send (HST_CMD)
				long timer = System.currentTimeMillis();
				clearReadBuffer(TCPDataIn);
				TCPDataOut.write(hexStringToByteArray("700100f00f000000"));
				logger.info("Start inventory - send (HST_CMD) 700100f00f000000");
				boolean sentAbortCmd = false;
				while (true) {
					if (!inventoryStarted && !sentAbortCmd) {
						// Send Abort command
						clearReadBuffer(TCPDataIn);
						TCPDataOut
								.write(hexStringToByteArray("4003000000000000"));
						logger.info("Send Abort command 0x4003000000000000");
						sentAbortCmd = true;
					}

					if (TCPDataIn.available() != 0) {
						timer = System.currentTimeMillis();
						len = TCPDataIn.read(inData, 0, 8);

						if (len < 8)
							continue;

						if (byteArrayToHexString(inData, len).startsWith(
								"9898989898989898")) {
							// clearReadBuffer(TCPDataIn);
							// date = new Date();
							// System.out.println(dateFormat.format(date) +
							// " TCP Notification Received.");
							continue;
						}

						if (byteArrayToHexString(inData, len).startsWith(
								"02000780")) {
							// clearReadBuffer(TCPDataIn);
							// /date = new Date();
							// System.out.println(dateFormat.format(date) +
							// " Antenna Cycle End Notification Received");
							continue;
						}

						if (byteArrayToHexString(inData, len).startsWith(
								"4003BFFCBFFCBFFC")) {
							TCPCtrlSocket.close();
							TCPDataSocket.close();
							clientSocket.close();
							date = new Date();
							logger.info("Abort cmd response received "
									+ byteArrayToHexString(inData, len));
							Thread.sleep(2000);
							inventoryStarted = false;
							break;
						}

						// int pkt_ver = (int) inData[0];
						// int flags = (int) inData[1];
						int pkt_type = (int) (inData[2] & 0xFF)
								+ ((int) (inData[3] & 0xFF) << 8);
						int pkt_len = (int) (inData[4] & 0xFF)
								+ ((int) (inData[5] & 0xFF) << 8);
						int datalen = pkt_len * 4;

						// wait until the full packet data has come in
						while (TCPDataIn.available() < datalen) {
						}
						// finish reading
						TCPDataIn.read(inData, 8, datalen);

						if (pkt_type == 0x8001) {
							TCPCtrlSocket.close();
							TCPDataSocket.close();
							clientSocket.close();
							this.state = ReaderState.NOT_CONNECTED;
							date = new Date();
							logger.info("Command End Packet: "
									+ byteArrayToHexString(inData, len
											+ datalen));
							Thread.sleep(2000);
							break;
						}
						if (pkt_type == 0x8000) {
							this.state = ReaderState.BUSY;
							date = new Date();
							logger.info(dateFormat.format(date)
									+ " Command Begin Packet: "
									+ byteArrayToHexString(inData, len
											+ datalen));
							continue;
						}
						if (pkt_type == 0x8005) {
							date = new Date();
							// System.out.println(dateFormat.format(date) +
							// " Inventory Packet: " +
							// byteArrayToHexString(inData,len+datalen));

							byte[] EPC = new byte[1000];
							TagInfo tag = new TagInfo();

							tag.pc = byteArrayToHexString(
									Arrays.copyOfRange(inData, 20, 22), 2);

							tag.rssi = (float) (inData[13] * 0.8);
							tag.antennaPort = inData[18];
							for (int cnt = 0; cnt < (datalen - 16); cnt++) {
								EPC[cnt] = inData[22 + cnt];
							}
							tag.addr = this.IPAddress;
							tag.epc = byteArrayToHexString(EPC, datalen - 16);
							tag.timestamp = System.currentTimeMillis();

							synchronized (TagBuffer) {
								if (TagBuffer.size() >= 10000)
									TagBuffer.remove();
								TagBuffer.add(tag);
							}
						}

					} else {
						if (System.currentTimeMillis() - timer >= 8000) {

							this.state = ReaderState.NOT_CONNECTED;
							logger.error("Connection lost.  To be reconnected");
							logger.error("Close Connections");

							TCPCtrlSocket.close();
							TCPDataSocket.close();
							clientSocket.close();
							Thread.sleep(2000);
							inventoryStarted = false;
							break;
						}
					}
				}

				if (sentAbortCmd) {
					// exit thread
					logger.info("Inventory Stopped");
					this.state = ReaderState.NOT_CONNECTED;
					inventoryStarted = false;
					break;
				}
			} catch (UnknownHostException e) {
				System.err.println(e.getMessage());
				inventoryStarted = false;
			} catch (IOException e) {
				System.err.println(e.getMessage());
				inventoryStarted = false;
			} catch (java.lang.InterruptedException e) {
				System.err.println(e.getMessage());
				inventoryStarted = false;
			} catch (java.lang.IndexOutOfBoundsException e) {
				System.err.println(e.getMessage());
				inventoryStarted = false;
			}
		}
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String charArrayToHexString(char[] a, int length) {
		String returnString = "";
		for (int j = 0; j < length; j++) {
			byte c = (byte) a[j];
			int uc = (int) (c & 0xFF);
			if (Integer.toHexString(uc).length() == 1)
				returnString += "0";
			returnString += Integer.toHexString(uc);
		}
		returnString = returnString.toUpperCase();
		return returnString;
	}

	public static String byteArrayToHexString(byte[] a, int length) {
		String returnString = "";
		for (int j = 0; j < length; j++) {
			int uc = (int) (a[j] & 0xFF);
			if (Integer.toHexString(uc).length() == 1)
				returnString += "0";
			returnString += Integer.toHexString(uc);
		}
		returnString = returnString.toUpperCase();
		return returnString;
	}

	public static void clearReadBuffer(DataInputStream data) {
		byte[] inData = new byte[1024];
		try {
			while (data.available() != 0)
				data.read(inData);
		} catch (IOException e) {
			logger.error("Could not clear buffer: " + e.getMessage());
		}
	}

	private String CslRfid_SendDataCMD(String SendBuf, int RecvSize,
			int RecvTimeOut) throws IOException {
		String RecvString = "";
		int len = 0;
		try {
			TCPDataOut.write(hexStringToByteArray(SendBuf));
			if (RecvSize == 0)
				return RecvString;
			long timer = System.currentTimeMillis();
			while (System.currentTimeMillis() - timer < RecvTimeOut) {
				if (TCPDataIn.available() != 0) {
					len += TCPDataIn.read(inData, len, RecvSize - len);
					if (len >= RecvSize)
						break;
				}
			}
			RecvString = byteArrayToHexString(inData, len);
		} catch (IOException e) {
			logger.info("Error writing/reading TCP data socket");
		}
		return RecvString;

	}

	private String CslRfid_SendCtrlCMD(String SendBuf, int RecvSize,
			int RecvTimeOut) throws IOException {
		String RecvString = "";
		int len = 0;
		try {
			TCPCtrlOut.write(hexStringToByteArray(SendBuf));
			if (RecvSize == 0)
				return RecvString;
			long timer = System.currentTimeMillis();
			while (System.currentTimeMillis() - timer < RecvTimeOut) {
				if (TCPCtrlIn.available() != 0) {
					len += TCPCtrlIn.read(inData, len, RecvSize - len);
					if (len >= RecvSize)
						break;
				}
			}
			RecvString = byteArrayToHexString(inData, len);
		} catch (IOException e) {
			logger.info("Error writing/reading TCP data socket");
		}
		return RecvString;

	}

	private boolean setMode(ReaderMode mode) {
		try {

			InetAddress IPInet = InetAddress.getByName(this.IPAddress);

			// Set low-level API mode
			String cmd = "80"
					+ byteArrayToHexString(IPInet.getAddress(),
							IPInet.getAddress().length)
					+ (mode == ReaderMode.lowLevel ? "000C" : "000D");
			logger.info("Set low-level API with command 0x" + cmd.toString());

			DatagramPacket sendPacket = new DatagramPacket(
					hexStringToByteArray(cmd),
					hexStringToByteArray(cmd).length, IPInet, 3041);
			byte[] receiveData = new byte[4];
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			clientSocket.receive(receivePacket);
			logger.info("Return: "
					+ byteArrayToHexString(receivePacket.getData(),
							receivePacket.getData().length));

			if (byteArrayToHexString(receivePacket.getData(),
					receivePacket.getData().length).startsWith("810100"))
				return true;
			else
				return false;
		} catch (UnknownHostException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	private ReaderMode checkMode() {
		try {

			InetAddress IPInet = InetAddress.getByName(this.IPAddress);

			// Set low-level API mode
			String cmd = "80"
					+ byteArrayToHexString(IPInet.getAddress(),
							IPInet.getAddress().length) + "000E";
			logger.info("Check mode with command 0x" + cmd.toString());

			DatagramPacket sendPacket = new DatagramPacket(
					hexStringToByteArray(cmd),
					hexStringToByteArray(cmd).length, IPInet, 3041);
			byte[] receiveData = new byte[5];
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			clientSocket.receive(receivePacket);
			logger.info(String.format("Return: "
					+ byteArrayToHexString(receivePacket.getData(),
							receivePacket.getData().length)));

			if (byteArrayToHexString(receivePacket.getData(),
					receivePacket.getData().length).startsWith("8101010E00"))
				return ReaderMode.highLevel;
			else if (byteArrayToHexString(receivePacket.getData(),
					receivePacket.getData().length).startsWith("8101010E01"))
				return ReaderMode.lowLevel;
			else
				return null;
		} catch (UnknownHostException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

}
