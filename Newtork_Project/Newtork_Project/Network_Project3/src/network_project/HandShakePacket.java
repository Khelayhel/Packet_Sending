package network_project;

public class HandShakePacket {

    private String command;  // changed to lowercase

    public HandShakePacket(String command) {  // changed to lowercase
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {  // changed to lowercase
        this.command = command;
    }
}
