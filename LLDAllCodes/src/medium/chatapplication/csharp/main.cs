

class Message
{
    private readonly string id;
    private readonly User sender;
    private readonly string content;
    private readonly DateTime timestamp;

    public Message(User sender, string content)
    {
        this.id = Guid.NewGuid().ToString();
        this.sender = sender;
        this.content = content;
        this.timestamp = DateTime.Now;
    }

    public string GetId()
    {
        return id;
    }

    public User GetSender()
    {
        return sender;
    }

    public string GetContent()
    {
        return content;
    }

    public DateTime GetTimestamp()
    {
        return timestamp;
    }

    public override string ToString()
    {
        return $"[{timestamp}] {sender.GetName()}: {content}";
    }
}











class User
{
    private readonly string id;
    private readonly string name;

    public User(string name)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
    }

    public string GetId()
    {
        return id;
    }

    public string GetName()
    {
        return name;
    }

    public void OnMessageReceived(Message message, Chat chatContext)
    {
        Console.WriteLine($"[Notification for {GetName()} in chat '{chatContext.GetName(this)}'] {message.GetSender().GetName()}: {message.GetContent()}");
    }

    public override bool Equals(object obj)
    {
        if (this == obj) return true;
        if (obj == null || GetType() != obj.GetType()) return false;
        User user = (User)obj;
        return id.Equals(user.id);
    }

    public override int GetHashCode()
    {
        return id.GetHashCode();
    }

    public override string ToString()
    {
        return $"User{{id='{id}', name='{name}'}}";
    }
}













abstract class Chat
{
    protected readonly string id;
    protected readonly List<User> members;
    protected readonly List<Message> messages;

    public Chat()
    {
        this.id = Guid.NewGuid().ToString();
        this.members = new List<User>();
        this.messages = new List<Message>();
    }

    public string GetId()
    {
        return id;
    }

    public List<User> GetMembers()
    {
        return new List<User>(members);
    }

    public List<Message> GetMessages()
    {
        return new List<Message>(messages);
    }

    public void AddMessage(Message message)
    {
        messages.Add(message);
    }

    public abstract string GetName(User perspectiveUser);
}








using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Linq;

public class ChatApplicationDemo
{
    public static void Main(string[] args)
    {
        // 1. Initialize the Mediator (ChatService)
        ChatService chatService = new ChatService();

        // 2. Create and register users
        User alice = chatService.CreateUser("Alice");
        User bob = chatService.CreateUser("Bob");
        User charlie = chatService.CreateUser("Charlie");

        Console.WriteLine("--- Users registered in the system ---");
        Console.WriteLine();

        // 3. Scenario 1: One-on-one chat between Alice and Bob
        Console.WriteLine("--- Starting one-on-one chat between Alice and Bob ---");
        Chat aliceBobChat = chatService.CreateOneToOneChat(alice.GetId(), bob.GetId());

        // Alice sends a message to Bob
        Console.WriteLine("Alice sends a message...");
        chatService.SendMessage(alice.GetId(), aliceBobChat.GetId(), "Hi Bob, how are you?");

        // Bob sends a reply
        Console.WriteLine("\nBob sends a reply...");
        chatService.SendMessage(bob.GetId(), aliceBobChat.GetId(), "I'm good, Alice! Thanks for asking.");
        Console.WriteLine();

        // 4. Scenario 2: Group chat
        Console.WriteLine("--- Starting a group chat for a 'Project Team' ---");
        List<string> projectMembers = new List<string> { alice.GetId(), bob.GetId(), charlie.GetId() };
        Chat projectGroup = chatService.CreateGroupChat("Project Team", projectMembers);

        // Charlie sends a message to the group
        Console.WriteLine("Charlie sends a message to the group...");
        chatService.SendMessage(charlie.GetId(), projectGroup.GetId(), "Hey team, when is our deadline?");

        // Alice replies to the group
        Console.WriteLine("\nAlice replies to the group...");
        chatService.SendMessage(alice.GetId(), projectGroup.GetId(), "It's next Friday. Let's sync up tomorrow.");
        Console.WriteLine();

        // 5. Demonstrate fetching chat history
        Console.WriteLine("--- Fetching Chat Histories ---");

        // History of Alice and Bob's chat
        Console.WriteLine($"\nHistory for chat '{aliceBobChat.GetName(alice)}':");
        List<Message> oneToOneHistory = chatService.PrintChatHistory(aliceBobChat.GetId());
        foreach (Message message in oneToOneHistory)
        {
            Console.WriteLine(message.ToString());
        }

        // History of the project group chat
        Console.WriteLine($"\nHistory for chat '{projectGroup.GetName(charlie)}':");
        List<Message> groupHistory = chatService.PrintChatHistory(projectGroup.GetId());
        foreach (Message message in groupHistory)
        {
            Console.WriteLine(message.ToString());
        }

        // 6. Demonstrate finding all of a user's chats
        Console.WriteLine("\n--- Fetching all of Alice's chats ---");
        List<Chat> aliceChats = chatService.GetUserChats(alice.GetId());
        foreach (Chat chat in aliceChats)
        {
            Console.WriteLine($"Chat: {chat.GetName(alice)} (ID: {chat.GetId()})");
        }
    }
}








class ChatService
{
    private readonly ConcurrentDictionary<string, User> users = new ConcurrentDictionary<string, User>();
    private readonly ConcurrentDictionary<string, Chat> chats = new ConcurrentDictionary<string, Chat>();

    public User CreateUser(string name)
    {
        User user = new User(name);
        users.TryAdd(user.GetId(), user);
        return user;
    }

    public Chat CreateOneToOneChat(string userId1, string userId2)
    {
        User user1 = users[userId1];
        User user2 = users[userId2];
        Chat chat = new OneToOneChat(user1, user2);
        chats.TryAdd(chat.GetId(), chat);
        return chat;
    }

    public Chat CreateGroupChat(string name, List<string> memberIds)
    {
        List<User> members = new List<User>();
        foreach (string memberId in memberIds)
        {
            members.Add(users[memberId]);
        }
        Chat chat = new GroupChat(name, members);
        chats.TryAdd(chat.GetId(), chat);
        return chat;
    }

    public void SendMessage(string senderId, string chatId, string messageContent)
    {
        User sender = users[senderId];
        
        if (!chats.TryGetValue(chatId, out Chat chat))
        {
            Console.WriteLine($"Error: Chat not found with ID: {chatId}");
            return;
        }

        if (!chat.GetMembers().Contains(sender))
        {
            Console.WriteLine($"Error: Sender {sender.GetName()} is not a member of this chat.");
            return;
        }

        Message message = new Message(sender, messageContent);
        chat.AddMessage(message);

        // Notify all members of the chat (Observer pattern)
        foreach (User member in chat.GetMembers())
        {
            // Do not send a notification to the sender
            if (!member.Equals(sender))
            {
                member.OnMessageReceived(message, chat);
            }
        }
    }

    public List<Message> PrintChatHistory(string chatId)
    {
        if (chats.TryGetValue(chatId, out Chat chat))
        {
            return chat.GetMessages();
        }
        return new List<Message>();
    }

    public List<Chat> GetUserChats(string userId)
    {
        return chats.Values
            .Where(chat => chat.GetMembers().Contains(users[userId]))
            .ToList();
    }
}








class GroupChat : Chat
{
    private string groupName;

    public GroupChat(string groupName, List<User> initialMembers)
    {
        this.groupName = groupName;
        members.AddRange(initialMembers);
    }

    public void AddMember(User user)
    {
        if (!members.Contains(user))
        {
            members.Add(user);
        }
    }

    public void RemoveMember(User user)
    {
        members.Remove(user);
    }

    public override string GetName(User perspectiveUser)
    {
        return groupName;
    }
}






class OneToOneChat : Chat
{
    public OneToOneChat(User user1, User user2)
    {
        members.AddRange(new[] { user1, user2 });
    }

    public override string GetName(User perspectiveUser)
    {
        return members
            .Where(member => !member.Equals(perspectiveUser))
            .Select(member => member.GetName())
            .FirstOrDefault() ?? "Unknown Chat";
    }
}






































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































