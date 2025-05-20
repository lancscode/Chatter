import java.io.Serializable;

public class PeerInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String peerID;
    private final String hostname;
    private final int port;
    
    public PeerInfo(String peerID, String hostname, int port) {
        this.peerID = peerID;
        this.hostname = hostname;
        this.port = port;
    }
    
    // Getters
    public String getPeerID() { return peerID; }
    public String getHostname() { return hostname; }
    public int getPort() { return port; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo that = (PeerInfo) o;
        return peerID.equals(that.peerID);
    }
    
    @Override
    public int hashCode() {
        return peerID.hashCode();
    }
}