class Comment : CommentableEntity
{
    public Comment(User author, string content) : base(author, content) { }

    public List<Comment> GetReplies()
    {
        return GetComments();
    }
}





abstract class CommentableEntity
{
    protected readonly string id;
    protected readonly User author;
    protected readonly string content;
    protected readonly DateTime timestamp;
    private readonly HashSet<User> likes = new HashSet<User>();
    protected readonly List<Comment> comments = new List<Comment>();

    public CommentableEntity(User author, string content)
    {
        this.id = Guid.NewGuid().ToString();
        this.author = author;
        this.content = content;
        this.timestamp = DateTime.Now;
    }

    public void AddLike(User user)
    {
        likes.Add(user);
    }

    public void AddComment(Comment comment)
    {
        comments.Add(comment);
    }

    public string GetId() { return id; }
    public User GetAuthor() { return author; }
    public string GetContent() { return content; }
    public DateTime GetTimestamp() { return timestamp; }
    public List<Comment> GetComments() { return comments; }
    public HashSet<User> GetLikes() { return likes; }
}






class Post : CommentableEntity
{
    public Post(User author, string content) : base(author, content) { }
}





class User
{
    private readonly string id;
    private readonly string name;
    private readonly string email;
    private readonly HashSet<User> friends = new HashSet<User>();
    private readonly List<Post> posts = new List<Post>();

    public User(string name, string email)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.email = email;
    }

    public void AddFriend(User friend)
    {
        friends.Add(friend);
    }

    public void AddPost(Post post)
    {
        posts.Add(post);
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
    public HashSet<User> GetFriends() { return friends; }
    public List<Post> GetPosts() { return posts; }
}







interface IPostObserver
{
    void OnPostCreated(Post post);
    void OnLike(Post post, User user);
    void OnComment(Post post, Comment comment);
}





class UserNotifier : IPostObserver
{
    public void OnPostCreated(Post post)
    {
        User author = post.GetAuthor();
        foreach (User friend in author.GetFriends())
        {
            Console.WriteLine($"Notification for {friend.GetName()}: {author.GetName()} created a new post: {post.GetContent()}");
        }
    }

    public void OnLike(Post post, User user)
    {
        User author = post.GetAuthor();
        Console.WriteLine($"Notification for {author.GetName()}: {user.GetName()} liked your post");
    }

    public void OnComment(Post post, Comment comment)
    {
        User author = post.GetAuthor();
        Console.WriteLine($"Notification for {author.GetName()}: {comment.GetAuthor().GetName()} commented on your post");
    }
}







class PostRepository
{
    private static readonly PostRepository INSTANCE = new PostRepository();
    private readonly Dictionary<string, Post> posts = new Dictionary<string, Post>();

    private PostRepository() { }

    public static PostRepository GetInstance()
    {
        return INSTANCE;
    }

    public void Save(Post post)
    {
        posts[post.GetId()] = post;
    }

    public Post FindById(string id)
    {
        posts.TryGetValue(id, out Post post);
        return post;
    }
}





class UserRepository
{
    private static readonly UserRepository INSTANCE = new UserRepository();
    private readonly Dictionary<string, User> users = new Dictionary<string, User>();

    private UserRepository() { }

    public static UserRepository GetInstance()
    {
        return INSTANCE;
    }

    public void Save(User user)
    {
        users[user.GetId()] = user;
    }

    public User FindById(string id)
    {
        users.TryGetValue(id, out User user);
        return user;
    }
}






class NewsFeedService
{
    private INewsFeedGenerationStrategy strategy;

    public NewsFeedService()
    {
        this.strategy = new ChronologicalStrategy(); // Default strategy
    }

    public void SetStrategy(INewsFeedGenerationStrategy strategy)
    {
        this.strategy = strategy;
    }

    public List<Post> GetNewsFeed(User user)
    {
        return strategy.GenerateFeed(user);
    }
}



class PostService
{
    private readonly PostRepository postRepository = PostRepository.GetInstance();
    private readonly List<IPostObserver> observers = new List<IPostObserver>();

    public void AddObserver(IPostObserver observer)
    {
        observers.Add(observer);
    }

    public Post CreatePost(User author, string content)
    {
        Post post = new Post(author, content);
        postRepository.Save(post);
        author.AddPost(post);
        foreach (var observer in observers)
        {
            observer.OnPostCreated(post);
        }
        return post;
    }

    public void LikePost(User user, string postId)
    {
        Post post = postRepository.FindById(postId);
        post.AddLike(user);
        foreach (var observer in observers)
        {
            observer.OnLike(post, user);
        }
    }

    public void AddComment(User author, string commentableId, string content)
    {
        Comment comment = new Comment(author, content);
        Post post = postRepository.FindById(commentableId);
        post.AddComment(comment);
        foreach (var observer in observers)
        {
            observer.OnComment(post, comment);
        }
    }
}






class UserService
{
    private readonly UserRepository userRepository = UserRepository.GetInstance();

    public User CreateUser(string name, string email)
    {
        User user = new User(name, email);
        userRepository.Save(user);
        return user;
    }

    public void AddFriend(string userId1, string userId2)
    {
        User user1 = userRepository.FindById(userId1);
        User user2 = userRepository.FindById(userId2);

        user1.AddFriend(user2);
        user2.AddFriend(user1);
    }

    public User GetUserById(string userId)
    {
        return userRepository.FindById(userId);
    }
}







class ChronologicalStrategy : INewsFeedGenerationStrategy
{
    public List<Post> GenerateFeed(User user)
    {
        HashSet<User> friends = user.GetFriends();
        List<Post> feed = new List<Post>();

        foreach (User friend in friends)
        {
            feed.AddRange(friend.GetPosts());
        }

        // Sort posts by timestamp in reverse (most recent first)
        feed.Sort((p1, p2) => p2.GetTimestamp().CompareTo(p1.GetTimestamp()));

        return feed;
    }
}




interface INewsFeedGenerationStrategy
{
    List<Post> GenerateFeed(User user);
}




using System;
using System.Collections.Generic;
using System.Linq;

public class SocialNetworkDemo
{
    private static void PrintFeed(List<Post> feed)
    {
        if (feed.Count == 0)
        {
            Console.WriteLine("  No posts in the feed.");
            return;
        }

        foreach (Post post in feed)
        {
            Console.WriteLine($"  Post by {post.GetAuthor().GetName()} at {post.GetTimestamp()}");
            Console.WriteLine($"    \"{post.GetContent()}\"");
            Console.WriteLine($"    Likes: {post.GetLikes().Count}, Comments: {post.GetComments().Count}");
        }
    }

    public static void Main(string[] args)
    {
        SocialNetworkFacade socialNetwork = new SocialNetworkFacade();

        Console.WriteLine("----------- 1. Creating Users -----------");
        User alice = socialNetwork.CreateUser("Alice", "alice@example.com");
        User bob = socialNetwork.CreateUser("Bob", "bob@example.com");
        User charlie = socialNetwork.CreateUser("Charlie", "charlie@example.com");
        Console.WriteLine($"Created users: {alice.GetName()}, {bob.GetName()}, {charlie.GetName()}");

        Console.WriteLine("\n----------- 2. Building Friendships -----------");
        socialNetwork.AddFriend(alice.GetId(), bob.GetId());
        socialNetwork.AddFriend(bob.GetId(), charlie.GetId());
        Console.WriteLine($"{alice.GetName()} and {bob.GetName()} are now friends.");
        Console.WriteLine($"{bob.GetName()} and {charlie.GetName()} are now friends.");

        Console.WriteLine("\n----------- 3. Users Create Posts -----------");
        Post alicePost = socialNetwork.CreatePost(alice.GetId(), "Hello from Alice!");
        Post bobPost = socialNetwork.CreatePost(bob.GetId(), "It's a beautiful day!");
        Post charliePost = socialNetwork.CreatePost(charlie.GetId(), "Thinking about design patterns.");

        Console.WriteLine("\n----------- 4. Users Interact with Posts -----------");
        socialNetwork.AddComment(bob.GetId(), alicePost.GetId(), "Hey Alice, nice to see you here!");
        socialNetwork.LikePost(charlie.GetId(), alicePost.GetId());

        Console.WriteLine("\n----------- 5. Viewing News Feeds (Strategy Pattern) -----------");

        Console.WriteLine("\n--- Alice's News Feed (should see Bob's post) ---");
        List<Post> alicesFeed = socialNetwork.GetNewsFeed(alice.GetId());
        PrintFeed(alicesFeed);

        Console.WriteLine("\n--- Bob's News Feed (should see Alice's, and Charlie's post) ---");
        List<Post> bobsFeed = socialNetwork.GetNewsFeed(bob.GetId());
        PrintFeed(bobsFeed);

        Console.WriteLine("\n--- Charlie's News Feed (should see Bob's post) ---");
        List<Post> charliesFeed = socialNetwork.GetNewsFeed(charlie.GetId());
        PrintFeed(charliesFeed);
    }
}






class SocialNetworkFacade
{
    private readonly UserService userService;
    private readonly PostService postService;
    private readonly NewsFeedService newsFeedService;

    public SocialNetworkFacade()
    {
        this.userService = new UserService();
        this.postService = new PostService();
        this.newsFeedService = new NewsFeedService();
        // Wire up the observer
        postService.AddObserver(new UserNotifier());
    }

    public User CreateUser(string name, string email)
    {
        return userService.CreateUser(name, email);
    }

    public void AddFriend(string userId1, string userId2)
    {
        userService.AddFriend(userId1, userId2);
    }

    public Post CreatePost(string authorId, string content)
    {
        User author = userService.GetUserById(authorId);
        return postService.CreatePost(author, content);
    }

    public void AddComment(string userId, string postId, string content)
    {
        User user = userService.GetUserById(userId);
        postService.AddComment(user, postId, content);
    }

    public void LikePost(string userId, string postId)
    {
        User user = userService.GetUserById(userId);
        postService.LikePost(user, postId);
    }

    public List<Post> GetNewsFeed(string userId)
    {
        User user = userService.GetUserById(userId);
        return newsFeedService.GetNewsFeed(user);
    }
}










































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































