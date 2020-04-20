/* 
* Bratin Ghosh 
* s2032224
*/
import java.util.*;
import java.io.*;
import java.net.*;
class Receiver2a {
    public static DatagramSocket receiverSocket;

    public static void main(String args[]) throws Exception {
        receiverSocket = new DatagramSocket(Integer.parseInt(args[0]));
        bytesToFile(getData(), args[1]);
    }

    public static byte[] getData() throws Exception {
        ArrayList<Byte> receiveData = new ArrayList<Byte>();
        int lastSequenceNum = -1;

        while (true) {
            byte[] packet = new byte[1027];
            DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
            receiverSocket.receive(receivePacket);

            byte[] ack = new byte[2];
            if (getSequenceNumber(packet)== (lastSequenceNum + 1)) {
                // correct packet received
                ack[0] = packet[0];
                ack[1] = packet[1];
                for (int i = 3; i < receivePacket.getLength(); i++) {
                    receiveData.add(packet[i]);
                }
                lastSequenceNum++;
            }
            else {
                // incorrect packet received, we send the sequence number of the last properly received packet
                ack[0] = (byte) (lastSequenceNum & 0xFF);
                ack[1] = (byte) ((lastSequenceNum >> 8) & 0xFF);
            }
            DatagramPacket acknowledgement = new DatagramPacket(ack, ack.length, receivePacket.getAddress(), receivePacket.getPort());
            receiverSocket.send(acknowledgement);
            if (packet[2] == 0x01 && getSequenceNumber(packet) == lastSequenceNum) {
                // we send the last ack 10 times with delay to increase probabilty of the sender receiving it
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(10);
                    receiverSocket.send(acknowledgement);
                }
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
    
    // helper function to convert the array of byte into the required file
    public static void bytesToFile(byte[] receiveData, String destination) throws Exception {
        FileOutputStream fileOuputStream = new FileOutputStream(destination);
        fileOuputStream.write(receiveData);
        fileOuputStream.close();
    }

    // helper function to calculate the sequence number of a packet
    public static int getSequenceNumber(byte[] packet) {
        return ((packet[0] & 0xff) | ((packet[1] & 0xff) << 8));
    }
}
