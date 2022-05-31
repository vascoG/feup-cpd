import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiverTCP implements Runnable {

    private int node_port;
    private String node_id;

    public ReceiverTCP(int node_port, String node_id) {
        this.node_port = node_port;
        this.node_id = node_id;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(node_port);
            System.out.println("LISTENING TO PORT (KeyStore Operations)" + node_port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Accepted conection");
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

}
