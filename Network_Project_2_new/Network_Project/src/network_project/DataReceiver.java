package network_project;

// Import required packages
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DataReceiver {
    // Declare instance variables
    private Set<Integer> receivedSequenceNumbers;  // To keep track of received sequence numbers
    private int expectedSequenceNumber;  // Expected sequence number for the next packet
    private int receiverPort;  // Port number for the receiver
    private DatagramSocket socket;  // Datagram socket for communication
    DatagramPacket receivePacket;  // Datagram packet to receive data
    private int senderPort;  // Port number for the sender
    private InetAddress senderAddress;  // IP address of the sender
    private InetAddress receiverAddress;  // IP address of the receiver
    private HashMap<Integer, Packet> receivedPackets;  // To store received packets

    // Constructor
    public DataReceiver(int receiverPort, String receiverAddress) {
        try {
            senderPort = 6000;
            this.receiverPort = receiverPort;
            this.receiverAddress = InetAddress.getByName(receiverAddress);
            this.socket = new DatagramSocket(receiverPort);
            this.receivedPackets = new HashMap<>();
            this.receivedSequenceNumbers = new HashSet<>();
            this.expectedSequenceNumber = 1;
            receivePackets();
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
    }

    // Method to deserialize received data into a Packet object
    private Packet deserializePacket(byte[] data, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
        try {
            int sequenceNumber = buffer.getInt();
            System.out.println("Receive Pkt# :" + sequenceNumber);
            // System.out.println("Read sequenceNumber: " + sequenceNumber);

            PacketType packetType = PacketType.values()[buffer.getInt()];
            //System.out.println("Read packetType: " + packetType);

            int dataSize = buffer.getInt();
            //System.out.println("Read dataSize: " + dataSize);

            if (dataSize > 0) {
                byte[] packetData = new byte[dataSize];
                buffer.get(packetData);
                //System.out.println("Read packetData: " + Arrays.toString(packetData));
                return new Packet(sequenceNumber, packetType, packetData.length,
                        packetData);
            } else {
                return new Packet(sequenceNumber, packetType, 0, null);
            }
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
            return null; // Handle the error, log it, or return null
        }
    } 
    
    private void processMetaDataPacket(Packet packet) {
        MetaDataPacket metadataPacket = Packet.ExtractMetadataPacket(packet);
        if (metadataPacket != null) {
            System.out.println("Metadata inbound; Filename specified is: " + metadataPacket.getFilename());

          
            sendAck(packet.GetSequenceNumber());

        } 
    }


    public void sendAck(int sequenceNumber) {
        Packet ackPacket = Packet.CreateAckPacket(sequenceNumber);
        sendPacket(ackPacket);
        System.out.println("ACK for sequence number successfully sent: "+ ackPacket.GetSequenceNumber() + " " + ackPacket.GetPacketType());
    }

   
    private void sendPacket(Packet packet) {
        try {

            byte[] packetBytes = packet.GetBytes();
            DatagramPacket sendPacket = new DatagramPacket(packetBytes,
                    packetBytes.length, senderAddress, senderPort);
            socket.send(sendPacket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void processHandshakePacket(Packet packet) {
        HandShakePacket handshakePacket = Packet.ExtractHandShakePacket(packet);
        if (handshakePacket != null) {

            if (handshakePacket.getCommand().equalsIgnoreCase("Finish_File")) {

                String filename = "received_data.txt";
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(filename, true);
                    for (int i = 1; i <= receivedPackets.size(); i++) {
                        if (receivedPackets.containsKey(i)) {
                            fileOutputStream.write(receivedPackets.get(i).GetData());
                            sendAck(i);
                        }
                    }
                    System.out.println("File has been successfully saved.");
                    fileOutputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    public void receivePackets() {
        System.out.println("Initializing incoming data stream..");
        while (true) {
            byte[] receiveData = new byte[Packet.MaxDataSize + Packet.HeaderSize];
            receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                //Start Receiving
                socket.receive(receivePacket);
                senderAddress = receivePacket.getAddress();//sender Address
                Packet receivedPacket = deserializePacket(receivePacket.getData(),
                        receivePacket.getLength());
                int sequenceNumber = receivedPacket.GetSequenceNumber();

                if (receivedPacket.GetPacketType() == PacketType.Metadata) {
                    processMetaDataPacket(receivedPacket);
                } else if (receivedPacket.GetPacketType() == PacketType.Data) {

                    //Duplicate Packets..
                    if (receivedSequenceNumbers.contains(sequenceNumber)) {
                        // This is a duplicate packet, ignore it
                        System.out.println("Duplicate packet with sequence number: " + sequenceNumber);
                        continue;
                    }

                    //Is packet in order?
                    if (sequenceNumber == expectedSequenceNumber) {

                        receivedPackets.put(sequenceNumber, receivedPacket);
                        // Configure the expected sequence number for the next data packet
                        expectedSequenceNumber++;
                    } else if (sequenceNumber != expectedSequenceNumber) {

                        // Disordered packet received; you may decide to either discard or cache for future handling.
                        System.out.println("Out of order packet received with sequence number: " + sequenceNumber);
                        receivedPackets.put(sequenceNumber, receivedPacket);
                    }
                   
                    receivedSequenceNumbers.add(sequenceNumber);
                }
                else if (receivedPacket.GetPacketType() == PacketType.Ack) {
                   

                } else if (receivedPacket.GetPacketType() == PacketType.Handshake) {

                    processHandshakePacket(receivedPacket);
                } else {
                    System.out.println("Unknown packet type: " + receivedPacket.GetPacketType());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




}
