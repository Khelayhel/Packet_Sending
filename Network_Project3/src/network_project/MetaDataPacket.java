
package network_project;

public class MetaDataPacket {

    private String filename;
    private int fileSize;

    public MetaDataPacket(String filename, int fileSize) {
        this.filename = filename;
        this.fileSize = fileSize;
    }

    public String getFilename() {
        return filename;
    }

    public int getFileSize() {
        return fileSize;
    
}}
