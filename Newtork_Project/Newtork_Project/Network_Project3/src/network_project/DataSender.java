package network_project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;

public class DataSender {

    private static InetAddress ReceiverAddress;
    private static int ReceiverPort;
    private static DatagramSocket Socket;
    private static HashMap<Integer, Packet> SentPackets;
    static int NextSequenceNum = 0;
    private static int TotalPacketsSent;
    private Instant StartTime;
    private Instant EndTime;
    ////Timer
    private static Timer timer;
    private static HashMap<Integer, PacketTimer> PacketTimers;
    private static int TimeoutDuration;

    ///////\\\\\\\
    double DesiredDropRate;
    String FilePath;

    public DataSender(String receiverAddress, int receiverPort, String filePath, double desiredDropRate) {

        try {
            this.ReceiverAddress = InetAddress.getByName(receiverAddress);// Receiver IP Address
            this.ReceiverPort = receiverPort; // Receiver port number
            this.Socket = new DatagramSocket(6000);
            SentPackets = new HashMap<>();
            TotalPacketsSent = 0;
            timer = new Timer();
            PacketTimers = new HashMap<>();
            TimeoutDuration = 1000;

            //////\\\\\
            this.DesiredDropRate = desiredDropRate;
            this.FilePath = filePath;

            File fileToSend = new File(filePath);
            startHandshake(fileToSend);

        }//end try
        catch (UnknownHostException | SocketException e) {

            e.printStackTrace();
        }

    }

    void startHandshake(File file) {
        sendMetadata(file);
    }

    void sendMetadata(File file) {

        //startTime = Instant.now();
        try {

            Packet metadataPacket = Packet.CreateMetadataPacket(file.getName(), (int) file.length());
            System.out.println("Send Meta Data Pkt#: "
                    + metadataPacket.GetSequenceNumber());

            // Send metadata packet
            //sendPacket(metadataPacket,-2);
            DatagramPacket datagramPacket = new DatagramPacket(metadataPacket.GetBytes(), metadataPacket.GetBytes().length,
                    ReceiverAddress, ReceiverPort);

            //Send Meta Packet
            Socket.send(datagramPacket);
            SentPackets.put(metadataPacket.GetSequenceNumber(), metadataPacket);//Array to hold all sent packets 
            System.out.println("Packet Number " + metadataPacket.GetSequenceNumber() + " is Sent.");
            NextSequenceNum++;

//wait for Ack.. 
            byte[] receiveData = new byte[Packet.HeaderSize]; // Assuming ACK packets are small
            DatagramPacket AckPacket = new DatagramPacket(receiveData,
                    receiveData.length);
            Socket.receive(AckPacket);
            //////\\\\\
            SentPackets.remove(metadataPacket.GetSequenceNumber());

            // Handle the received ACK packet
            Packet receivedAck = Packet.SetBytes(AckPacket.getData());
            System.out.println("Received Packet#: " + receivedAck.GetSequenceNumber() + ", Type: " + receivedAck.GetPacketType());
            if (receivedAck.GetSequenceNumber() == metadataPacket.GetSequenceNumber()) {

                //AckPackets.remove(receivedAck.getSequenceNumber() ) ; 
                sendFile(file);//Send File

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void sendPacket(Packet packet, int dropseq) {

        try {

            NextSequenceNum++;

            if (packet.GetSequenceNumber() != dropseq) {

                DatagramPacket datagramPacket = new DatagramPacket(packet.GetBytes(),
                        packet.GetBytes().length,
                        ReceiverAddress, ReceiverPort);

                Instant PktstartTime = Instant.now(); //get current time (Start time of packet) 

                Socket.send(datagramPacket);
                SentPackets.put(packet.GetSequenceNumber(), packet);//Array to hold all sent packets //System.out.println("Packet# "+ packet.getSequenceNumber() + " is Sent.");

                //Calculate Packet transfer time..
                long pktdelay = Instant.now().toEpochMilli() - PktstartTime.toEpochMilli();
                System.out.println("Delay for Packet Number " + packet.GetSequenceNumber() + ": " + pktdelay + " ms");
//increase the total number of packets sent..
                TotalPacketsSent++;

                //Start a separate thread to listen for ACKs 
                AckListener ac = new AckListener(packet, Socket, SentPackets, ReceiverAddress, ReceiverPort);
                Thread ackListenerThread = new Thread(ac);
                ackListenerThread.start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }//end 
////////////////////////////////////////

    public void sendFile(File file) {
        int dropseq = -2;

        try {
            FileInputStream fileInputStream = new FileInputStream(file);

            //////////\\\\\\\\\\
            int fileSize = (int) file.length();
            int numPackets = (int) Math.ceil((double) fileSize / 500);
            int dataSize = Math.min(Packet.MaxDataSize,
                    fileInputStream.available());
            Set<Integer> packetsToDrop = calculatePacketsToDrop(numPackets,
                    DesiredDropRate);

            //File Splitting
            StartTime = Instant.now();

            while (fileInputStream.available() > 0) {

                // Create a data packet with a unique sequence number
                byte[] data = new byte[dataSize];
                fileInputStream.read(data);

                //Create Packet for file split
                Packet packet = Packet.CreateDataPacket(NextSequenceNum, data);

                // Check if this packet should be dropped
                if (packetsToDrop.contains(NextSequenceNum)) {
                    System.out.println("Dropping packet with sequence number: "
                            + NextSequenceNum);
                    packetsToDrop.remove(NextSequenceNum);

                    // **Start the timer for this packet
                    startPacketTimer(NextSequenceNum);
                    //***Start a separate thread to periodically check for timeouts
                    Thread timeoutCheckerThread = new Thread(new TimeoutChecker(packet, PacketTimers, SentPackets));
                    timeoutCheckerThread.start();
                }

                // Send the packet to the receiver
                sendPacket(packet, dropseq);

                // **Start the timer for this packet
                startPacketTimer(NextSequenceNum);

                //***Start a separate thread to periodically check for timeouts
                Thread timeoutCheckerThread = new Thread(new TimeoutChecker(packet, PacketTimers, SentPackets));
                timeoutCheckerThread.start();

            }

            fileInputStream.close();
            //dropseq =-2 ; 

            //send Finish to close the file
            sendFinPkt();

            while (true) {

                if (SentPackets.size() == 0) {
                    EndTime = Instant.now();
                    displayStatistics(file.length());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//end sendFile

    private void displayStatistics(long fileSize) {
        long totalTimeMillis = EndTime.toEpochMilli() - StartTime.toEpochMilli();
        double totalTimeSeconds = totalTimeMillis / 1000.0;
        double throughput = (fileSize / 1024.0) / totalTimeSeconds; // Throughput in KBps
        JOptionPane.showMessageDialog(null, "**File has been sent successfully**");
        System.out.println("\n-----*** Statistics ***-----");
        System.out.println("-File Size: " + fileSize + " Bytes");
        System.out.println("-Total Packets Sent: " + TotalPacketsSent);
        System.out.println("-Total Time: " + totalTimeSeconds + " seconds");
        System.out.println("-Throughput: " + String.format("%.2f", throughput) + " kbps");
    }

    void sendFinPkt() {
        String Fin_command = "Finish_File";
        Packet FinPacket = Packet.CreateHandshakePacket(Fin_command);
        //System.out.println("Send Finish Packet.. ");

        DatagramPacket datagramFinPacket = new DatagramPacket(FinPacket.GetBytes(),
                FinPacket.GetBytes().length,
                ReceiverAddress, ReceiverPort);

        try {
            Socket.send(datagramFinPacket);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    static void startPacketTimer(int sequenceNumber) {
        PacketTimer packetTimer = new PacketTimer(sequenceNumber);
        timer.schedule(packetTimer, TimeoutDuration);
        PacketTimers.put(sequenceNumber, packetTimer);

    }

    public static Set<Integer> calculatePacketsToDrop(int totalPackets, double desiredDropRate) {
        Set<Integer> droppedPackets = new HashSet<>();
        Random random = new Random();
        int packetsToDrop = (int) (totalPackets * desiredDropRate);//number of packet to be dropped 
        while (droppedPackets.size() < packetsToDrop) {
            int packetIndex = random.nextInt(totalPackets);//0-totalPackets
            while (packetIndex == 0) {
                packetIndex = random.nextInt(totalPackets);
            }

            droppedPackets.add(packetIndex);
        }

        // Print the dropped packets using a traditional for loop
        List<Integer> droppedPacketsList = new ArrayList<>(droppedPackets);
        System.err.print("Packets to be dropped: [");
        for (int i = 0; i < droppedPacketsList.size(); i++) {
            System.err.print(droppedPacketsList.get(i));
            if (i < droppedPacketsList.size() - 1) {
                System.err.print(", ");
            }
        }
        System.err.println("]");

        return droppedPackets;
    }

}

class AckListener implements Runnable {

    Packet packet;
    DatagramSocket ackSocket;
    HashMap<Integer, Packet> sentPackets;
    InetAddress receiverAddress;
    int receiverPort;

    public AckListener(Packet packet, DatagramSocket ackSocket, HashMap<Integer, Packet> sentPackets, InetAddress receiverAddress, int receiverPort) {
        this.packet = packet;
        this.ackSocket = ackSocket;
        this.sentPackets = sentPackets;
        this.receiverAddress = receiverAddress;
        this.receiverPort = receiverPort; // Receiver port number
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] receiveData = new byte[Packet.HeaderSize]; // Assuming ACK packets are small 
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                ackSocket.receive(receivePacket); //Start receiving Ack packet..
// Handle the received ACK packet
                Packet receivedAck = Packet.SetBytes(receivePacket.getData());
                if (receivedAck.GetPacketType() == PacketType.Ack) {
                    int ackNum = receivedAck.GetSequenceNumber();

                    sentPackets.remove(ackNum);
                    System.out.println("Received Ack for Packet: " + ackNum);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class PacketTimer extends TimerTask {

    private int sequenceNumber;
    private long startTime;
    private int timeoutDuration = 1000; //1000ms

    public PacketTimer(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        this.startTime = System.currentTimeMillis();
    }

    public boolean hasTimedOut() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        return elapsedTime >= timeoutDuration;
    }

    @Override
    public void run() {
// Do nothing here, the timer is used to check timeouts separately
    }
}

class TimeoutChecker implements Runnable {

    Packet packet;
    HashMap<Integer, PacketTimer> packetTimers;
    HashMap<Integer, Packet> sentPackets;

    @Override
    public void run() {
        while (true) {
            try {
                // Sleep for a while before checking again
                Thread.sleep(100);//100ms
                checkTimeouts();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public TimeoutChecker(Packet packet, HashMap<Integer, PacketTimer> packetTimers,
        HashMap<Integer, Packet> sentPackets) {
        this.packet = packet;
        this.packetTimers = packetTimers;
        this.sentPackets = sentPackets;
    }

    private void checkTimeouts() {
        for (HashMap.Entry<Integer, PacketTimer> entry : packetTimers.entrySet()) {
            int sequenceNumber = entry.getKey();
            PacketTimer packetTimer = entry.getValue();
            if (packetTimer.hasTimedOut() && sentPackets.containsKey(sequenceNumber)) {//timed out and no Ack received for packet
// Resend the packet associated with this sequence number
                DataSender.sendPacket(packet, -2);
                DataSender.startPacketTimer(sequenceNumber);
            }
        }
    }
}
