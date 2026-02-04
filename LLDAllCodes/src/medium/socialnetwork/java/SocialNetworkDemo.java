package easy.snakeandladder.java;



class Comment extends CommentableEntity {
    public Comment(User author, String content) {
        super(author, content);
    }

    public List<Comment> getReplies() {
        return getComments();
    }
}



abstract class CommentableEntity {
    protected final String id;
    protected final User author;
    protected final String content;
    protected final LocalDateTime timestamp;
    private final Set<User> likes = new HashSet<>();
    protected final List<Comment> comments = new ArrayList<>();

    public CommentableEntity(User author, String content) {
        this.id = UUID.randomUUID().toString();
        this.author = author;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public void addLike(User user) {
        likes.add(user);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public String getId() { return id; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<Comment> getComments() { return comments; }
    public Set<User> getLikes() {
        return likes;
    }
}





class Post extends CommentableEntity {
    public Post(User author, String content) {
        super(author, content);
    }
}




class User {
    private final String id;
    private final String name;
    private final String email;
    private final Set<User> friends = new HashSet<>();
    private final List<Post> posts = new ArrayList<>();

    public User(String name, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
    }

    public void addFriend(User friend) {
        friends.add(friend);
    }

    public  void addPost(Post post) {
        posts.add(post);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Set<User> getFriends() { return friends; }
    public List<Post> getPosts() { return posts; }
}




interface PostObserver {
    void onPostCreated(Post post);
    void onLike(Post post, User user);
    void onComment(Post post, Comment comment);
}




class UserNotifier implements PostObserver {
    @Override
    public void onPostCreated(Post post) {
        User author = post.getAuthor();
        for (User friend: author.getFriends()) {
            System.out.println("Notification for " + friend.getName() + ": " + author.getName() + " created a new post: " + post.getContent());
        }
    }

    @Override
    public void onLike(Post post, User user) {
        User author = post.getAuthor();
        System.out.println("Notification for " + author.getName() + ": " + user.getName() + " liked your post");
    }

    @Override
    public void onComment(Post post, Comment comment) {
        User author = post.getAuthor();
        System.out.println("Notification for " + author.getName() + ": " + comment.getAuthor().getName() + " commented on your post");
    }
}





class PostRepository {
    private static final PostRepository INSTANCE = new PostRepository();
    private final Map<String, Post> posts = new ConcurrentHashMap<>();

    private PostRepository() {}

    public static PostRepository getInstance() { return INSTANCE; }

    public void save(Post post) {
        posts.put(post.getId(), post);
    }

    public Post findById(String id) {
        return posts.get(id);
    }
}




class UserRepository {
    private static final UserRepository INSTANCE = new UserRepository();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    private UserRepository() {}

    public static UserRepository getInstance() {
        return INSTANCE;
    }

    public void save(User user) {
        users.put(user.getId(), user);
    }

    public User findById(String id) {
        return users.get(id);
    }
}





class NewsFeedService {
    private NewsFeedGenerationStrategy strategy;

    public NewsFeedService() {
        this.strategy = new ChronologicalStrategy(); // Default strategy
    }

    public void setStrategy(NewsFeedGenerationStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Post> getNewsFeed(User user) {
        return strategy.generateFeed(user);
    }
}



class PostService {
    private final PostRepository postRepository = PostRepository.getInstance();
    private final List<PostObserver> observers = new ArrayList<>();

    public void addObserver(PostObserver observer) { observers.add(observer); }

    public Post createPost(User author, String content) {
        Post post = new Post(author, content);
        postRepository.save(post);
        author.addPost(post);
        observers.forEach(observer -> observer.onPostCreated(post)); // Notify observers
        return post;
    }

    public void likePost(User user, String postId) {
        Post post = postRepository.findById(postId);
        post.addLike(user);
        observers.forEach(observer -> observer.onLike(post, user));
    }

    public void addComment(User author, String commentableId, String content) {
        Comment comment = new Comment(author, content);
        Post post = postRepository.findById(commentableId);
        post.addComment(comment);
        observers.forEach(observer -> observer.onComment(post, comment));
    }
}





class UserService {
    private final UserRepository userRepository = UserRepository.getInstance();

    public User createUser(String name, String email) {
        User user = new User(name, email);
        userRepository.save(user);
        return user;
    }

    public void addFriend(String userId1, String userId2) {
        User user1 = userRepository.findById(userId1);
        User user2 = userRepository.findById(userId2);

        user1.addFriend(user2);
        user2.addFriend(user1);
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId);
    }
}




class ChronologicalStrategy implements NewsFeedGenerationStrategy {
    @Override
    public List<Post> generateFeed(User user) {
        Set<User> friends = user.getFriends();
        List<Post> feed = new ArrayList<>();

        for (User friend: friends) {
            feed.addAll(friend.getPosts());
        }

        // Sort posts by timestamp in reverse (most recent first)
        feed.sort((p1, p2) -> p2.getTimestamp().compareTo(p1.getTimestamp()));

        return feed;
    }
}




interface NewsFeedGenerationStrategy {
    List<Post> generateFeed(User user);
}





import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;

public class SocialNetworkDemo {
    public static void main(String[] args) {
        SocialNetworkFacade socialNetwork = new SocialNetworkFacade();

        System.out.println("----------- 1. Creating Users -----------");
        User alice = socialNetwork.createUser("Alice", "alice@example.com");
        User bob = socialNetwork.createUser("Bob", "bob@example.com");
        User charlie = socialNetwork.createUser("Charlie", "charlie@example.com");
        System.out.println("Created users: " + alice.getName() + ", " + bob.getName() + ", " + charlie.getName());

        System.out.println("\n----------- 2. Building Friendships -----------");
        socialNetwork.addFriend(alice.getId(), bob.getId());
        socialNetwork.addFriend(bob.getId(), charlie.getId());
        System.out.println(alice.getName() + " and " + bob.getName() + " are now friends.");
        System.out.println(bob.getName() + " and " + charlie.getName() + " are now friends.");

        System.out.println("\n----------- 3. Users Create Posts -----------");
        Post alicePost = socialNetwork.createPost(alice.getId(), "Hello from Alice!");
        Post bobPost = socialNetwork.createPost(bob.getId(), "It's a beautiful day!");
        Post charliePost = socialNetwork.createPost(charlie.getId(), "Thinking about design patterns.");

        System.out.println("\n----------- 4. Users Interact with Posts -----------");
        socialNetwork.addComment(bob.getId(), alicePost.getId(), "Hey Alice, nice to see you here!");
        socialNetwork.likePost(charlie.getId(), alicePost.getId());

        System.out.println("\n----------- 5. Viewing News Feeds (Strategy Pattern) -----------");

        System.out.println("\n--- Alice's News Feed (should see Bob's post) ---");
        List<Post> alicesFeed = socialNetwork.getNewsFeed(alice.getId());
        printFeed(alicesFeed);

        System.out.println("\n--- Bob's News Feed (should see Alice's, and Charlie's post) ---");
        List<Post> bobsFeed = socialNetwork.getNewsFeed(bob.getId());
        printFeed(bobsFeed);

        System.out.println("\n--- Charlie's News Feed (should see Bob's post) ---");
        List<Post> charliesFeed = socialNetwork.getNewsFeed(charlie.getId());
        printFeed(charliesFeed);
    }

    private static void printFeed(List<Post> feed) {
        if (feed.isEmpty()) {
            System.out.println("  No posts in the feed.");
            return;
        }
        feed.forEach(post -> {
            System.out.println("  Post by " + post.getAuthor().getName() + " at " + post.getTimestamp());
            System.out.println("    \"" + post.getContent() + "\"");
            System.out.println("    Likes: " + post.getLikes().size() + ", Comments: " + post.getComments().size());
        });
    }
}





class SocialNetworkFacade {
    private final UserService userService;
    private final PostService postService;
    private final NewsFeedService newsFeedService;

    public SocialNetworkFacade() {
        this.userService = new UserService();
        this.postService = new PostService();
        this.newsFeedService = new NewsFeedService();
        // Wire up the observer
        postService.addObserver(new UserNotifier());
    }

    public User createUser(String name, String email) {
        return userService.createUser(name, email);
    }

    public void addFriend(String userId1, String userId2) {
        userService.addFriend(userId1, userId2);
    }

    public Post createPost(String authorId, String content) {
        User author = userService.getUserById(authorId);
        return postService.createPost(author, content);
    }

    public void addComment(String userId, String postId, String content) {
        User user = userService.getUserById(userId);
        postService.addComment(user, postId, content);
    }

    public void likePost(String userId, String postId) {
        User user = userService.getUserById(userId);
        postService.likePost(user, postId);
    }

    public List<Post> getNewsFeed(String userId) {
        User user = userService.getUserById(userId);
        return newsFeedService.getNewsFeed(user);
    }
}






















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































