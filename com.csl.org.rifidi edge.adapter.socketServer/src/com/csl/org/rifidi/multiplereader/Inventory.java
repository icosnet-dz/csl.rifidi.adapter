/**
 *
 * @author Gene Yeung - Convergence Systems Ltd
 *
 * CS468/CS203 Low-level API demo code in Java
 * Here is the flow of the program
 *
1) Open socket connection with CS203 on port 1515 (iport), 1516 (cport)
2) Enable TCP notification - send to cport (8000011701)
3) Send AbortCmd to iport (4003000000000000)
4) select antenna port 1 ANT_PORT_SEL - send to iport (70010107000000)
5) Set RF power 30dBm - send to iport (700106072C010000)
6) Set channel to iport (optional)
7) set link profile 2 - send to iport (7001600b02000000)
8) Send HST_CMD to iport (700100f019000000)
9) Send ANT_CYCLES (set continuous mode) to iport (70010007ffff0000)
10) Send QUERY_CFG to iport (7001000900000000)
11) Set DynamicQ algorithm (INV_SEL) - send to iport (7001020901000000)
12) Set DynamicQ values (INV_ALG_PARM_0) - send to iport (70010309f7005003)
13) Send INV_CFG to iport (7001010901000000)
14) start inventory - send (HST_CMD) to iport (700100f00f000000)
15) stop inventory - send (ABORT) to iport (4003000000000000)
16) Turn off RFID board - send to cport (80000002)
17) close socket connection

 */

package com.csl.org.rifidi.multiplereader;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import CSLibrary.Structures.*;
import CSLibrary.Constants.*;


public class Inventory {

    private Thread reset;
    private String ipAddress = "192.168.25.203";
    private String macAddress = "";
    private String applicationSettings = ""; 
    private String deviceName = "";
    public boolean stopInventory = false;
    
    private Socket TCPCtrlSocket, TCPDataSocket = null;
    private DataOutputStream TCPCtrlOut, TCPDataOut = null;
    private DataInputStream TCPCtrlIn, TCPDataIn = null;
    
    private Vector AsyncCallbackEventSubscribers = new Vector();
    
    public void addAsyncCallbackEventListener(AsyncCallbackEventListener cls){
        AsyncCallbackEventSubscribers.add(cls);
    }
    public void removeAsyncCallbackEventListener(AsyncCallbackEventListener cls){
        AsyncCallbackEventSubscribers.remove(cls);
    }
    
    public Inventory(String deviceName)
    {
        this.deviceName = deviceName;
    }   
    
    public  boolean Connect(String ip)
    {
        ipAddress = ip;
        
        byte[] inData=new byte[1024];
        int len=0;
        
        try {
        
            //open cport (1516)
            TCPCtrlSocket = new Socket(ipAddress, 1516);
            TCPCtrlOut = new DataOutputStream(TCPCtrlSocket.getOutputStream());
            TCPCtrlIn = new DataInputStream(new BufferedInputStream(TCPCtrlSocket.getInputStream()));

            //Enable TCP Notifications
            TCPCtrlOut.write(hexStringToByteArray("8000011701"));
            System.out.println("Enable TCP Notifications with command 0x8000011701");
            while(true)
            {
                if(TCPCtrlIn.available() != 0)
                {
                    len=TCPCtrlIn.read(inData);
                    break;
                }
            }
            System.out.printf("Return: %s \n", byteArrayToHexString(inData,len));
            TCPCtrlSocket.close();
            
            Thread.sleep(10);
            
            //open iport (1515)
            TCPDataSocket = new Socket(ipAddress, 1515);
            TCPDataOut = new DataOutputStream(TCPDataSocket.getOutputStream());
            TCPDataIn = new DataInputStream(new BufferedInputStream(TCPDataSocket.getInputStream()));
            
            return true;
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
            return false;
        }
    }
    
    public void Disconnect()
    {           
        try 
        {
            TCPDataSocket.close();
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
        }
    }
    
    public void StartInventory(String ip) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = null;
        Scanner in;
        String IPAddress, cmdString;
        int power = 300; // 300=30.0dBm
        int port=0; // antenna port number
        byte[] inData=new byte[1024];
        int len=0;
        int Country = 2;
        
        while (!Connect(ip)){};

        try {
            
            while (!stopInventory)
            {
                try {
                        date = new Date();
                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ipAddress + ".txt", true)));
                        out.println(dateFormat.format(date) + " Start Reading...");
                        out.close();
                } catch (IOException e) {
                    System.out.println("Could not write to log file " + e.toString());
                }

                //Send Abort command
                TCPDataOut.write(hexStringToByteArray("4003000000000000"));
                System.out.println("Send Abort command 0x4003000000000000");
                clearReadBuffer(TCPDataIn);

                if (port != 0)
                {
                    //Select Antenna port ANT_PORT_SEL
                    TCPDataOut.write(hexStringToByteArray("7001010700000000"));
                    System.out.println("Send ANT_PORT_SEL command 0x7001010700000000");
                    Thread.sleep(1);

                    TCPDataOut.write(hexStringToByteArray("7001020700000000"));
                    System.out.println("Send ANT_PORT_CFG command 0x7001020700000000");
                    Thread.sleep(1);

                    cmdString=String.format("70010107%02X000000", port & 0xFF);
                    TCPDataOut.write(hexStringToByteArray(cmdString));
                    System.out.println("Send ANT_PORT_SEL command 0x" + cmdString);
                    Thread.sleep(1);

                    TCPDataOut.write(hexStringToByteArray("7001020701000000"));
                    System.out.println("Send ANT_PORT_CFG command 0x7001020701000000");
                    Thread.sleep(1);
                }
                else
                {
                    //Select Antenna port ANT_PORT_SEL
                    TCPDataOut.write(hexStringToByteArray("7001010700000000"));
                    System.out.println("Send ANT_PORT_SEL command 0x7001010700000000");
                    Thread.sleep(1);

                }

                //Select RF power 30dBm
                cmdString=String.format("70010607%02X%02X0000", power & 0xFF, ((power >> 8) & 0xFF));
                //TCPDataOut.write(hexStringToByteArray("700106072C010000"));
                TCPDataOut.write(hexStringToByteArray(cmdString));
                System.out.println(String.format("Set RF Power to %4.1f dBm with command %s",(float)power / 10,cmdString));

                Thread.sleep(1);
                //Set Link Profile 2
                TCPDataOut.write(hexStringToByteArray("7001600b02000000"));
                System.out.println("Set Link Profile 2 command 7001600b02000000");

                Thread.sleep(1);
                //HST Command
                TCPDataOut.write(hexStringToByteArray("700100f019000000"));
                System.out.println("HST_CMD command 700100f019000000");
                clearReadBuffer(TCPDataIn);

                Thread.sleep(1);
                //QUERY_CFG Command for continuous inventory
                TCPDataOut.write(hexStringToByteArray("70010007ffff0000"));
                System.out.println("QUERY_CFG (continuous mode) command 70010007ffff0000");

                Thread.sleep(1);
                //Read Country code
                TCPDataOut.write(hexStringToByteArray("7001000502000000"));
                Thread.sleep(1);
                TCPDataOut.write(hexStringToByteArray("700100f003000000"));
                Thread.sleep(1);
                while(true)
                {
                    if(TCPDataIn.available() != 0)
                    {
                        len=TCPDataIn.read(inData);
                        break;
                    }
                }
                Country = inData[28];
                                
                Thread.sleep(1);
                //Fixed channel code
                if (Country == 1 || Country == 3 || Country == 8)
                {
                    //disable all channels         
                    for (int i=0;i<=49;i++)
                    {
                        TCPDataOut.write(hexStringToByteArray(String.format("7001010C%02X000000", i)));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray("7001020C00000000"));
                        Thread.sleep(1);
                    }
                    //enable channel 1
                    TCPDataOut.write(hexStringToByteArray("7001010C01000000"));
                    Thread.sleep(1);
                    TCPDataOut.write(hexStringToByteArray("7001020C01000000"));
                    Thread.sleep(1);
                }
                else if (Country == 2)
                {
                    //Enable channels
                    for (int i = 0; i < FCC_CHN_CNT; i++)
                    {
                        TCPDataOut.write(hexStringToByteArray(String.format("7001010C%02X000000", i)));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray("7001020C01000000"));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray(String.format("7001030C%08X", fccFreqTable[fccFreqSortedIdx[i]])));
                        Thread.sleep(1);
                    }
                }
                else if (Country == 4)
                {
                    //Enable channels
                    for (int i = 0; i < TW_CHN_CNT; i++)
                    {
                        TCPDataOut.write(hexStringToByteArray(String.format("7001010C%02X000000", i)));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray("7001020C01000000"));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray(String.format("7001030C%08X", twFreqTable[twFreqSortedIdx[i]])));
                        Thread.sleep(1);
                    }
                    //Disable channels
                    for (int i = TW_CHN_CNT; i < 50; i++)
                    {
                        TCPDataOut.write(hexStringToByteArray(String.format("7001010C%02X000000", i)));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray("7001020C00000000"));
                        Thread.sleep(1);
                    }
                }
                else if (Country == 7)
                {
                    //Enable channels
                    for (int i = 0; i < CN_CHN_CNT; i++)
                    {
                        TCPDataOut.write(hexStringToByteArray(String.format("7001010C%02X000000", i)));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray("7001020C01000000"));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray(String.format("7001030C%08X", cnFreqTable[cnFreqSortedIdx[i]])));
                        Thread.sleep(1);
                    }
                    //Disable channels
                    for (int i = CN_CHN_CNT; i < 50; i++)
                    {
                        TCPDataOut.write(hexStringToByteArray(String.format("7001010C%02X000000", i)));
                        Thread.sleep(1);
                        TCPDataOut.write(hexStringToByteArray("7001020C00000000"));
                        Thread.sleep(1);
                    }
                }

                //Set DynamicQ algorithm
                TCPDataOut.write(hexStringToByteArray("7001020903000000"));     //algorith
                System.out.println("Set DynamicQ algorithm (INV_SEL) command 7001020903000000");
                Thread.sleep(1);

                //Send INV_CFG
                TCPDataOut.write(hexStringToByteArray("7001010903000000"));
                System.out.println("Send INV_CFG command 7001010903000000");

                Thread.sleep(1);
                //Set dwell time
                //TCPDataOut.write(hexStringToByteArray("70010507D0070000"));
                //System.out.println("Send Dwell time command 7001050700000000");

                //Start inventory - send (HST_CMD)
                long timer=System.currentTimeMillis();
                clearReadBuffer(TCPDataIn);
                TCPDataOut.write(hexStringToByteArray("700100f00f000000"));
                System.out.println("Start inventory - send (HST_CMD) 700100f00f000000");
                while(true)
                {
                    if(TCPDataIn.available() >= 8)
                    {
                        timer=System.currentTimeMillis();                        
                        //get packet header first
                        len=TCPDataIn.read(inData, 0, 8);

                        //if (len<8)
                        //    continue;

                        if (byteArrayToHexString(inData,len).startsWith("9898989898989898"))
                        {
                            date = new Date();
                            System.out.println(dateFormat.format(date) + " TCP Notification Received.");
                            continue;
                        }
                        else if (byteArrayToHexString(inData,len).startsWith("02000780")
                                    || byteArrayToHexString(inData,len).startsWith("01000780"))
                        {
                            date = new Date();
                            System.out.println(dateFormat.format(date) + " Antenna Cycle End Notification Received");
                            continue;
                        }
                        else if (byteArrayToHexString(inData,len).startsWith("4003BFFCBFFCBFFC"))
                        {
                            // Check Abort command response
                            System.out.println("All tag data has been returned");
                            break ;
                        }

                        int pkt_ver = (int)(inData[0] & 0xFF);
                        int flags = (int) (inData[1] & 0xFF);
                        int pkt_type = (int) (inData[2] & 0xFF) + ((int)(inData[3] & 0xFF) << 8);
                        int pkt_len = (int) (inData[4] & 0xFF) + ((int)(inData[5] & 0xFF) << 8);
                        int datalen = pkt_len * 4;
                        if (pkt_ver != 0x01 && pkt_ver != 0x02)
                        {
                            System.out.println("Unrecognized packet header: " + byteArrayToHexString(inData, len));
                            continue;
                        }

                        //wait until the full packet data has come in
                        boolean inCompletePacket=false;
                        while(TCPDataIn.available() < datalen)
                        {
                            if (System.currentTimeMillis() - timer >= 3000) {
                                System.out.println("Incomplete packet returned.  Packet header: " + byteArrayToHexString(inData, len));
                                len=TCPDataIn.read(inData, 0, TCPDataIn.available());
                                System.out.println("Packet body: " + byteArrayToHexString(inData, len));
                                inCompletePacket=true;
                                break;
                            }
                        }
                        if (inCompletePacket)
                        {
                            try {
                                date = new Date();
                                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ipAddress + ".txt", true)));
                                out.println(dateFormat.format(date) + " Incomplete data packet due to reader connection lost");
                                out.close();
                            } catch (IOException e) {
                                System.out.println("Could not write to log file " + e.toString());
                            }
                            break;
                        }
                        //finish reading
                        TCPDataIn.read(inData, 8, datalen);

                        if (pkt_type == 0x8001 || pkt_type == 0x0001)
                        {
                            date = new Date();
                            //System.out.println(dateFormat.format(date) + " Command End Packet: " + byteArrayToHexString(inData,len+datalen));
                            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ipAddress + ".txt", true)));
                            out.println(dateFormat.format(date) + " Command End Packet: " + byteArrayToHexString(inData,len+datalen));
                            out.close();
                            continue;
                        }
                        if (pkt_type == 0x8000 || pkt_type == 0x0000)
                        {
                            date = new Date();
                            System.out.println(dateFormat.format(date) + " Command Begin Packet: " + byteArrayToHexString(inData,len+datalen));
                            continue;
                        }
                        if (pkt_type == 0x8005 || pkt_type == 0x0005)
                        {
                            byte[] PC = new byte[2];
                            byte[] EPC = new byte[64];
                            for (int cnt = 0; cnt < 2; cnt++)
                            {
                                PC[cnt] = inData[20 + cnt];
                            }
                            for (int cnt = 0; cnt < (datalen - 16); cnt++)
                            {
                                EPC[cnt] = inData[22 + cnt];
                            }
                            float rssi = inData[13];
                            rssi *= 0.8;
                            
                            AsyncCallbackRaiseEvent(new TagCallbackInfo(rssi, new S_PC(byteArrayToHexString(PC,2)), new S_EPC(byteArrayToHexString(EPC,datalen-16)), ipAddress, deviceName));
                            continue;
                        }
                    }
                    else {
                        if (System.currentTimeMillis() - timer >= 8000) {
                            System.out.println("Connection lost.  Please reconnect");
                            System.out.println("Close Connections");

                            Disconnect();
                            try {
                                date = new Date();
                                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ipAddress + ".txt", true)));
                                out.println(dateFormat.format(date) + " Reader connection lost");
                                out.close();
                            } catch (IOException e) {
                                System.out.println("Could not write to log file " + e.toString());
                            }
                            
                            System.out.println("Reconnect");
                            clearReadBuffer(TCPDataIn);
                            while (!Connect(ip)){};

                            break;
                        }
                    }
                    Thread.sleep(1); // save CPU usage
                }
            }
            //Send Abort command
            clearReadBuffer(TCPDataIn);
            TCPDataOut.write(hexStringToByteArray("4003000000000000"));
            System.out.println("Send Abort command 0x4003000000000000");
        }
        catch (Exception ex)
        {
            System.err.println(ex.getMessage());
        }
    }
    
    private void AsyncCallbackRaiseEvent(TagCallbackInfo data)
    {
        for(int i=0, size = AsyncCallbackEventSubscribers.size(); i < size; i++)
        {
            ((AsyncCallbackEventListener)AsyncCallbackEventSubscribers.get(i)).AsyncCallbackEvent(new AsyncCallbackEventArgs(this, data));
        }
    }
    
    public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public String charArrayToHexString(char[] a,int length) {    
    String returnString="";
    for (int j = 0; j < length; j++) {
        byte c =  (byte)a[j];
        int uc=(int)(c & 0xFF);
        if (Integer.toHexString(uc).length() == 1) returnString+="0";
        returnString+=Integer.toHexString(uc);
    }
    returnString=returnString.toUpperCase();
    return returnString;
    }
    public String byteArrayToHexString(byte[] a,int length) {
    String returnString="";
    for (int j = 0; j < length; j++) {
        int uc=(int)(a[j] & 0xFF);
        if (Integer.toHexString(uc).length() == 1) returnString+="0";
        returnString+=Integer.toHexString(uc);
    }
    returnString=returnString.toUpperCase();
    return returnString;
    }

    public void clearReadBuffer(DataInputStream data) {
        byte[] inData = new byte[1024];
        int len = 0;
        try {
            Thread.sleep(10);
            if(data.available() != 0)
            {
                len=data.read(inData);
            }
            System.out.printf("Return: %s \n", byteArrayToHexString(inData,len));
        } catch (Exception e) {
            System.out.println("Could not clear buffer: " + e.getMessage());
        }
    }
    
    private int[] fccFreqTable = new int[]
    {
        0x4F0E1800, /*915.75 MHz   */
        0x4D0E1800, /*915.25 MHz   */
        0x1D0E1800, /*903.25 MHz   */
        0x7B0E1800, /*926.75 MHz   */
        0x790E1800, /*926.25 MHz   */
        0x210E1800, /*904.25 MHz   */
        0x7D0E1800, /*927.25 MHz   */
        0x610E1800, /*920.25 MHz   */
        0x5D0E1800, /*919.25 MHz   */
        0x350E1800, /*909.25 MHz   */
        0x5B0E1800, /*918.75 MHz   */
        0x570E1800, /*917.75 MHz   */
        0x250E1800, /*905.25 MHz   */
        0x230E1800, /*904.75 MHz   */
        0x750E1800, /*925.25 MHz   */
        0x670E1800, /*921.75 MHz   */
        0x4B0E1800, /*914.75 MHz   */
        0x2B0E1800, /*906.75 MHz   */
        0x470E1800, /*913.75 MHz   */
        0x690E1800, /*922.25 MHz   */
        0x3D0E1800, /*911.25 MHz   */
        0x3F0E1800, /*911.75 MHz   */
        0x1F0E1800, /*903.75 MHz   */
        0x330E1800, /*908.75 MHz   */
        0x270E1800, /*905.75 MHz   */
        0x410E1800, /*912.25 MHz   */
        0x290E1800, /*906.25 MHz   */
        0x550E1800, /*917.25 MHz   */
        0x490E1800, /*914.25 MHz   */
        0x2D0E1800, /*907.25 MHz   */
        0x590E1800, /*918.25 MHz   */
        0x510E1800, /*916.25 MHz   */
        0x390E1800, /*910.25 MHz   */
        0x3B0E1800, /*910.75 MHz   */
        0x2F0E1800, /*907.75 MHz   */
        0x730E1800, /*924.75 MHz   */
        0x370E1800, /*909.75 MHz   */
        0x5F0E1800, /*919.75 MHz   */
        0x530E1800, /*916.75 MHz   */
        0x450E1800, /*913.25 MHz   */
        0x6F0E1800, /*923.75 MHz   */
        0x310E1800, /*908.25 MHz   */
        0x770E1800, /*925.75 MHz   */
        0x430E1800, /*912.75 MHz   */
        0x710E1800, /*924.25 MHz   */
        0x650E1800, /*921.25 MHz   */
        0x630E1800, /*920.75 MHz   */
        0x6B0E1800, /*922.75 MHz   */
        0x1B0E1800, /*902.75 MHz   */
        0x6D0E1800, /*923.25 MHz   */
    };
    /// <summary>
    /// FCC Frequency Channel number
    /// </summary>
    private int FCC_CHN_CNT = 50;
    private int[] fccFreqSortedIdx = new int[]{
        26, 25, 1, 48, 47,
        3, 49, 35, 33, 13,
        32, 30, 5, 4, 45,
        38, 24, 8, 22, 39,
        17, 18, 2, 12, 6,
        19, 7, 29, 23, 9,
        31, 27, 15, 16, 10,
        44, 14, 34, 28, 21,
        42, 11, 46, 20, 43,
        37, 36, 40, 0, 41,
    };
    
    private int[] twFreqTable = new int[]
    {
        0x7D0E1800, /*927.25MHz   10*/
        0x730E1800, /*924.75MHz   5*/
        0x6B0E1800, /*922.75MHz   1*/
        0x750E1800, /*925.25MHz   6*/
        0x7F0E1800, /*927.75MHz   11*/
        0x710E1800, /*924.25MHz   4*/
        0x790E1800, /*926.25MHz   8*/
        0x6D0E1800, /*923.25MHz   2*/
        0x7B0E1800, /*926.75MHz   9*/
        0x690E1800, /*922.25MHz   0*/
        0x770E1800, /*925.75MHz   7*/
        0x6F0E1800, /*923.75MHz   3*/
    };
    /// <summary>
    /// Taiwan Frequency Channel number
    /// </summary>
    private int TW_CHN_CNT = 12;
    private int[] twFreqSortedIdx = new int[]{
        10, 5, 1, 6,
        11, 4, 8, 2,
        9, 0, 7, 3,
    };
    
    private int[] cnFreqTable = new int[]
    {
        0xD31c3000, /*922.375MHz   */
        0xD11c3000, /*922.125MHz   */
        0xCD1c3000, /*921.625MHz   */
        0xC51c3000, /*920.625MHz   */
        0xD91c3000, /*923.125MHz   */
        0xE11c3000, /*924.125MHz   */
        0xCB1c3000, /*921.375MHz   */
        0xC71c3000, /*920.875MHz   */
        0xD71c3000, /*922.875MHz   */
        0xD51c3000, /*922.625MHz   */
        0xC91c3000, /*921.125MHz   */
        0xDF1c3000, /*923.875MHz   */
        0xDD1c3000, /*923.625MHz   */
        0xDB1c3000, /*923.375MHz   */
        0xCF1c3000, /*921.875MHz   */
        0xE31c3000, /*924.375MHz   */
    };
    /// <summary>
    /// China Frequency Channel number
    /// </summary>
    private int CN_CHN_CNT = 16;
    private int[] cnFreqSortedIdx = new int[]{
	      7, 6, 4, 0,
	      10, 14, 3, 1,
	      9, 8, 2, 13,
	      12, 11, 5, 15,
	      };
}
