/* 
* Bratin Ghosh 
* s2032224
*/
import java.util.*;
import java.io.*;
import java.net.*;
class Receiver1b {
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
        int previousSequenceNum = -1;
        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(smallReceiveData, smallReceiveData.length);
            receiverSocket.receive(receivePacket);

            int currentSequenceNum = getSequenceNumber(smallReceiveData);

            //check for duplicate packets using the sequence number
            if (!checkDuplicate(currentSequenceNum, previousSequenceNum)) {
                for (int i=3; i<receivePacket.getLength(); i++) {
                    receiveData.add(smallReceiveData[i]);
                }
                previousSequenceNum = currentSequenceNum;
            }

            //Send acknowledgement of the currentsequencenum
            byte[] acknowledgementPacket = new byte[2];
            acknowledgementPacket[0] = smallReceiveData[0];
            acknowledgementPacket[1] = smallReceiveData[1];
            DatagramPacket acknowledgement = new DatagramPacket(acknowledgementPacket, acknowledgementPacket.length, receivePacket.getAddress(), receivePacket.getPort());
            receiverSocket.send(acknowledgement);

            //check if the last packet has been received
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

    // check for duplicate packet
    public static boolean checkDuplicate(int currentSequenceNum, int previousSequenceNum) {
        if (currentSequenceNum == previousSequenceNum) {
            return true;
        }
        return false;
    }

    // extracts the sequence number from a given packet
    public static int getSequenceNumber(byte[] packet) {
        return ((packet[0] & 0xff) | ((packet[1] & 0xff) << 8));
    }
}

//Receive a packet
//Send the acknowledgement for receiving the next packet
//Check for duplicate -> send the acknowledgement again
//Receive the next packet