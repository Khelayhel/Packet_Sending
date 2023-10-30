package network_project;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Packet {
    private int SequenceNumber;
    private PacketType packetType;
    private byte[] Data;
    private int DataSize;
    static final int MaxDataSize = 484; // Maximum data size for a packet
    static final int HeaderSize = 16; // Size of the header in bytes, each one 4 bytes
    
    public Packet(int SequenceNumber, PacketType packetType, int DataSize, byte[] Data) {
        this.SequenceNumber = SequenceNumber;
        this.packetType = packetType;
        this.Data = Data;
        this.DataSize = DataSize;
    }
    
    public int GetSequenceNumber() {
        return SequenceNumber;
    }
    
    public void SetSequenceNumber(int SequenceNumber) {
        this.SequenceNumber = SequenceNumber;
    }
    
    public int GetPacketSize() {
        return Data.length;
    }
    
    public PacketType GetPacketType() {
        return packetType;
    }
    
    public byte[] GetData() {
        return Data;
    }
    
    public int GetHeaderSize() {
        return HeaderSize;
    }
    
    public byte[] GetBytes() {
        ByteBuffer Buffer = ByteBuffer.allocate(HeaderSize + Data.length);
        Buffer.putInt(SequenceNumber);
        Buffer.putInt(packetType.ordinal());
        Buffer.putInt(Data.length);
        Buffer.put(Data);
        return Buffer.array();
    }
    
    public static Packet SetBytes(byte[] bytes) {
        ByteBuffer Buffer = ByteBuffer.wrap(bytes);
        int SequenceNumber = Buffer.getInt();
        PacketType packetType = PacketType.values()[Buffer.getInt()];
        int DataSize = Buffer.getInt();
        byte[] Data = new byte[DataSize];
        Buffer.get(Data);
        return new Packet(SequenceNumber, packetType, DataSize, Data);
    }
    
    /* Creating Data Packet */
    public static Packet CreateDataPacket(int SequenceNumber, byte[] Data) {
        return new Packet(SequenceNumber, PacketType.Data, Data.length, Data);
    }
    
    /* Creating Ack Packet */
    public static Packet CreateAckPacket(int SequenceNumber) {
        System.out.println("Create ACK for pkt: " + SequenceNumber);
        return new Packet(SequenceNumber, PacketType.Ack, 0, new byte[0]);
    }
    
    public static Packet CreateHandshakePacket(String message) {
        return new Packet(-1, PacketType.Handshake, message.length(), message.getBytes());
    }
    
    ////////////////////////////// Meta Info Packet //////////////////
    
    public static Packet CreateMetadataPacket(String FileName, long FileSize) {
        // Create a metadata packet
        Packet packet = new Packet(-1, PacketType.Metadata, (int) FileSize, SerializeMetadata(FileName, FileSize));
        return packet;
    }
    
    private static byte[] SerializeMetadata(String Filename, long FileSize) {
        try (ByteArrayOutputStream OutputStream = new ByteArrayOutputStream();
                DataOutputStream DataOutputStream = new DataOutputStream(OutputStream)) {
            DataOutputStream.writeUTF(Filename);
            DataOutputStream.writeLong(FileSize);
            return OutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
    
    public static MetaDataPacket ExtractMetadataPacket(Packet packet) {
        if (packet.GetPacketType() == PacketType.Metadata) {
            byte[] data = packet.GetData();
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                    DataInputStream dataInputStream = new DataInputStream(inputStream)) {
                String Filename = dataInputStream.readUTF();
                int FileSize = dataInputStream.readInt();
                return new MetaDataPacket(Filename, FileSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /* Handshake Info */
    public static HandShakePacket ExtractHandShakePacket(Packet packet) {
        if (packet.GetPacketType() == PacketType.Handshake) {
            byte[] data = packet.GetData();
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                    DataInputStream dataInputStream = new DataInputStream(inputStream)) {
                String Command = dataInputStream.readLine();
                return new HandShakePacket(Command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}

enum PacketType {
    Data,
    Ack,
    Metadata,
    Handshake
}