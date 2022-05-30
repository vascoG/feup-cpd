public class Member {
    String ipAddress;
    int counter;
    String hashKey;
    
    public Member(String ipAddress, int counter) {
        this.ipAddress = ipAddress;
        this.counter = counter;
        this.hashKey= KeyHash.getSHA256(ipAddress);
    }

    public boolean isInsideCluster()
    {
        return counter%2==0;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
    
}
