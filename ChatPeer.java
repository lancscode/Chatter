import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatPeer implements PeerService {
    private final String peerID;
    private final String hostname;
    private final int port;
    private final Map<String, PeerInfo> knownPeers = new ConcurrentHashMap<>();
    private final Set<ChatMessage> receivedMessages = ConcurrentHashMap.newKeySet();
    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(5);
    
    private Registry registry;
    private PeerService exportedService;
    
    public ChatPeer(String username, int port) {
        this.peerID = username;
        this.port = port;
        
        try {
            this.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            throw new RuntimeException("Could not determine hostname", e);
        }
        
        startPeerService();
    }
    
    private void startPeerService() {
        try {
            // Create and start registry on specified port
            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                // Registry may already exist
                registry = LocateRegistry.getRegistry(port);
            }
            
            // Export this peer as a remote object
            exportedService = (PeerService) UnicastRemoteObject.exportObject(this, 0);
            
            // Bind to registry
            registry.rebind("PeerService", exportedService);
            
            System.out.println("Peer " + peerID + " started on " + hostname + ":" + port);
        } catch (Exception e) {
            System.err.println("Peer startup failed: " + e);
            e.printStackTrace();
        }
    }
    
    // Connect to another peer
    public void connectToPeer(String hostname, int port) {
        try {
            Registry remoteRegistry = LocateRegistry.getRegistry(hostname, port);
            PeerService remotePeer = (PeerService) remoteRegistry.lookup("PeerService");
            
            // Register with the remote peer
            remotePeer.registerPeer(peerID, this.hostname, this.port);
            
            // Get peers known to the remote peer
            List<PeerInfo> remotePeers = remotePeer.getKnownPeers();
            
            // Add remote peers to our known peers
            for (PeerInfo peerInfo : remotePeers) {
                if (!peerInfo.getPeerID().equals(peerID) && 
                    !knownPeers.containsKey(peerInfo.getPeerID())) {
                    knownPeers.put(peerInfo.getPeerID(), peerInfo);
                }
            }
            
            System.out.println("Connected to peer network via " + hostname + ":" + port);
        } catch (Exception e) {
            System.err.println("Connection failed: " + e);
        }
    }
    
    // Send a message to the network
    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            System.out.println("Cannot send empty message");
            return;
        }
        
        ChatMessage message = new ChatMessage(peerID, content);
        receivedMessages.add(message);
        
        propagateMessage(message);
        displayMessage(message);
    }
    
    // Propagate a message to all connected peers
    private void propagateMessage(ChatMessage message) {
        for (PeerInfo peerInfo : knownPeers.values()) {
            if (peerInfo.isOnline() && !message.isSeenBy(peerInfo.getPeerID())) {
                final PeerInfo finalPeerInfo = peerInfo;
                messageExecutor.submit(() -> {
                    try {
                        Registry peerRegistry = LocateRegistry.getRegistry(
                            finalPeerInfo.getHostname(), finalPeerInfo.getPort());
                        PeerService peer = (PeerService) peerRegistry.lookup("PeerService");
                        peer.receiveMessage(message);
                    } catch (Exception e) {
                        // Peer appears to be offline
                        finalPeerInfo.setOnline(false);
                        System.out.println("Peer seems offline: " + finalPeerInfo.getPeerID());
                    }
                });
            }
        }
    }
    
    // Display a message locally
    private void displayMessage(ChatMessage message) {
        System.out.println(message.getSenderID() + ": " + message.getContent());
    }
    
    // PeerService implementation methods
    
    @Override
    public void registerPeer(String remotePeerID, String remoteHostname, int remotePort) 
            throws RemoteException {
        PeerInfo peerInfo = new PeerInfo(remotePeerID, remoteHostname, remotePort);
        
        if (!knownPeers.containsKey(remotePeerID) && !remotePeerID.equals(peerID)) {
            knownPeers.put(remotePeerID, peerInfo);
            System.out.println("New peer registered: " + remotePeerID);
        }
    }
    
    @Override
    public void receiveMessage(ChatMessage message) throws RemoteException {
        // If this is a new message, process and propagate it
        if (!receivedMessages.contains(message)) {
            // Add this peer to message's seen list
            message.markSeenBy(peerID);
            receivedMessages.add(message);
            
            displayMessage(message);
            propagateMessage(message);
        }
    }
    
    @Override
    public List<PeerInfo> getKnownPeers() throws RemoteException {
        return new ArrayList<>(knownPeers.values());
    }
    
    // Simple shutdown method
    public void shutdown() {
        try {
            messageExecutor.shutdown();
            if (registry != null && exportedService != null) {
                UnicastRemoteObject.unexportObject(this, true);
                registry.unbind("PeerService");
                System.out.println("Peer shutdown complete");
            }
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
    
    // Main method to start a peer
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        
        System.out.print("Enter port number for this peer: ");
        int port = Integer.parseInt(scanner.nextLine());
        
        ChatPeer peer = new ChatPeer(username, port);
        
        // Connection menu
        System.out.println("1. Start a new peer network");
        System.out.println("2. Connect to existing peer");
        System.out.print("Choice: ");
        int choice = Integer.parseInt(scanner.nextLine());
        
        if (choice == 2) {
            System.out.print("Enter hostname to connect to: ");
            String hostname = scanner.nextLine();
            
            System.out.print("Enter port to connect to: ");
            int remotePort = Integer.parseInt(scanner.nextLine());
            
            peer.connectToPeer(hostname, remotePort);
        }
        
        // Message loop
        System.out.println("Start chatting (type 'exit' to quit):");
        String message;
        while (true) {
            message = scanner.nextLine();
            if (message.equalsIgnoreCase("exit")) {
                peer.shutdown();
                break;
            } else {
                peer.sendMessage(message);
            }
        }
        
        System.exit(0);
    }
}