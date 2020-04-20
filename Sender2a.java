/* 
* Bratin Ghosh 
* s2032224
*/
import java.io.*;
import java.net.DatagramPacket;
import java.net.*;
import java.util.*;

class Sender2a {
    public static DatagramSocket senderSocket;
    public static Timer timer = new Timer();;
    public static Object lock = new Object();

    public static InetAddress IPAddress;
    public static int PortNumber;
    public static int Timeout;
    public static int WindowSize;
    public static ArrayList<byte[]> packets= new ArrayList<byte[]>();
    public static int base = 0;

    public static double startTime;
    public static double endTime;
    public static int totalFileLength;

    public static void main(String args[]) throws Exception {
        senderSocket = new DatagramSocket();
        IPAddress = InetAddress.getByName(args[0]);
        PortNumber = Integer.parseInt(args[1]);
        Timeout = Integer.parseInt(args[3]);
        WindowSize = Integer.parseInt(args[4]);


        byte[] sendData = fileToBytes(args[2]);
        int sequenceNum = 0;
        int sendDatalength = 0;
        while (sendDatalength < sendData.length) {
            byte[] packet = new byte[Math.min(1027,(sendData.length-(1024*sequenceNum)+3))];
            packet[0] = (byte) (sequenceNum & 0xFF);
            packet[1] = (byte) ((sequenceNum >> 8) & 0xFF);
            packet[2] = (byte) ((sendDatalength+1024 >= sendData.length) ? 0x01:0x00);
            for (int i=3; i<Math.min(1027,(sendData.length-(1024*sequenceNum)+3)); i++, sendDatalength++) {
                packet[i] = sendData[(1024*sequenceNum)+i-3];
                totalFileLength++;
            }
            packets.add(packet);
            sequenceNum++;
        }
        Thread st = new Thread(new SendPacketThread());
        Thread rt = new Thread(new ReceiveAckThread());
        startTime = System.nanoTime();
        st.start();
        rt.start();
    }

    // This thread class is responsible for sending the packets present in the window
    public static class SendPacketThread implements Runnable {
        public void run() {
            synchronized(lock) {
                for (int i = 0; i < WindowSize; i++) {
                    DatagramPacket sendPacket = new DatagramPacket(packets.get(i), packets.get(i).length, IPAddress, PortNumber);
                    try {
                        senderSocket.send(sendPacket);
                    } catch (IOException e) {}
                    if (i == base) {
                        timer.schedule(new Timeout(), 0, Timeout);
                    }
                }
            }
        }
    }

    // This thread class is resposible for receiving the acknoledgement and send the next packet in the updated window
    public static class ReceiveAckThread implements Runnable {
        public void run() {
            while (true) {
                byte[] ack = new byte[2];
                DatagramPacket receivePacket = new DatagramPacket(ack, ack.length);
                try {
                    senderSocket.receive(receivePacket);
                } catch (IOException e) {}
                int ackNum =  getSequenceNumber(ack);
                synchronized(lock) {
                    if (ackNum == packets.size() - 1) {
                        endTime = System.nanoTime();
                        System.out.println(getThroughput(totalFileLength, endTime - startTime));
                        System.exit(0);
                    }
                    if (ackNum >= base) {
                        if (base + WindowSize < packets.size()) {
                            for (int i = base + WindowSize; i <= Math.min(packets.size() - 1, ackNum + WindowSize); i++) {
                                DatagramPacket sendPacket = new DatagramPacket(packets.get(i), packets.get(i).length, IPAddress, PortNumber);
                                try {
                                    senderSocket.send(sendPacket);
                                } catch (IOException e) {}
                            }
                        }
                        base = ackNum + 1;
                        timer.cancel();
                        timer.purge();
                        timer = new Timer();
                        timer.schedule(new Timeout(), 0, Timeout);
                    }
                }
            }
        }
    }

    // This thread class is responsible for re-sending all the packets in the window in case of a timeout
    public static class Timeout extends TimerTask {
        public void run() {
            synchronized (lock) {
                for (int i = base; i < Math.min((base + WindowSize), packets.size()); i++) {
                    DatagramPacket sendPacket = new DatagramPacket(packets.get(i), packets.get(i).length, IPAddress, PortNumber);
                    try {
                        senderSocket.send(sendPacket);
                    } catch (IOException e) {}
                }
                lock.notifyAll();
            }
        }
    } 

    // helper function to convert the provided file into an array of bytes
    public static byte[] fileToBytes(String Filename) throws Exception {
        File file = new File(Filename);
        byte[] sendData = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        fileInputStream.read(sendData);
        fileInputStream.close();
        return sendData;
    }

    // helper function to calculate the sequence number of a packet
    public static int getSequenceNumber(byte[] packet) {
        return ((packet[0] & 0xff) | ((packet[1] & 0xff) << 8));
    }

    // helper function to calculate the throughput
    public static double getThroughput(int totalFileLength, double totalTime) {
        return ((totalFileLength/1024) / (totalTime/1000000000));
    }
}
