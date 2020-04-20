/* 
* Bratin Ghosh 
* s2032224
*/
import java.io.*;
import java.net.*;
class Sender1b {
    public static void main(String args[]) throws Exception {
        InetAddress IPAddress = InetAddress.getByName(args[0]);
        byte[] sendData = fileToBytes(args[2]);
        sendSmallPackets(sendData, IPAddress, Integer.parseInt(args[1]), Integer.parseInt(args[3]));
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
    public static void sendSmallPackets(byte[] sendData, InetAddress IPAddress, int PortNumber, int Timeout) throws Exception {
        DatagramSocket senderSocket = new DatagramSocket();
        int sequenceNum = 0;
        int sendDatalength = 0;
        int numOfRetransmissions = 0;
        double startTime = System.nanoTime();
        while(sendDatalength < sendData.length) {
            byte[] splitSendData = new byte[Math.min(1027,(sendData.length-(1024*sequenceNum)+3))];
            splitSendData[0] = (byte) (sequenceNum & 0xFF);
            splitSendData[1] = (byte) ((sequenceNum >> 8) & 0xFF);
            splitSendData[2] = (byte) ((sendDatalength+1024 >= sendData.length) ? 0x01:0x00);
            for (int i=3; i<Math.min(1027,(sendData.length-(1024*sequenceNum)+3)); i++, sendDatalength++) {
                splitSendData[i] = sendData[(1024*sequenceNum)+i-3];
            }

            DatagramPacket sendPacket = new DatagramPacket(splitSendData, splitSendData.length, IPAddress, PortNumber);
            byte[] acknowledgement = new byte[2];
            int receiverNotRespondingCount = 0;//keep a check if the sender is resending the last packet to an offline receiver
            while(true) {
                //send the packet
                senderSocket.send(sendPacket);
                try {
                    //after 10 timeouts the sender goes offline
                    if (receiverNotRespondingCount > 10) {
                        break;
                    }

                    //reset timeout
                    senderSocket.setSoTimeout(Timeout);
                    //receive acknowledgement
                    DatagramPacket acknowledgementPacket = new DatagramPacket(acknowledgement, acknowledgement.length);
                    senderSocket.receive(acknowledgementPacket);
                    int acknowledgementSequenceNum = getSequenceNumber(acknowledgement);
                    if (confirmAcknowledgement(sequenceNum, acknowledgementSequenceNum)) {
                        break;
                    }
                    else {
                        numOfRetransmissions++;
                    }
                    receiverNotRespondingCount = 0;//reinitialize the value as the sender got some acknowledgement from the receiver and hence the receiver is still online
                }
                catch (SocketTimeoutException e) {
                    numOfRetransmissions++;
                    receiverNotRespondingCount++;
                    //resend the packet if there is a timeout
                }
            }

            sequenceNum++;
        }
        double endTime   = System.nanoTime();
        double totalTime = endTime - startTime;
        senderSocket.close();
        System.out.println(numOfRetransmissions + " " + getThroughput(sendData, totalTime));
    }

    // confirm the acknowledgement received from the receiver
    public static boolean confirmAcknowledgement(int sequenceNum, int acknowledgementSequenceNum) {
        if (sequenceNum == acknowledgementSequenceNum) {
            return true;
        }
        return false;
    }

    // extracts the sequence number from a given packet
    public static int getSequenceNumber(byte[] packet) {
        return ((packet[0] & 0xff) | ((packet[1] & 0xff) << 8));
    }

    // get the throughput
    public static double getThroughput(byte[] sendData, double totalTime) {
        return ((sendData.length/1024) / (totalTime/1000000000));
    }
}


//Send a packet
//Wait for the acknowledgement of the sent packet
//Timeout Mechanism in case the packet/acknowledgement is lost
//Move on to the next packet