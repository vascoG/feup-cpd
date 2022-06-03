import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiverTCP implements Runnable {

    private int node_port;
    private String node_id;

    private MembershipProtocol protocol;

    public ReceiverTCP(int node_port, String node_id, MembershipProtocol protocol) {
        this.node_port = node_port;
        this.node_id = node_id;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(node_port);
            System.out.println("LISTENING TO PORT (KeyStore Operations): " + node_port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Accepted conection KeyStore Operation");
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String header = reader.readLine();
                System.out.println(header);
                String[] arrayHeader = header.split(" ");
                String operation = arrayHeader[0];
                switch (operation) {
                    case "PUT":
                        String key = arrayHeader[3];
                        String line;
                        StringBuilder value = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            value.append(line).append("\n");
                        }
                        put(node_id, key, value.toString());
                        break;
                    case "DELETE":
                        delete(node_id,arrayHeader[3]);
                        break;
                    case "PUTREPLICATE":
                        String lineR;
                        StringBuilder valueR = new StringBuilder();
                        while ((lineR = reader.readLine()) != null) {
                            valueR.append(lineR).append("\n");
                        }
                        putReplicate(node_id,arrayHeader[3],valueR.toString());
                        break;
                    case "DELETEREPLICATE":
                        deleteReplicate(node_id,arrayHeader[3]);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void delete(String node_id, String key) {
        File keyFile= new File("./"+node_id+"/"+key+".txt");
        if(keyFile.exists())
            keyFile.delete();
    }
    private void deleteReplicate(String node_id, String key) {
        Member sucessor=protocol.clusterMembership.findSucessor(KeyHash.getSHA256(this.node_id));
        Member predecessor=protocol.clusterMembership.findPredecessor(KeyHash.getSHA256(this.node_id));

        File keyFile= new File("./"+node_id+"/"+key+".txt");
        if(keyFile.exists()){
            keyFile.delete();
            try {
                Socket socketSuc = new Socket("localhost", sucessor.port);
                Socket socketPre = new Socket("localhost", predecessor.port);
                OutputStream outputSuc = socketSuc.getOutputStream();
                PrintWriter writerSuc = new PrintWriter(outputSuc, true);

                OutputStream outputPre = socketPre.getOutputStream();
                PrintWriter writerPre = new PrintWriter(outputPre, true);

                Message message = new Message(this.node_id, this.node_port, key,MessageType.DELETE);
                writerSuc.println(message.toString());
                writerPre.println(message.toString());

                socketSuc.close();
                socketPre.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }
    }


    private void put(String node_id, String key, String value) {
        try {
            File keyFile = new File("./" + node_id + "/" + key + ".txt");
            if (keyFile.exists())
                keyFile.delete();
            try {
                keyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileWriter fw = new FileWriter(keyFile);
            fw.write(value);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void putReplicate(String node_id, String key, String value) {
        try{
            Member sucessor=protocol.clusterMembership.findSucessor(KeyHash.getSHA256(this.node_id));
            Member predecessor=protocol.clusterMembership.findPredecessor(KeyHash.getSHA256(this.node_id));

            File keyFile= new File("./"+node_id+"/"+key+".txt");
            if(keyFile.exists())
                keyFile.delete();
            try {
                keyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileWriter fw =new FileWriter(keyFile);
            fw.write(value);
            fw.close();

            Message message = new Message(this.node_id, this.node_port, key,value,MessageType.PUT);

            try {
                Socket socketSuc = new Socket("localhost", sucessor.port);
                OutputStream outputSuc = socketSuc.getOutputStream();
                PrintWriter writerSuc = new PrintWriter(outputSuc, true);
                writerSuc.println(message.toString());
                socketSuc.close();
            } catch (Exception e) {
                MulticastSocket multi_cast_socket = new MulticastSocket(4003);
                multi_cast_socket.joinGroup(InetAddress.getByName("224.0.0.0"));
                String msg = new Message(sucessor.ipAddress, sucessor.port, sucessor.counter+1, MessageType.LEAVE).toString();
                DatagramPacket datagram_packet = new DatagramPacket(msg.getBytes(), msg.length(),InetAddress.getByName("224.0.0.0"), 4003);
                multi_cast_socket.send(datagram_packet);
                multi_cast_socket.close();
            }

            try {
            
                Socket socketPre = new Socket("localhost", predecessor.port);
                OutputStream outputPre = socketPre.getOutputStream();
                PrintWriter writerPre = new PrintWriter(outputPre, true);
                writerPre.println(message.toString());
                socketPre.close();
            } catch (Exception e) {
                MulticastSocket multi_cast_socket = new MulticastSocket(4003);
                multi_cast_socket.joinGroup(InetAddress.getByName("224.0.0.0"));
                String msg = new Message(predecessor.ipAddress, predecessor.port, predecessor.counter+1, MessageType.LEAVE).toString();
                DatagramPacket datagram_packet = new DatagramPacket(msg.getBytes(), msg.length(),InetAddress.getByName("224.0.0.0"), 4003);
                multi_cast_socket.send(datagram_packet);
                multi_cast_socket.close();
            }
           
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}
