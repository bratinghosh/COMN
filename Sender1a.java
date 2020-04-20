/* 
* Bratin Ghosh 
* s2032224
*/
import java.io.*;
import java.net.*;
class Sender1a {
    public static void main(String args[]) throws Exception {
        InetAddress IPAddress = InetAddress.getByName(args[0]);
        byte[] sendData = fileToBytes(args[2]);
        sendSmallPackets(sendData, IPAddress, Integer.parseInt(args[1]));
    }

    // turns the file into bytes
    public static byte[] fileToBytes(String Filename) throws Exception {
        File file = new File(Filename);
        byte[] sendData = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        fileInputStream.read(sendData);
        fileInputStream.close();
        return sendData;
    }

    // breaks the whole byte array into small packets and sends it to the receiver
    public static void sendSmallPackets(byte[] sendData, InetAddress IPAddress, int PortNumber) throws Exception {
        DatagramSocket senderSocket = new DatagramSocket();
        int sequenceNum = 0;
        int sendDatalength = 0;
        while(sendDatalength < sendData.length) {
            byte[] splitSendData = new byte[Math.min(1027,(sendData.length-(1024*sequenceNum)+3))];
            splitSendData[0] = (byte) (sequenceNum & 0xFF);
            splitSendData[1] = (byte) ((sequenceNum >> 8) & 0xFF);
            splitSendData[2] = (byte) ((sendDatalength+1024 >= sendData.length) ? 0x01:0x00);
            for (int i=3; i<Math.min(1027,(sendData.length-(1024*sequenceNum)+3)); i++, sendDatalength++) {
                splitSendData[i] = sendData[(1024*sequenceNum)+i-3];
            }
            DatagramPacket sendPacket = new DatagramPacket(splitSendData, splitSendData.length, IPAddress, PortNumber);
            senderSocket.send(sendPacket);
            sequenceNum++;
            Thread.sleep(10);
        }
        senderSocket.close();
    }
}