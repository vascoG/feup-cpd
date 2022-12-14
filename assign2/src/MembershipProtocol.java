import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MembershipProtocol {

    public ClusterMembership clusterMembership = new ClusterMembership();

    public boolean join( String ip_mcast_addr, int ip_mcast_port, String node_id, int store_port, int number)
    {
        if(number>2){
            System.out.println("NUMBER OF TRIES TO JOIN EXCEEDED!");
            return false;
        }

        int membership_counter = getMembershipCounter(node_id);

        if(membership_counter%2!=0)
        {
            System.out.println("Trying to JOIN without LEAVING");
            return false;
        }
        //create serverSocket to accept MEMBERSHIP MESSAGES
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket;
                int join_port;
                try {
                    serverSocket = new ServerSocket(0);
                    serverSocket.setSoTimeout(5*1000);
                    join_port = serverSocket.getLocalPort();
                    System.out.println("LISTENING TO PORT FOR JOIN: " + join_port);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            //SEND MULTICAST JOIN
                            try {
                                MulticastSocket multi_cast_socket = new MulticastSocket(ip_mcast_port);
                                multi_cast_socket.joinGroup(InetAddress.getByName(ip_mcast_addr));

                                String msg = new Message(node_id, store_port, membership_counter, MessageType.JOIN,join_port).toString();
            
                                DatagramPacket datagram_packet = new DatagramPacket(msg.getBytes(), msg.length(),InetAddress.getByName(ip_mcast_addr), ip_mcast_port);//so 2 argumentos?
                                multi_cast_socket.send(datagram_packet);

                                multi_cast_socket.close();

                                setMembershipCounter(membership_counter+1, node_id);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                    }).start();

                    int counter =0;
                    while(counter < 3){
                        Socket socket = serverSocket.accept();
                        System.out.println("Accepted Membership connection");
                        InputStream input = socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                        String header = reader.readLine();
                        System.out.println(header);
                        StringBuilder membershipBuilder = new StringBuilder();
                        String line;
                         while((line=reader.readLine()) != null)
                         {
                             membershipBuilder.append(line).append("\n");
                         }
                         String membership = membershipBuilder.toString();
                        System.out.println(membership);
                        updateMembershipLog(membership,node_id);
                        socket.close();
                        counter++;
                    }
                serverSocket.close();
                System.out.println("CLOSED JOIN PORT");
                } 
                catch(SocketTimeoutException e)
                {
                    setMembershipCounter(membership_counter, node_id);
                    join(ip_mcast_addr, ip_mcast_port, node_id, store_port,number+1);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                
            }
            
        });

        thread.start();


       

        return true;
    }

    public boolean leave(String ip_mcast_addr, int ip_mcast_port, String node_id, int store_port) 
    {
        int counter = getMembershipCounter(node_id);
        if(counter%2==0)
        {
            System.out.println("Trying to LEAVE without JOINING");
            return false;
        } 

        new Thread(new Runnable() {

            @Override
            public void run() {
                File directory = new File("./"+node_id);
                Member newSuccesor = clusterMembership.findSucessor(KeyHash.getSHA256(node_id));
                for(File file : directory.listFiles())
                {
                    String fileName = file.getName();
                    String key = fileName.split(".txt")[0];
                    if(!fileName.equals("membership_log.txt") && !fileName.equals("membership_counter.txt"))
                    {
                        String value;
                        if(clusterMembership.findSucessor(KeyHash.getSHA256(key)).ipAddress.equals(node_id)){
                        try {
                            value = Files.readString(Paths.get("./"+node_id + "/" + fileName));

                            Member successorOfSuccessor = clusterMembership.findSucessor(newSuccesor.hashKey);

                            Socket socket = new Socket("localhost", successorOfSuccessor.port);
                            System.out.println("GOING TO SEND PUT BECAUSE I AM LEAVING");

                            OutputStream output = socket.getOutputStream();
                            PrintWriter writer = new PrintWriter(output, true);

                            Message message = new Message(node_id, store_port, key,value,MessageType.PUT);

                            writer.println(message.toString());

                            socket.close();
                        } catch (Exception e) {
                        e.printStackTrace();
                        }
                    }
                    file.delete();
                }
                }
            }
            
        }).start();

        //SEND MULTICAST LEAVE
        try {
            MulticastSocket multi_cast_socket = new MulticastSocket(ip_mcast_port);
            multi_cast_socket.joinGroup(InetAddress.getByName(ip_mcast_addr));


            String msg = new Message(node_id, store_port, counter, MessageType.LEAVE).toString();
            
            DatagramPacket datagram_packet = new DatagramPacket(msg.getBytes(), msg.length(),InetAddress.getByName(ip_mcast_addr), ip_mcast_port);//so 2 argumentos?
            multi_cast_socket.send(datagram_packet);

            multi_cast_socket.close();

            setMembershipCounter(counter+1, node_id);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    protected void updateMembershipLog(String membership, String node_id) {
        FileWriter fw;
        File file = new File("./"+node_id+"/membership_log.txt");
        //32 logs
        try {
            String fr=getMembershipLog(node_id);
            String [] arrayReceivedLog = membership.split("\n");
            List<String> resultLog = new ArrayList<>();
            if(fr.length()==0)
            {   
                fw = new FileWriter(file);
                fw.write(membership);
                fw.close();
                for(int i = 0 ; i<arrayReceivedLog.length;i++)
                {
                    String [] log_line = arrayReceivedLog[i].split("-");
                    clusterMembership.insert(new Member(log_line[0], Integer.parseInt(log_line[1]),Integer.parseInt(log_line[2])));
                }
                return;
            }
            String [] arrayLog=fr.split("\n");
            resultLog.addAll(Arrays.asList(arrayLog));
            for(int i = 0;i<arrayReceivedLog.length;i++)
            {   
                boolean found = false;
                String [] log_received_line = arrayReceivedLog[i].split("-");
                int received_counter = Integer.parseInt(log_received_line[1]);
                String received_log = log_received_line[0];
                clusterMembership.insert(new Member(received_log,received_counter,Integer.parseInt(log_received_line[2])));
                for(int j=0;j<arrayLog.length;j++)
                {
                    String [] log_line = arrayLog[j].split("-");
                    if(received_log.contains(log_line[0]))
                    {
                        found = true;
                        
                        if(received_counter<=Integer.parseInt(log_line[1]))
                            break;
                        else
                        {
                            resultLog.remove(j);
                            resultLog.add(arrayReceivedLog[i]);
                            break;
                        }
                    }   
                }
                if(!found){
                    resultLog.add(arrayReceivedLog[i]);   
                }
            }

            fw = new FileWriter(file);
            StringBuilder logbuilder  = new StringBuilder();
            int initial = 0;
            if(resultLog.size()>32)
                initial = resultLog.size()-32;
            for(int i = initial;i<resultLog.size();i++)
            {
                logbuilder.append(resultLog.get(i));
                logbuilder.append("\n");
            }
            String log = logbuilder.toString();
            fw.write(log);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void sendMembershipMessage(String ipAddress, int port, String sender_id, int sender_port) 
    {
        //wait random time
        int random = (new Random().nextInt(11)+1)*10;
        
        String log = getMembershipLog(ipAddress);
        if(log.isEmpty())
            random+=500;
        else
            random+=500/log.length();

        try {
            Thread.sleep(random);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        try {
            Socket socket = new Socket("localhost", sender_port);

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            Message message = new Message(ipAddress, port, log, MessageType.MEMBERSHIP);
            writer.println(message.toString());

            socket.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
   


    }

    public String getMembershipLog(String ipAddress) {
        try {
            String log = Files.readString(Paths.get("./"+ipAddress+"/membership_log.txt"));
            return log;
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
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

    public void updateMembershipLogOnJoinLeave(Message message, String node_id) {
        FileWriter fw;
        File file = new File("./"+node_id+"/membership_log.txt");
        //32 logs
        try {
            String fr=getMembershipLog(node_id);
            String [] arrayLog=fr.split("\n");
            for(int i=0;i<arrayLog.length;i++){
                String event=arrayLog[i];
                if(event.contains(message.getSender_id())){
                    String[] event_line = event.split("-");
                    int counter=Integer.parseInt(event_line[1]);
                    if(counter< message.getMembership_counter()){
                        String [] newArrayLog=updateArray(arrayLog,i);
                        fw=new FileWriter(file);
                        String logString="";
                        clusterMembership.insert(new Member(message.getSender_id(), message.getMembership_counter(),message.getSender_port()));
                        String newLog=message.getSender_id()+"-"+message.getMembership_counter()+"-"+message.getSender_port();
                        for(String elem:newArrayLog){
                            logString+=elem+"\n";
                        }
                        logString+=newLog;
                        fw.write(logString);
                        fw.close();
                        return;
                    }else{
                        return;
                    }
                }
            }
            String log;
            clusterMembership.insert(new Member(message.getSender_id(), message.getMembership_counter(),message.getSender_port()));
            if(fr.length()==0)
                {
                    fw = new FileWriter(file,false);
                   log=message.getSender_id() + "-" + message.getMembership_counter()+"-"+message.getSender_port() + "\n";
                }
            else
                {   
                    fw = new FileWriter(file,true);
                    if(arrayLog.length<32){
                    log= message.getSender_id() + "-" + message.getMembership_counter()+"-"+message.getSender_port() + "\n";
                    }
                    else
                    {
                        StringBuilder logbuilder = new StringBuilder();
                        for(int i = 1;i<arrayLog.length;i++)
                        {
                            logbuilder.append(arrayLog[i]);
                            logbuilder.append("\n");
                        }
                        log = logbuilder.toString();
                    }
                }fw.write(log);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String [] updateArray(String[] arrayLog, int index) {
        String [] r = new String[arrayLog.length-1];
        for(int i=0;i<index;i++)
            r[i]=arrayLog[i];
        for(int i=index;i<arrayLog.length-1;i++){
            r[i]=arrayLog[i+1];
        }
        return r;
    }

    public void updateFilesOnJoin(Message message, String node_id, int node_port) {
        File directory = new File("./"+node_id);
        String keyNewNode = KeyHash.getSHA256(message.getSender_id());
        Member successor = clusterMembership.findSucessor(keyNewNode);
        Member predecessor = clusterMembership.findPredecessor(keyNewNode);
        Member sucessorOfSucessor = clusterMembership.findSucessor(successor.hashKey);
        if(successor.ipAddress.equals(node_id))
        { 
            for(File file : directory.listFiles())
            {
                String fileName = file.getName();
                String key = fileName.split(".txt")[0];
                if(!fileName.equals("membership_log.txt") && !fileName.equals("membership_counter.txt"))
                {
                    if(clusterMembership.findSucessor(key).ipAddress.equals(message.getSender_id()) || clusterMembership.findSucessor(key).ipAddress.equals(node_id)){
                    String value;
                    try {
                        value = Files.readString(Paths.get("./"+node_id + "/" + fileName));

                        Socket socket = new Socket("localhost", message.getSender_port());
                        System.out.println("GOING TO SEND PUT BECAUSE HE IS JOINING");

                        OutputStream output = socket.getOutputStream();
                        PrintWriter writer = new PrintWriter(output, true);

                        Message msg = new Message(node_id, node_port, key,value,MessageType.PUT);

                        writer.println(msg.toString());

                        socket.close();
                    } catch (Exception e) {
                    e.printStackTrace();
                    }
                }
                else if(clusterMembership.findSucessor(key).ipAddress.equals(predecessor.ipAddress))
                    file.delete();
            }
            }
            
        }
        else if(predecessor.ipAddress.equals(node_id))
        {
            for(File file : directory.listFiles())
            {
                String fileName = file.getName();
                String key = fileName.split(".txt")[0];
                if(!fileName.equals("membership_log.txt") && !fileName.equals("membership_counter.txt"))
                {
                    if(clusterMembership.findSucessor(key).ipAddress.equals(node_id)){
                    String value;
                    try {
                        value = Files.readString(Paths.get("./"+node_id + "/" + fileName));

                        Socket socket = new Socket("localhost", message.getSender_port());
                        System.out.println("GOING TO SEND PUT BECAUSE HE IS JOINING");

                        OutputStream output = socket.getOutputStream();
                        PrintWriter writer = new PrintWriter(output, true);

                        Message msg = new Message(node_id, node_port, key,value,MessageType.PUT);

                        writer.println(msg.toString());

                        socket.close();
                    } catch (Exception e) {
                    e.printStackTrace();
                    }
                }
                else if(clusterMembership.findSucessor(key).ipAddress.equals(successor.ipAddress))
                    file.delete();
            }
            }
        }
        else if(sucessorOfSucessor.ipAddress.equals(node_id))
        {
            for(File file : directory.listFiles())
            {
                String fileName = file.getName();
                String key = fileName.split(".txt")[0];
                if(!fileName.equals("membership_log.txt") && !fileName.equals("membership_counter.txt"))
                {
                    if(clusterMembership.findSucessor(key).hashKey.equals(keyNewNode))
                        file.delete();
            }
            }
        }
    }

    public void updateFilesOnLeave(Message message, String node_id, int node_port) {
        File directory = new File("./"+node_id);
        String keyOldNode = KeyHash.getSHA256(message.getSender_id());
        Member successor = clusterMembership.findSucessor(keyOldNode);
        Member predecessor = clusterMembership.findPredecessor(keyOldNode);
        if(successor.ipAddress.equals(node_id))
        { 
            for(File file : directory.listFiles())
            {
                String fileName = file.getName();
                String key = fileName.split(".txt")[0];
                if(!fileName.equals("membership_log.txt") && !fileName.equals("membership_counter.txt"))
                {
                    if(clusterMembership.findSucessor(key).ipAddress.equals(node_id)){
                    String value;
                    try {
                        value = Files.readString(Paths.get("./"+node_id + "/" + fileName));

                        Socket socket = new Socket("localhost", predecessor.port);
                        System.out.println("GOING TO SEND PUT BECAUSE SOMEONE IS LEAVING");

                        OutputStream output = socket.getOutputStream();
                        PrintWriter writer = new PrintWriter(output, true);

                        Message msg = new Message(node_id, node_port, key,value,MessageType.PUT);

                        writer.println(msg.toString());

                        socket.close();
                    } catch (Exception e) {
                    e.printStackTrace();
                    }
                }
            }
            }
            
        }
        else if(predecessor.ipAddress.equals(node_id))
        {
            for(File file : directory.listFiles())
            {
                String fileName = file.getName();
                String key = fileName.split(".txt")[0];
                if(!fileName.equals("membership_log.txt") && !fileName.equals("membership_counter.txt"))
                {
                    if(clusterMembership.findSucessor(key).ipAddress.equals(node_id)){
                    String value;
                    try {
                        value = Files.readString(Paths.get("./"+node_id + "/" + fileName));

                        Socket socket = new Socket("localhost", successor.port);
                        System.out.println("GOING TO SEND PUT BECAUSE SOMEONE IS LEAVING");

                        OutputStream output = socket.getOutputStream();
                        PrintWriter writer = new PrintWriter(output, true);

                        Message msg = new Message(node_id, node_port, key,value,MessageType.PUT);

                        writer.println(msg.toString());

                        socket.close();
                    } catch (Exception e) {
                    e.printStackTrace();
                    }
                }
            }
            }

        }
    }



}
