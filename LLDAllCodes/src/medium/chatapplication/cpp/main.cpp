class Chat {
protected:
    string id;
    vector<User> members;
    vector<Message> messages;

public:
    Chat() {
        this->id = generateUUID();
    }

    virtual ~Chat() {}

    string getId() const {
        return id;
    }

    vector<User> getMembers() const {
        return members;
    }

    vector<Message> getMessages() const {
        return messages;
    }

    void addMessage(const Message& message) {
        messages.push_back(message);
    }

    virtual string getName(const User& perspectiveUser) const = 0;
};
















class GroupChat : public Chat {
private:
    string groupName;

public:
    GroupChat(const string& groupName, const vector<User>& initialMembers) : groupName(groupName) {
        members = initialMembers;
    }

    void addMember(const User& user) {
        auto it = find(members.begin(), members.end(), user);
        if (it == members.end()) {
            members.push_back(user);
        }
    }

    void removeMember(const User& user) {
        auto it = find(members.begin(), members.end(), user);
        if (it != members.end()) {
            members.erase(it);
        }
    }

    string getName(const User& perspectiveUser) const {
        return groupName;
    }
};

void User::onMessageReceived(const Message& message, const Chat& chatContext) {
    cout << "[NOTIFICATION] " << name << " received a message in '" 
         << chatContext.getName(*this) << "': " << message.getContent() << endl;
}













class Message {
private:
    string id;
    User sender;
    string content;
    time_t timestamp;

public:
    Message(const User& sender, const string& content) : sender(sender), content(content) {
        this->id = generateUUID();
        this->timestamp = time(NULL);
    }

    string getId() const {
        return id;
    }

    User getSender() const {
        return sender;
    }

    string getContent() const {
        return content;
    }

    time_t getTimestamp() const {
        return timestamp;
    }

    string toString() const {
        stringstream ss;
        ss << "[" << put_time(localtime(&timestamp), "%Y-%m-%d %H:%M:%S") << "] " 
           << sender.getName() << ": " << content;
        return ss.str();
    }
};










class OneToOneChat : public Chat {
public:
    OneToOneChat(const User& user1, const User& user2) {
        members.push_back(user1);
        members.push_back(user2);
    }

    string getName(const User& perspectiveUser) const {
        for (const auto& member : members) {
            if (member != perspectiveUser) {
                return member.getName();
            }
        }
        return "Unknown Chat";
    }
};








class User {
private:
    string id;
    string name;

public:
    User() {} // Default constructor for map operations
    
    User(const string& name) : name(name) {
        this->id = generateUUID();
    }

    string getId() const {
        return id;
    }

    string getName() const {
        return name;
    }

    void onMessageReceived(const Message& message, const Chat& chatContext);

    bool operator==(const User& other) const {
        return id == other.id;
    }

    bool operator!=(const User& other) const {
        return !(*this == other);
    }

    string toString() const {
        return "User{id='" + id + "', name='" + name + "'}";
    }
};










class ChatApplicationDemo {
public:
    static void main() {
        // 1. Initialize the Mediator (ChatService)
        ChatService chatService;

        // 2. Create and register users
        User alice = chatService.createUser("Alice");
        User bob = chatService.createUser("Bob");
        User charlie = chatService.createUser("Charlie");

        cout << "--- Users registered in the system ---" << endl;
        cout << endl;

        // 3. Scenario 1: One-on-one chat between Alice and Bob
        cout << "--- Starting one-on-one chat between Alice and Bob ---" << endl;
        Chat* aliceBobChat = chatService.createOneToOneChat(alice.getId(), bob.getId());

        // Alice sends a message to Bob
        cout << "Alice sends a message..." << endl;
        chatService.sendMessage(alice.getId(), aliceBobChat->getId(), "Hi Bob, how are you?");

        // Bob sends a reply
        cout << "\nBob sends a reply..." << endl;
        chatService.sendMessage(bob.getId(), aliceBobChat->getId(), "I'm good, Alice! Thanks for asking.");
        cout << endl;

        // 4. Scenario 2: Group chat
        cout << "--- Starting a group chat for a 'Project Team' ---" << endl;
        vector<string> projectMembers = {alice.getId(), bob.getId(), charlie.getId()};
        Chat* projectGroup = chatService.createGroupChat("Project Team", projectMembers);

        // Charlie sends a message to the group
        cout << "Charlie sends a message to the group..." << endl;
        chatService.sendMessage(charlie.getId(), projectGroup->getId(), "Hey team, when is our deadline?");

        // Alice replies to the group
        cout << "\nAlice replies to the group..." << endl;
        chatService.sendMessage(alice.getId(), projectGroup->getId(), "It's next Friday. Let's sync up tomorrow.");
        cout << endl;

        // 5. Demonstrate fetching chat history
        cout << "--- Fetching Chat Histories ---" << endl;

        // History of Alice and Bob's chat
        cout << "\nHistory for chat '" << aliceBobChat->getName(alice) << "':" << endl;
        vector<Message> oneToOneHistory = chatService.printChatHistory(aliceBobChat->getId());
        for (const Message& message : oneToOneHistory) {
            cout << message.toString() << endl;
        }

        // History of the project group chat
        cout << "\nHistory for chat '" << projectGroup->getName(charlie) << "':" << endl;
        vector<Message> groupHistory = chatService.printChatHistory(projectGroup->getId());
        for (const Message& message : groupHistory) {
            cout << message.toString() << endl;
        }

        // 6. Demonstrate finding all of a user's chats
        cout << "\n--- Fetching all of Alice's chats ---" << endl;
        vector<Chat*> aliceChats = chatService.getUserChats(alice.getId());
        for (const auto& chat : aliceChats) {
            cout << "Chat: " << chat->getName(alice) << " (ID: " << chat->getId() << ")" << endl;
        }
    }
};

int main() {
    ChatApplicationDemo::main();
    return 0;
}







class ChatService {
private:
    map<string, User> users;
    map<string, Chat*> chats;

public:
    ~ChatService() {
        // Clean up dynamically allocated chats
        for (auto& pair : chats) {
            delete pair.second;
        }
    }

    User createUser(const string& name) {
        User user(name);
        users[user.getId()] = user;
        return user;
    }

    Chat* createOneToOneChat(const string& userId1, const string& userId2) {
        User user1 = users[userId1];
        User user2 = users[userId2];
        Chat* chat = new OneToOneChat(user1, user2);
        chats[chat->getId()] = chat;
        return chat;
    }

    Chat* createGroupChat(const string& name, const vector<string>& memberIds) {
        vector<User> members;
        for (const string& memberId : memberIds) {
            members.push_back(users[memberId]);
        }
        Chat* chat = new GroupChat(name, members);
        chats[chat->getId()] = chat;
        return chat;
    }

    void sendMessage(const string& senderId, const string& chatId, const string& messageContent) {
        User sender = users[senderId];
        auto chatIt = chats.find(chatId);
        
        if (chatIt == chats.end()) {
            cerr << "Error: Chat not found with ID: " << chatId << endl;
            return;
        }

        Chat* chat = chatIt->second;
        vector<User> chatMembers = chat->getMembers();
        
        auto memberIt = find(chatMembers.begin(), chatMembers.end(), sender);
        if (memberIt == chatMembers.end()) {
            cerr << "Error: Sender " << sender.getName() << " is not a member of this chat." << endl;
            return;
        }

        Message message(sender, messageContent);
        chat->addMessage(message);

        // Notify all members of the chat (Observer pattern)
        for (const User& member : chatMembers) {
            // Do not send a notification to the sender
            if (member != sender) {
                User memberCopy = member;
                memberCopy.onMessageReceived(message, *chat);
            }
        }
    }

    vector<Message> printChatHistory(const string& chatId) {
        auto chatIt = chats.find(chatId);
        if (chatIt != chats.end()) {
            return chatIt->second->getMessages();
        }
        return vector<Message>();
    }

    vector<Chat*> getUserChats(const string& userId) {
        vector<Chat*> result;
        User user = users[userId];
        
        for (const auto& chatPair : chats) {
            vector<User> members = chatPair.second->getMembers();
            if (find(members.begin(), members.end(), user) != members.end()) {
                result.push_back(chatPair.second);
            }
        }
        return result;
    }
};






























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































