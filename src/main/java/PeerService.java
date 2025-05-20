
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface PeerService extends Remote {
    // Register with another peer
    void registerPeer(String peerID, String hostname, int port) throws RemoteException;
    
    // Receive a message from another peer
    void receiveMessage(ChatMessage message) throws RemoteException;
    
    // Get list of known peers
    List<PeerInfo> getKnownPeers() throws RemoteException;
}