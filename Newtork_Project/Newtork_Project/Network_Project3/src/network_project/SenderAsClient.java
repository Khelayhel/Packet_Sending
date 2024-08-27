package network_project;
import java.io.File;

public class SenderAsClient {
	
	   public static void main(String[] args) {
		   
 String receiverAddress = args[0]; //"127.0.0.1"; //
 int receiverPort = Integer.parseInt(args[1]); //6500 ;
 String filePath = args[2]; //"send_data.txt"
 double packetLossProbability = Double.parseDouble(args[3]); ; // Adjust as needed (0.1, 0.3, or 0.6)
 
 	
  DataSender sender = new DataSender(receiverAddress, receiverPort , filePath , packetLossProbability );
  
  
	   }  

}
