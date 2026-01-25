package medium.chatservice;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

// ======================= USER =======================
class User {
    private final String id;
    private final String name;

    public User(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void onMessageReceived(Message message, Chat chatContext) {
        System.out.printf(
                "[Notification for %s | Chat: %s] %s: %s%n",
                name,
                chatContext.getName(this),
                message.getSender().getName(),
                message.getContent()
        );
    }
}

// ======================= MESSAGE =======================
class Message {
    private final String id;
    private final User sender;
    private final String content;
    private final LocalDateTime timestamp;

    public Message(String content, User sender) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public User getSender() { return sender; }
    public String getContent() { return content; }
}

// ======================= CHAT (ABSTRACT) =======================
abstract class Chat {
    protected final String id;
    protected final List<User> members;
    protected final List<Message> messages;

    protected Chat() {
        this.id = UUID.randomUUID().toString();
        this.members = new CopyOnWriteArrayList<>();
        this.messages = new CopyOnWriteArrayList<>();
    }

    public String getChatId() { return id; }

    public List<User> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void sendMessage(Message message) {
        messages.add(message);
        for (User member : members) {
            if (!member.equals(message.getSender())) {
                member.onMessageReceived(message, this);
            }
        }
    }

    public abstract String getName(User perspectiveUser);
}

// ======================= ONE TO ONE CHAT =======================
class OneToOneChat extends Chat {

    public OneToOneChat(User user1, User user2) {
        super();
        members.add(user1);
        members.add(user2);
    }

    @Override
    public String getName(User self) {
        for (User member : members) {
            if (!member.equals(self)) {
                return member.getName();
            }
        }
        return "Private Chat";
    }
}

// ======================= GROUP CHAT =======================
class GroupChat extends Chat {
    private final String groupName;

    public GroupChat(String groupName, List<User> initialMembers) {
        super();
        this.groupName = groupName;
        members.addAll(initialMembers);
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
    public String getName(User user) {
        return groupName;
    }
}

// ======================= CHAT SERVICE =======================
class ChatService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Chat> chats = new ConcurrentHashMap<>();

    public User createUser(String name) {
        User user = new User(name);
        users.put(user.getId(), user);
        return user;
    }

    public Chat createOneToOneChat(String user1Id, String user2Id) {
        User user1 = users.get(user1Id);
        User user2 = users.get(user2Id);

        Chat chat = new OneToOneChat(user1, user2);
        chats.put(chat.getChatId(), chat);
        return chat;
    }

    public Chat createGroupChat(String groupName, List<String> memberIds) {
        List<User> members = new ArrayList<>();
        for (String id : memberIds) {
            members.add(users.get(id));
        }

        Chat chat = new GroupChat(groupName, members);
        chats.put(chat.getChatId(), chat);
        return chat;
    }

    public void sendMessage(String senderId, String chatId, String content) {
        User sender = users.get(senderId);
        Chat chat = chats.get(chatId);

        if (chat == null || sender == null) {
            System.out.println("Invalid sender or chat");
            return;
        }

        if (!chat.getMembers().contains(sender)) {
            System.out.println("Sender is not a member of this chat");
            return;
        }

        Message message = new Message(content, sender);
        chat.sendMessage(message);
    }
}

// ======================= CLIENT (MAIN) =======================
class ChatApplication {
    public static void main(String[] args) {

        ChatService chatService = new ChatService();

        // Create users
        User alice = chatService.createUser("Alice");
        User bob = chatService.createUser("Bob");
        User charlie = chatService.createUser("Charlie");

        // One-to-one chat
        Chat privateChat = chatService.createOneToOneChat(alice.getId(), bob.getId());

        chatService.sendMessage(alice.getId(), privateChat.getChatId(), "Hey Bob!");
        chatService.sendMessage(bob.getId(), privateChat.getChatId(), "Hi Alice 👋");

        System.out.println();

        // Group chat
        Chat groupChat = chatService.createGroupChat(
                "Dev Group",
                List.of(alice.getId(), bob.getId(), charlie.getId())
        );

        chatService.sendMessage(charlie.getId(), groupChat.getChatId(), "Hello everyone!");
        chatService.sendMessage(alice.getId(), groupChat.getChatId(), "Welcome Charlie 🎉");
    }
}
