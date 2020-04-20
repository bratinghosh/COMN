/* 
* Bratin Ghosh 
* s2032224
*/
import java.util.*;
import java.io.*;
import java.net.*;
class Receiver2b {
    public static DatagramSocket receiverSocket;

    public static void main(String args[]) throws Exception {
        receiverSocket = new DatagramSocket(Integer.parseInt(args[0]));
        bytesToFile(getData(Integer.parseInt(args[2])), args[1]);
    }

    public static byte[] getData(int WindowSize) throws Exception {
        // hashmap is used to store the unordered incoming packets in order
        HashMap<Integer, byte[]> receiveData = new HashMap<Integer, byte[]>();
        int base = 0;

        while (true) {
            byte[] packet = new byte[1027];
            DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
            receiverSocket.receive(receivePacket);

            byte[] ack = new byte[2];
            // check if the packet is present in the window
            if (getSequenceNumber(packet) >= base && getSequenceNumber(packet) < base + WindowSize) {
                ack[0] = packet[0];
                ack[1] = packet[1];
                byte[] bufferpacket = new byte[receivePacket.getLength() - 3];
                for (int i = 3; i < receivePacket.getLength(); i++) {
                    bufferpacket[i-3] = packet[i];
                }
                receiveData.put(getSequenceNumber(packet), bufferpacket);
                // check if higher sequence number have been already received
                while (receiveData.containsKey(base)) {
                    base++;
                }
            }

            // resend the ack according to the algorithm
            if (getSequenceNumber(packet) < base && getSequenceNumber(packet) >= base - WindowSize) {
                ack[0] = packet[0];
                ack[1] = packet[1];
            }
            
            DatagramPacket acknowledgement = new DatagramPacket(ack, ack.length, receivePacket.getAddress(), receivePacket.getPort());
            receiverSocket.send(acknowledgement);
            if (packet[2] == 0x01 && getSequenceNumber(packet) == receiveData.size() - 1) {
                // we send the last ack 10 times with delay to increase probabilty of the sender receiving it
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(10);
                    receiverSocket.send(acknowledgement);
                }
                break;
            }
        }


        ArrayList<Byte> data = new ArrayList<Byte>();

        int i = 0;
        while(receiveData.get(i) != null) {
            for (int j = 0; j < receiveData.get(i).length; j++) {
                data.add(receiveData.get(i)[j]);
            }
            receiveData.remove(i);
            i++;
        }

        byte[] byteReceiveData = new byte[data.size()];
        for (i=0; i<byteReceiveData.length; i++ ) {
            byteReceiveData[i] = data.get(i);
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
