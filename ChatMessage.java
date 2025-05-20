import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String messageID;
    private final String senderID;
    private final String content;
    private final Set<String> seenBy;
    
    public ChatMessage(String senderID, String content) {
        this.messageID = UUID.randomUUID().toString();
        this.senderID = senderID;
        this.content = content;
        this.seenBy = new HashSet<>();
        this.seenBy.add(senderID);
    }
    
    // Getters
    public String getSenderID() { return senderID; }
    public String getContent() { return content; }
    
    // Mark as seen
    public void markSeenBy(String peerID) {
        seenBy.add(peerID);
    }
    
    // Check if seen
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