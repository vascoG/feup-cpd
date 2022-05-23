import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MembershipProtocol {

    public void join( String ip_mcast_addr, int ip_mcast_port, String node_id, int store_port)
    {

        int counter = getMembershipCounter(node_id);
        if(counter%2!=0)
        {
            System.out.println("Trying to JOIN without LEAVING");
            return;
        }
        //create serverSocket to accept MEMBERSHIP MESSAGES
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket;
                try {
                    serverSocket = new ServerSocket(store_port);
                    System.out.println("LISTENING TO PORT " + store_port);
                    int counter =0;
                    while(counter < 3){
                        Socket socket = serverSocket.accept();
                        System.out.println("Accepted conection");
                        InputStream input = socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                        String message = reader.readLine();
                        System.out.println(message);
                        String membership = reader.readLine();
                        System.out.println(membership);
                        updateMembershipLog(membership,node_id);
                        //TODO: Better while condition (different logs?)
                        socket.close();
                        counter++;
                    }
                serverSocket.close();
                System.out.println("CLOSED");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                
            }
            
        });

        thread.start();


        //SEND MULTICAST JOIN
        try {
            MulticastSocket multi_cast_socket = new MulticastSocket(ip_mcast_port);
            multi_cast_socket.joinGroup(InetAddress.getByName(ip_mcast_addr));


            String msg = new Message(node_id, store_port, counter, MessageType.JOIN).toString();
            
            DatagramPacket datagram_packet = new DatagramPacket(msg.getBytes(), msg.length(),InetAddress.getByName(ip_mcast_addr), ip_mcast_port);//so 2 argumentos?
            multi_cast_socket.send(datagram_packet);

            multi_cast_socket.close();

            setMembershipCounter(counter+1, node_id);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }



        // Atualizar chaves 
    }

    protected void updateMembershipLog(String membership, String node_id) {
        FileWriter fw;
        File file = new File("./"+node_id+"/membership_log.txt");
        //32 logs
        try {
            String fr=getMembershipLog(node_id);
            String [] arrayReceivedLog = membership.split(" ");
            List<String> resultLog = new ArrayList<>();
            String [] arrayLog=fr.split(" ");
            resultLog.addAll(Arrays.asList(arrayLog));
            for(int i = 0;i<arrayReceivedLog.length;i++)
            {   
                boolean found = false;
                String [] log_received_line = arrayReceivedLog[i].split("-");
                for(int j=0;j<arrayLog.length;j++)
                {
                    String [] log_line = arrayLog[j].split("-");
                    if(log_received_line[0].contains(log_line[0]))
                    {
                        found = true;
                        if(Integer.parseInt(log_received_line[1])<=Integer.parseInt(log_line[1]))
                            break;
                        else
                        {
                            resultLog.set(j, arrayReceivedLog[i]);
                            break;
                        }
                    }   
                }
                if(!found)
                resultLog.add(arrayReceivedLog[i]);   
            }

            fw = new FileWriter(file);
            String log = String.join(" ", resultLog);
            fw.write(log);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void sendMembershipMessage(String ipAddress, int port, String sender_id, int sender_port) 
    {
        //wait random time
        

        //get log and table (maybe 2 threads)
        String log = getMembershipLog(ipAddress);

        try {
            System.out.println("TRYING TO CONNECT TO SOCKET " + sender_port);
            Socket socket = new Socket("localhost", sender_port);
            System.out.println("CONNECTED");

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            Message message = new Message(ipAddress, port, log, MessageType.MEMBERSHIP);
            writer.println(message.toString());

            System.out.println(message.toString());

            socket.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
   


    }

    private String getMembershipLog(String ipAddress) {
        try {
            String log = Files.readString(Paths.get("./"+ipAddress+"/membership_log.txt"));
            return log;
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public void setMembershipLog(String log,String node_id) {
        FileWriter fw;
        try {
            File file = new File("./"+node_id+"/membership_log.txt");
            fw = new FileWriter(file);
            fw.write(log);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getMembershipCounter(String ipAddress) {
        try {
            String log = Files.readString(Paths.get("./"+ipAddress+"/membership_counter.txt"));
            return Integer.parseInt(log);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void setMembershipCounter(Integer counter,String node_id) {
        FileWriter fw;
        try {
            File file = new File("./"+node_id+"/membership_counter.txt");
            fw = new FileWriter(file);
            fw.write(counter.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateMembershipLogOnJoin(Message message, String node_id) {
        FileWriter fw;
        File file = new File("./"+node_id+"/membership_log.txt");
        //32 logs
        try {
            String fr=getMembershipLog(node_id);
            String [] arrayLog=fr.split(" ");
            for(int i=0;i<arrayLog.length;i++){
                String event=arrayLog[i];
                if(event.contains(message.getSender_id())){
                    int counter=Integer.parseInt(event.split("-")[1]);
                    if(counter< message.getMembership_counter()){
                      //TODO:: TESTE
                        String [] newArrayLog=updateArray(arrayLog,i);
                        fw=new FileWriter(file);
                        String logString="",newLog=message.getSender_id()+"-"+message.getMembership_counter();
                        for(String elem:newArrayLog){
                            logString+=elem+" ";
                        }
                        logString.concat(newLog);
                        fw.write(logString);
                        fw.close();
                        return;
                    }else{
                        return;
                    }
                }
            }

            fw = new FileWriter(file,true);
            String log= " " + message.getSender_id() + "-" + message.getMembership_counter();
            fw.write(log);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String [] updateArray(String[] arrayLog, int index) {
        for(int i=index;i<=arrayLog.length;i++){
            arrayLog[i]=arrayLog[i+1];
        }
        return arrayLog;
    }

}
