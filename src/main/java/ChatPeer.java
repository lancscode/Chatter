
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.net.InetAddress;

public class ChatPeer implements PeerService {
    private final String peerID;
    private final String hostname;
    private final int port;
    private final Map<String, PeerInfo> knownPeers = new HashMap<>();
    private final Set<String> receivedMessageIDs = new HashSet<>();
    
    public ChatPeer(String username, int port) {
        this.peerID = username;
        this.port = port;
        
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
            startService();
        } catch (Exception e) {
            System.err.println("Error starting peer: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private void startService() throws Exception {
        // Create or get registry
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(port);
        }
        
        // Export and bind this peer
        PeerService stub = (PeerService) UnicastRemoteObject.exportObject(this, 0);
        registry.rebind("PeerService", stub);
        
        System.out.println("Peer started: " + peerID + " on " + hostname + ":" + port);
    }
    
    public void connectToPeer(String hostname, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            PeerService remotePeer = (PeerService) registry.lookup("PeerService");
            
            // Register with remote peer
            remotePeer.registerPeer(peerID, this.hostname, this.port);
            
            System.out.println("Connected to peer at " + hostname + ":" + port);
        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
        }
    }
    
    public void sendMessage(String content) {
        ChatMessage message = new ChatMessage(peerID, content);
        System.out.println("You: " + content);
        propagateMessage(message);
    }
    
    private void propagateMessage(ChatMessage message) {
        // Remember we've seen this message
        receivedMessageIDs.add(message.hashCode() + "");
        
        // Send to all peers
        for (PeerInfo peer : knownPeers.values()) {
            try {
                Registry registry = LocateRegistry.getRegistry(peer.getHostname(), peer.getPort());
                PeerService remotePeer = (PeerService) registry.lookup("PeerService");
                remotePeer.receiveMessage(message);
            } catch (Exception e) {
                // Ignore failures - bare bones implementation
            }
        }
    }
    
    // PeerService interface implementations
    
    @Override
    public void registerPeer(String remotePeerID, String remoteHostname, int remotePort) 
            throws RemoteException {
        if (!remotePeerID.equals(peerID)) {
            PeerInfo peerInfo = new PeerInfo(remotePeerID, remoteHostname, remotePort);
            knownPeers.put(remotePeerID, peerInfo);
            System.out.println("Peer registered: " + remotePeerID);
        }
    }
    
    @Override
    public void receiveMessage(ChatMessage message) throws RemoteException {
        // Skip if we've seen this message before
        String messageHash = message.hashCode() + "";
        if (receivedMessageIDs.contains(messageHash) || message.getSenderID().equals(peerID)) {
            return;
        }
        
        // Display and propagate
        System.out.println(message.getSenderID() + ": " + message.getContent());
        message.markSeenBy(peerID);
        receivedMessageIDs.add(messageHash);
        propagateMessage(message);
    }
    
    @Override
    public List<PeerInfo> getKnownPeers() throws RemoteException {
        return new ArrayList<>(knownPeers.values());
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Username: ");
        String username = scanner.nextLine();
        
        System.out.print("Port: ");
        int port = Integer.parseInt(scanner.nextLine());
        
        ChatPeer peer = new ChatPeer(username, port);
        
        System.out.println("1. Start new network");
        System.out.println("2. Join existing network");
        System.out.print("Choice: ");
        int choice = Integer.parseInt(scanner.nextLine());
        
        if (choice == 2) {
            System.out.print("Remote hostname: ");
            String remoteHost = scanner.nextLine();
            
            System.out.print("Remote port: ");
            int remotePort = Integer.parseInt(scanner.nextLine());
            
            peer.connectToPeer(remoteHost, remotePort);
        }
        
        System.out.println("Start chatting (type 'exit' to quit):");
        
        String message;
        while (true) {
            message = scanner.nextLine();
            if (message.equalsIgnoreCase("exit")) {
                break;
            }
            peer.sendMessage(message);
        }
        
        System.exit(0);
    }
}