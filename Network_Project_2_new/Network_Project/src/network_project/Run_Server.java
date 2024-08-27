package network_project;
public class Run_Server {
	
	

	    public static void main(String[] args) {
	    	
	    	  String receiverAddress = args[0] ;//"127.0.0.1"; //args[0];	    	 
	          int receiverPort = Integer.parseInt(args[1]); //6500
	        
	          DataReceiver receiver = new DataReceiver(receiverPort,receiverAddress);
	        
	         

}//end main	    
}//end Run_Server
