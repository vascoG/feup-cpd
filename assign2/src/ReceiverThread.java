import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class ReceiverThread implements Runnable {

    private MulticastSocket multi_cast_socket;
    private MembershipProtocol protocol = new MembershipProtocol();


    private String ipAddress;
    private int port;
    private String node_id;
    private int node_port;

    public ReceiverThread(String ipAddress, int port, String node_id, int node_port) 
    {
        try {
            this.multi_cast_socket = new MulticastSocket(port);
            this.multi_cast_socket.joinGroup(InetAddress.getByName(ipAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.ipAddress=ipAddress;
        this.port=port;
        this.node_id = node_id;
        this.node_port = node_port;

    }

    @Override
    public void run() {
        while(true){
        byte[] buf = new byte[500]; 
        DatagramPacket p = new DatagramPacket(buf, 500);
        try {
            this.multi_cast_socket.receive(p);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        String string = new String(p.getData(), StandardCharsets.ISO_8859_1);
        System.out.println(string);

        Message message = new Message(buf);
        //create thread for this
        if(message.getMessage_type()==MessageType.JOIN && !this.node_id.equals(message.getSender_id())){
            System.out.println("Going to send MEMBERSHIP MESSAGE TO " + message.getSender_id() + " " + message.getSender_port());
            protocol.updateMembershipLog(message,this.node_id,this.node_port);
            protocol.sendMembershipMessage(this.node_id,this.node_port,message.getSender_id(),message.getSender_port());
        }

        //TODO: Handle Message
        }
    }
    
}
