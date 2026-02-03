package easy.snakeandladder.java;

final class Message {
    private final String id;
    private final User sender;
    private final String content;
    private final LocalDateTime timestamp;

    public Message(User sender, String content) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", timestamp, sender.getName(), content);
    }
}












class User {
    private final String id;
    private final String name;

    public User(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void onMessageReceived(Message message, Chat chatContext) {
        System.out.printf("[Notification for %s in chat '%s'] %s: %s\n",
                this.getName(), chatContext.getName(this), message.getSender().getName(), message.getContent());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" + "id='" + id + '\'' + ", name='" + name + '\'' + '}';
    }
}










abstract class Chat {
    protected final String id;
    protected final List<User> members;
    protected final List<Message> messages;

    public Chat() {
        this.id = UUID.randomUUID().toString();
        this.members = new CopyOnWriteArrayList<>(); // Thread-safe for reads
        this.messages = new CopyOnWriteArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<User> getMembers() {
        return List.copyOf(members); // Return an immutable view
    }

    public List<Message> getMessages() {
        return List.copyOf(messages); // Return an immutable view
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public abstract String getName(User perspectiveUser);
}













import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

public class ChatApplicationDemo {
    public static void main(String[] args) {
        // 1. Initialize the Mediator (ChatService)
        ChatService chatService = new ChatService();

        // 2. Create and register users
        User alice = chatService.createUser("Alice");
        User bob = chatService.createUser("Bob");
        User charlie = chatService.createUser("Charlie");

        System.out.println("--- Users registered in the system ---");
        System.out.println();

        // 3. Scenario 1: One-on-one chat between Alice and Bob
        System.out.println("--- Starting one-on-one chat between Alice and Bob ---");
        Chat aliceBobChat = chatService.createOneToOneChat(alice.getId(), bob.getId());

        // Alice sends a message to Bob
        System.out.println("Alice sends a message...");
        chatService.sendMessage(alice.getId(), aliceBobChat.getId(), "Hi Bob, how are you?");

        // Bob sends a reply
        System.out.println("\nBob sends a reply...");
        chatService.sendMessage(bob.getId(), aliceBobChat.getId(), "I'm good, Alice! Thanks for asking.");
        System.out.println();

        // 4. Scenario 2: Group chat
        System.out.println("--- Starting a group chat for a 'Project Team' ---");
        List<String> projectMembers = List.of(alice.getId(), bob.getId(), charlie.getId());
        Chat projectGroup = chatService.createGroupChat("Project Team", projectMembers);

        // Charlie sends a message to the group
        System.out.println("Charlie sends a message to the group...");
        chatService.sendMessage(charlie.getId(), projectGroup.getId(), "Hey team, when is our deadline?");

        // Alice replies to the group
        System.out.println("\nAlice replies to the group...");
        chatService.sendMessage(alice.getId(), projectGroup.getId(), "It's next Friday. Let's sync up tomorrow.");
        System.out.println();

        // 5. Demonstrate fetching chat history
        System.out.println("--- Fetching Chat Histories ---");

        // History of Alice and Bob's chat
        System.out.println("\nHistory for chat '" + aliceBobChat.getName(alice) + "':");
        List<Message> oneToOneHistory = chatService.printChatHistory(aliceBobChat.getId());
        oneToOneHistory.forEach(System.out::println);

        // History of the project group chat
        System.out.println("\nHistory for chat '" + projectGroup.getName(charlie) + "':");
        List<Message> groupHistory = chatService.printChatHistory(projectGroup.getId());
        groupHistory.forEach(System.out::println);

        // 6. Demonstrate finding all of a user's chats
        System.out.println("\n--- Fetching all of Alice's chats ---");
        List<Chat> aliceChats = chatService.getUserChats(alice.getId());
        for(Chat chat : aliceChats) {
            System.out.println("Chat: " + chat.getName(alice) + " (ID: " + chat.getId() + ")");
        }
    }
}















class ChatService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Chat> chats = new ConcurrentHashMap<>();

    public User createUser(String name) {
        User user = new User(name);
        users.put(user.getId(), user);
        return user;
    }

    public Chat createOneToOneChat(String userId1, String userId2) {
        User user1 = users.get(userId1);
        User user2 = users.get(userId2);
        Chat chat = new OneToOneChat(user1, user2);
        chats.put(chat.getId(), chat);
        return chat;
    }

    public Chat createGroupChat(String name, List<String> memberIds) {
        List<User> members = new ArrayList<>();
        for (String memberId: memberIds) {
            members.add(users.get(memberId));
        }
        Chat chat = new GroupChat(name, members);
        chats.put(chat.getId(), chat);
        return chat;
    }

    public void sendMessage(String senderId, String chatId, String messageContent) {
        User sender = users.get(senderId);
        Chat chat = chats.get(chatId);
        if (chat == null) {
            System.err.println("Error: Chat not found with ID: " + chatId);
            return;
        }

        if (!chat.getMembers().contains(sender)) {
            System.err.println("Error: Sender " + sender.getName() + " is not a member of this chat.");
            return;
        }

        Message message = new Message(sender, messageContent);
        chat.addMessage(message);

        // Notify all members of the chat (Observer pattern)
        for (User member : chat.getMembers()) {
            // Do not send a notification to the sender
            if (!member.equals(sender)) {
                member.onMessageReceived(message, chat);
            }
        }
    }

    public List<Message> printChatHistory(String chatId) {
        Chat chat = chats.get(chatId);
        if (chat != null) {
            return chat.getMessages();
        }
        return new ArrayList<>();
    }

    public List<Chat> getUserChats(String userId) {
        return chats.values().stream()
                .filter(chat -> chat.getMembers().contains(users.get(userId)))
                .collect(Collectors.toList());
    }
}













class GroupChat extends Chat {
    private String groupName;

    public GroupChat(String groupName, List<User> initialMembers) {
        super();
        this.groupName = groupName;
        this.members.addAll(initialMembers);
    }

    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
        }
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    @Override
    public String getName(User perspectiveUser) {
        return groupName;
    }
}














class OneToOneChat extends Chat {

    public OneToOneChat(User user1, User user2) {
        super();
        this.members.addAll(List.of(user1, user2));
    }

    @Override
    public String getName(User perspectiveUser) {
        // The chat name from a user's perspective is the other user's name.
        return members.stream()
                .filter(member -> !member.equals(perspectiveUser))
                .findFirst()
                .map(User::getName)
                .orElse("Unknown Chat");
    }
}















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































