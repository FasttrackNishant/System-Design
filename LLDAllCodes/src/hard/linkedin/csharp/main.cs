enum ConnectionStatus
{
    PENDING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}




enum NotificationType
{
    CONNECTION_REQUEST,
    POST_LIKE,
    POST_COMMENT
}






class Comment
{
    private readonly Member author;
    private readonly string text;
    private readonly DateTime createdAt;

    public Comment(Member author, string text)
    {
        this.author = author;
        this.text = text;
        this.createdAt = DateTime.Now;
    }

    public Member GetAuthor() { return author; }
    public string GetText() { return text; }
}



class Connection
{
    private readonly Member fromMember;
    private readonly Member toMember;
    private ConnectionStatus status;
    private readonly DateTime requestedAt;
    private DateTime acceptedAt;

    public Connection(Member fromMember, Member toMember)
    {
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.status = ConnectionStatus.PENDING;
        this.requestedAt = DateTime.Now;
    }

    public Member GetFromMember() { return fromMember; }
    public Member GetToMember() { return toMember; }
    public ConnectionStatus GetStatus() { return status; }

    public void SetStatus(ConnectionStatus status)
    {
        this.status = status;
        if (status == ConnectionStatus.ACCEPTED)
        {
            this.acceptedAt = DateTime.Now;
        }
    }
}




class Education
{
    private readonly string school;
    private readonly string degree;
    private readonly int startYear;
    private readonly int endYear;

    public Education(string school, string degree, int startYear, int endYear)
    {
        this.school = school;
        this.degree = degree;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    public override string ToString()
    {
        return $"{degree}, {school} ({startYear} - {endYear})";
    }
}



class Experience
{
    private readonly string title;
    private readonly string company;
    private readonly string startDate;
    private readonly string endDate; // null for current job

    public Experience(string title, string company, string startDate, string endDate)
    {
        this.title = title;
        this.company = company;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public override string ToString()
    {
        string end = string.IsNullOrEmpty(endDate) ? "Present" : endDate;
        return $"{title} at {company} ({startDate} to {end})";
    }
}




class Like
{
    private readonly Member member;
    private readonly DateTime createdAt;

    public Like(Member member)
    {
        this.member = member;
        this.createdAt = DateTime.Now;
    }

    public Member GetMember() { return member; }
}




class Member : INotificationObserver
{
    private readonly string id;
    private readonly string name;
    private readonly string email;
    private readonly Profile profile;
    private readonly HashSet<Member> connections = new HashSet<Member>();
    private readonly List<Notification> notifications = new List<Notification>();

    public Member(string id, string name, string email, Profile profile)
    {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profile = profile;
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
    public string GetEmail() { return email; }
    public HashSet<Member> GetConnections() { return connections; }
    public Profile GetProfile() { return profile; }

    public void AddConnection(Member member)
    {
        connections.Add(member);
    }

    public void DisplayProfile()
    {
        Console.WriteLine($"\n--- Profile for {name} ({email}) ---");
        profile.Display();
        Console.WriteLine($"  Connections: {connections.Count}");
    }

    public void ViewNotifications()
    {
        Console.WriteLine($"\n--- Notifications for {name} ---");
        var unreadNotifications = notifications.Where(n => !n.IsRead()).ToList();

        if (!unreadNotifications.Any())
        {
            Console.WriteLine("  No new notifications.");
            return;
        }

        foreach (var notification in unreadNotifications)
        {
            Console.WriteLine($"  - {notification.GetContent()}");
            notification.MarkAsRead();
        }
    }

    public void Update(Notification notification)
    {
        notifications.Add(notification);
        Console.WriteLine($"Notification pushed to {name}: {notification.GetContent()}");
    }
}

class MemberBuilder
{
    private readonly string id;
    private readonly string name;
    private readonly string email;
    private readonly Profile profile = new Profile();

    public MemberBuilder(string name, string email)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.email = email;
    }

    public MemberBuilder WithSummary(string summary)
    {
        profile.SetSummary(summary);
        return this;
    }

    public MemberBuilder AddExperience(Experience experience)
    {
        profile.AddExperience(experience);
        return this;
    }

    public MemberBuilder AddEducation(Education education)
    {
        profile.AddEducation(education);
        return this;
    }

    public Member Build()
    {
        return new Member(id, name, email, profile);
    }
}






class NewsFeed
{
    private readonly List<Post> posts;

    public NewsFeed(List<Post> posts)
    {
        this.posts = posts;
    }

    public void Display(IFeedSortingStrategy strategy)
    {
        var sortedPosts = strategy.Sort(posts);
        if (!sortedPosts.Any())
        {
            Console.WriteLine("  Your news feed is empty.");
            return;
        }

        foreach (var post in sortedPosts)
        {
            Console.WriteLine("----------------------------------------");
            Console.WriteLine($"Post by: {post.GetAuthor().GetName()} (at {post.GetCreatedAt().ToShortDateString()})");
            Console.WriteLine($"Content: {post.GetContent()}");
            Console.WriteLine($"Likes: {post.GetLikes().Count}, Comments: {post.GetComments().Count}");
            Console.WriteLine("----------------------------------------");
        }
    }
}





class Notification
{
    private readonly string id;
    private readonly string memberId;
    private readonly NotificationType type;
    private readonly string content;
    private readonly DateTime createdAt;
    private bool isRead = false;

    public Notification(string memberId, NotificationType type, string content)
    {
        this.id = Guid.NewGuid().ToString();
        this.memberId = memberId;
        this.type = type;
        this.content = content;
        this.createdAt = DateTime.Now;
    }

    public string GetContent() { return content; }
    public void MarkAsRead() { isRead = true; }
    public bool IsRead() { return isRead; }
}






class Post : Subject
{
    private readonly string id;
    private readonly Member author;
    private readonly string content;
    private readonly DateTime createdAt;
    private readonly List<Like> likes = new List<Like>();
    private readonly List<Comment> comments = new List<Comment>();

    public Post(Member author, string content)
    {
        this.id = Guid.NewGuid().ToString();
        this.author = author;
        this.content = content;
        this.createdAt = DateTime.Now;
        AddObserver(author);
    }

    public void AddLike(Member member)
    {
        likes.Add(new Like(member));
        string notificationContent = $"{member.GetName()} liked your post.";
        var notification = new Notification(author.GetId(), NotificationType.POST_LIKE, notificationContent);
        NotifyObservers(notification);
    }

    public void AddComment(Member member, string text)
    {
        comments.Add(new Comment(member, text));
        string notificationContent = $"{member.GetName()} commented on your post: \"{text}\"";
        var notification = new Notification(author.GetId(), NotificationType.POST_COMMENT, notificationContent);
        NotifyObservers(notification);
    }

    public string GetId() { return id; }
    public Member GetAuthor() { return author; }
    public string GetContent() { return content; }
    public DateTime GetCreatedAt() { return createdAt; }
    public List<Like> GetLikes() { return likes; }
    public List<Comment> GetComments() { return comments; }
}





class Profile
{
    private string summary;
    private readonly List<Experience> experiences = new List<Experience>();
    private readonly List<Education> educations = new List<Education>();

    public void SetSummary(string summary) { this.summary = summary; }
    public void AddExperience(Experience experience) { experiences.Add(experience); }
    public void AddEducation(Education education) { educations.Add(education); }

    public void Display()
    {
        Console.WriteLine($"  Summary: {(summary ?? "N/A")}");

        Console.WriteLine("  Experience:");
        if (!experiences.Any())
        {
            Console.WriteLine("    - None");
        }
        else
        {
            foreach (var exp in experiences)
            {
                Console.WriteLine($"    - {exp}");
            }
        }

        Console.WriteLine("  Education:");
        if (!educations.Any())
        {
            Console.WriteLine("    - None");
        }
        else
        {
            foreach (var edu in educations)
            {
                Console.WriteLine($"    - {edu}");
            }
        }
    }
}






interface INotificationObserver
{
    void Update(Notification notification);
}




abstract class Subject
{
    private readonly List<INotificationObserver> observers = new List<INotificationObserver>();

    public void AddObserver(INotificationObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(INotificationObserver observer)
    {
        observers.Remove(observer);
    }

    public void NotifyObservers(Notification notification)
    {
        foreach (var observer in observers)
        {
            observer.Update(notification);
        }
    }
}









class ConnectionService
{
    private readonly NotificationService notificationService;
    private readonly Dictionary<string, Connection> connectionRequests = new Dictionary<string, Connection>();
    private readonly object lockObj = new object();

    public ConnectionService(NotificationService notificationService)
    {
        this.notificationService = notificationService;
    }

    public string SendRequest(Member from, Member to)
    {
        var connection = new Connection(from, to);
        string requestId = Guid.NewGuid().ToString();
        
        lock (lockObj)
        {
            connectionRequests[requestId] = connection;
        }

        Console.WriteLine($"{from.GetName()} sent a connection request to {to.GetName()}.");

        var notification = new Notification(
            to.GetId(),
            NotificationType.CONNECTION_REQUEST,
            $"{from.GetName()} wants to connect with you. Request ID: {requestId}"
        );
        notificationService.SendNotification(to, notification);

        return requestId;
    }

    public void AcceptRequest(string requestId)
    {
        lock (lockObj)
        {
            if (connectionRequests.TryGetValue(requestId, out var request) && 
                request.GetStatus() == ConnectionStatus.PENDING)
            {
                request.SetStatus(ConnectionStatus.ACCEPTED);

                var from = request.GetFromMember();
                var to = request.GetToMember();

                from.AddConnection(to);
                to.AddConnection(from);

                Console.WriteLine($"{to.GetName()} accepted the connection request from {from.GetName()}.");
                connectionRequests.Remove(requestId);
            }
            else
            {
                Console.WriteLine("Invalid or already handled request ID.");
            }
        }
    }
}





class NewsFeedService
{
    private readonly Dictionary<string, List<Post>> allPosts = new Dictionary<string, List<Post>>();
    private readonly object lockObj = new object();

    public void AddPost(Member member, Post post)
    {
        lock (lockObj)
        {
            if (!allPosts.ContainsKey(member.GetId()))
            {
                allPosts[member.GetId()] = new List<Post>();
            }
            allPosts[member.GetId()].Add(post);
        }
    }

    public List<Post> GetMemberPosts(Member member)
    {
        lock (lockObj)
        {
            return allPosts.TryGetValue(member.GetId(), out var posts) ? posts : new List<Post>();
        }
    }

    public void DisplayFeedForMember(Member member, IFeedSortingStrategy feedSortingStrategy)
    {
        var feedPosts = new List<Post>();

        foreach (var connection in member.GetConnections())
        {
            var connectionPosts = GetMemberPosts(connection);
            feedPosts.AddRange(connectionPosts);
        }

        var feed = new NewsFeed(feedPosts);
        feed.Display(feedSortingStrategy);
    }
}







class NotificationService
{
    public void SendNotification(Member member, Notification notification)
    {
        member.Update(notification);
    }
}







class SearchService
{
    private readonly ICollection<Member> members;

    public SearchService(ICollection<Member> members)
    {
        this.members = members;
    }

    public List<Member> SearchByName(string name)
    {
        return members
            .Where(member => member.GetName().ToLower().Contains(name.ToLower()))
            .ToList();
    }
}





interface IFeedSortingStrategy
{
    List<Post> Sort(List<Post> posts);
}



class ChronologicalSortStrategy : IFeedSortingStrategy
{
    public List<Post> Sort(List<Post> posts)
    {
        return posts.OrderByDescending(post => post.GetCreatedAt()).ToList();
    }
}








using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class LinkedInDemo
{
    public static void Main(string[] args)
    {
        var system = LinkedInSystem.GetInstance();

        // 1. Create Members using the Builder Pattern
        Console.WriteLine("--- 1. Member Registration ---");
        var alice = new MemberBuilder("Alice", "alice@example.com")
            .WithSummary("Senior Software Engineer with 10 years of experience.")
            .AddExperience(new Experience("Sr. Software Engineer", "Google", "2018-01-01", null))
            .AddExperience(new Experience("Software Engineer", "Microsoft", "2014-06-01", "2017-12-31"))
            .AddEducation(new Education("Princeton University", "M.S. in Computer Science", 2012, 2014))
            .Build();

        var bob = new MemberBuilder("Bob", "bob@example.com")
            .WithSummary("Product Manager at Stripe.")
            .AddExperience(new Experience("Product Manager", "Stripe", "2020-02-01", null))
            .AddEducation(new Education("MIT", "B.S. in Business Analytics", 2015, 2019))
            .Build();

        var charlie = new MemberBuilder("Charlie", "charlie@example.com").Build();

        system.RegisterMember(alice);
        system.RegisterMember(bob);
        system.RegisterMember(charlie);

        alice.DisplayProfile();

        // 2. Connection Management
        Console.WriteLine("\n--- 2. Connection Management ---");
        string requestId1 = system.SendConnectionRequest(alice, bob);
        string requestId2 = system.SendConnectionRequest(alice, charlie);

        bob.ViewNotifications();

        Console.WriteLine("\nBob accepts Alice's request.");
        system.AcceptConnectionRequest(requestId1);
        Console.WriteLine("Alice and Bob are now connected.");

        // 3. Posting and News Feed
        Console.WriteLine("\n--- 3. Posting & News Feed ---");
        bob.DisplayProfile();
        system.CreatePost(bob.GetId(), "Excited to share we've launched our new feature! #productmanagement");

        system.ViewNewsFeed(alice.GetId());
        system.ViewNewsFeed(charlie.GetId());

        // 4. Interacting with a Post
        Console.WriteLine("\n--- 4. Post Interaction & Notifications ---");
        var bobsPost = system.GetLatestPostByMember(bob.GetId());
        if (bobsPost != null)
        {
            bobsPost.AddLike(alice);
            bobsPost.AddComment(alice, "This looks amazing! Great work!");
        }

        bob.ViewNotifications();

        // 5. Searching for Members
        Console.WriteLine("\n--- 5. Member Search ---");
        var searchResults = system.SearchMemberByName("ali");
        Console.WriteLine("Search results for 'ali':");
        foreach (var member in searchResults)
        {
            Console.WriteLine($" - {member.GetName()}");
        }
    }
}









class LinkedInSystem
{
    private static volatile LinkedInSystem instance;
    private static readonly object syncRoot = new object();

    private readonly Dictionary<string, Member> members = new Dictionary<string, Member>();
    private readonly ConnectionService connectionService;
    private readonly NewsFeedService newsFeedService;
    private readonly SearchService searchService;

    private LinkedInSystem()
    {
        connectionService = new ConnectionService(new NotificationService());
        newsFeedService = new NewsFeedService();
        searchService = new SearchService(members.Values);
    }

    public static LinkedInSystem GetInstance()
    {
        if (instance == null)
        {
            lock (syncRoot)
            {
                if (instance == null)
                {
                    instance = new LinkedInSystem();
                }
            }
        }
        return instance;
    }

    public void RegisterMember(Member member)
    {
        members[member.GetId()] = member;
        Console.WriteLine($"New member registered: {member.GetName()}");
    }

    public Member GetMember(string name)
    {
        return members.Values.FirstOrDefault(m => m.GetName() == name);
    }

    public string SendConnectionRequest(Member from, Member to)
    {
        return connectionService.SendRequest(from, to);
    }

    public void AcceptConnectionRequest(string requestId)
    {
        connectionService.AcceptRequest(requestId);
    }

    public void CreatePost(string memberId, string content)
    {
        var author = members[memberId];
        var post = new Post(author, content);
        newsFeedService.AddPost(author, post);
        Console.WriteLine($"{author.GetName()} created a new post.");
    }

    public Post GetLatestPostByMember(string memberId)
    {
        var memberPosts = newsFeedService.GetMemberPosts(members[memberId]);
        return memberPosts.LastOrDefault();
    }

    public void ViewNewsFeed(string memberId)
    {
        var member = members[memberId];
        Console.WriteLine($"\n--- News Feed for {member.GetName()} ---");
        newsFeedService.DisplayFeedForMember(member, new ChronologicalSortStrategy());
    }

    public List<Member> SearchMemberByName(string name)
    {
        return searchService.SearchByName(name);
    }
}





























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































