/* 
* Bratin Ghosh 
* s2032224
*/
import java.util.*;
import java.io.*;
import java.net.*;
class Receiver1a {
    public static void main(String args[]) throws Exception {
        bytesToFile(receiveSmallPackets(Integer.parseInt(args[0])), args[1]);
    }

    // turns the bytes array into the file
    public static void bytesToFile(byte[] receiveData, String destination) throws Exception {
        FileOutputStream fileOuputStream = new FileOutputStream(destination);
        fileOuputStream.write(receiveData);
        fileOuputStream.close();
    }

    // receives small packets from the sender and makes it into a single bytes array
    public static byte[] receiveSmallPackets(int PortNumber) throws Exception {
        DatagramSocket receiverSocket = new DatagramSocket(PortNumber);
        ArrayList<Byte> receiveData = new ArrayList<Byte>();
        byte[] smallReceiveData = new byte[1027];
        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(smallReceiveData, smallReceiveData.length);
            receiverSocket.receive(receivePacket);
            for (int i=3; i<receivePacket.getLength(); i++) {
                receiveData.add(smallReceiveData[i]);
            }
            if (smallReceiveData[2] == 0x01) {
                break;
            }
        }

        byte[] byteReceiveData = new byte[receiveData.size()];
        for (int i=0; i<byteReceiveData.length; i++ ) {
            byteReceiveData[i] = receiveData.get(i);
        }
        receiverSocket.close();
        return byteReceiveData;
    }
}