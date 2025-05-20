import java.io.Serializable;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String messageID;
    private final String senderID;
    private final String content;
    private final long timestamp;
    private final Set<String> seenBy;
    
    public ChatMessage(String senderID, String content) {
        this.messageID = UUID.randomUUID().toString();
        this.senderID = senderID;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.seenBy = ConcurrentHashMap.newKeySet();
        this.seenBy.add(senderID); // Sender has seen it
    }
    
    // Getters
    public String getMessageID() { return messageID; }
    public String getSenderID() { return senderID; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    
    // Mark as seen by a peer
    public void markSeenBy(String peerID) {
        seenBy.add(peerID);
    }
    
    // Check if a peer has seen this message
    public boolean isSeenBy(String peerID) {
        return seenBy.contains(peerID);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return messageID.equals(that.messageID);
    }
    
    @Override
    public int hashCode() {
        return messageID.hashCode();
    }
}