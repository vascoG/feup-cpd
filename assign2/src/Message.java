import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Message {


    private final String crlf = "\r\n";
    private final String last_crlf = "\r\n\r\n";

    private MessageType message_type;

    private String sender_id;
    private int sender_port;

    private int join_port;

    private int membership_counter;

    private String membership_log;

    private String value;

    private String key;

    public MessageType getMessage_type() {
        return message_type;
    }

    public String getSender_id() {
        return sender_id;
    }

    public int getSender_port() {
        return sender_port;
    }

    public int getjoin_port() {
        return join_port;
    }

    public int getMembership_counter() {
        return membership_counter;
    }


    public Message(byte[] data){
        String message = new String(data, StandardCharsets.ISO_8859_1);

        String header = message.split(this.last_crlf,2)[0];

        processHeader(header);

    }

    public Message(String ipAddress, int port, String string, MessageType messageType) {
        switch (messageType){
            case DELETE:
                this.key = string; break;
            case MEMBERSHIP:
                this.membership_log=string; break;
            default:break;
        }
        this.message_type = messageType;
        this.sender_id = ipAddress;
        this.sender_port = port;
    }

    public Message(String node_id, int store_port, int i, MessageType messageType) {
        this.message_type = messageType;
        this.sender_id = node_id;
        this.sender_port = store_port;
        this.membership_counter = i;
    }

    public Message(String node_id, int store_port, int membership_counter, MessageType join, int join_port) {
        this.message_type = join;
        this.sender_id = node_id;
        this.sender_port = store_port;
        this.membership_counter = membership_counter;
        this.join_port = join_port;
    }

    public Message(String node_id, int store_port, String key, String value, MessageType put) {
        this.message_type = put;
        this.sender_id = node_id;
        this.sender_port = store_port;
        this.key = key;
        this.value=value;
    }

    private void processHeader(String header) {
        ArrayList<String> headerLines = new ArrayList<>(Arrays.asList(header.split(crlf)));

        ArrayList<String> firstHeader = new ArrayList<>(Arrays.asList(headerLines.remove(0).split("\\s+")));

        this.message_type = MessageType.valueOf(firstHeader.remove(0).trim());
        this.sender_id = firstHeader.remove(0).trim();
        this.sender_port = Integer.parseInt(firstHeader.remove(0).trim());

        switch (this.message_type) {
            case JOIN:
                this.membership_counter = Integer.parseInt(firstHeader.remove(0).trim());
                this.join_port = Integer.parseInt(firstHeader.remove(0).trim());
                break;
            case LEAVE:
                this.membership_counter = Integer.parseInt(firstHeader.remove(0).trim());
                break;
            case MEMBERSHIP:
                this.membership_log = headerLines.remove(0).trim();
            //TODO: Process stuff
            default:
                break;
        }

    }

    public String toString()
    {
        switch(this.message_type)
        {
            case MEMBERSHIP:
                return "MEMBERSHIP " + this.sender_id + " " + this.sender_port + " " + crlf + this.membership_log + last_crlf;
            case JOIN: 
                return "JOIN " + this.sender_id + " " + this.sender_port + " " + this.membership_counter + " " + this.join_port + last_crlf;
            case LEAVE:
                return "LEAVE " + this.sender_id + " " + this.sender_port + " " + this.membership_counter + last_crlf;
            case PUT: 
                return "PUT " + this.sender_id + " "+ this.sender_port + " " + this.key + crlf + this.value + last_crlf;
            case PUTREPLICATE:
                return "PUTREPLICATE " + this.sender_id + " "+ this.sender_port + " " + this.key + crlf + this.value + last_crlf;
            case DELETE:
                return "DELETE " + this.sender_id + " "+ this.sender_port + " " + this.key + last_crlf;
            default: 
                return "ERROR";
        }
    }
    
}
