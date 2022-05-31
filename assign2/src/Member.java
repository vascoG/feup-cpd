public class Member {
    String ipAddress;
    int counter;
    String hashKey;

    int port;
    
    public Member(String ipAddress, int counter,int port) {
        this.ipAddress = ipAddress;
        this.counter = counter;
        this.hashKey= KeyHash.getSHA256(ipAddress);
        this.port=port;
    }

    public boolean isInsideCluster()
    {
        return counter%2==0;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
    
}
