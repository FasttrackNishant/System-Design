package easy.snakeandladder.java;

enum ConnectionStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}



enum NotificationType {
    CONNECTION_REQUEST,
    POST_LIKE,
    POST_COMMENT
}



class Comment {
    private final Member author;
    private final String text;
    private final LocalDateTime createdAt;

    public Comment(Member author, String text) {
        this.author = author;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }
    public Member getAuthor() { return author; }
    public String getText() { return text; }
}



class Connection {
    private final Member fromMember;
    private final Member toMember;
    private ConnectionStatus status;
    private final LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;

    public Connection(Member fromMember, Member toMember) {
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.status = ConnectionStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public Member getFromMember() { return fromMember; }
    public Member getToMember() { return toMember; }
    public ConnectionStatus getStatus() { return status; }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
        if (status == ConnectionStatus.ACCEPTED) {
            this.acceptedAt = LocalDateTime.now();
        }
    }
}




class Education {
    private final String school;
    private final String degree;
    private final int startYear;
    private final int endYear;

    public Education(String school, String degree, int startYear, int endYear) {
        this.school = school;
        this.degree = degree;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    @Override
    public String toString() {
        return String.format("%s, %s (%d - %d)", degree, school, startYear, endYear);
    }
}


class Experience {
    private final String title;
    private final String company;
    private final LocalDate startDate;
    private final LocalDate endDate; // Can be null for current job

    public Experience(String title, String company, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.company = company;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return String.format("%s at %s (%s to %s)", title, company, startDate, endDate == null ? "Present" : endDate);
    }
}



class Like {
    private final Member member;
    private final LocalDateTime createdAt;

    public Like(Member member) {
        this.member = member;
        this.createdAt = LocalDateTime.now();
    }
    public Member getMember() { return member; }
}




class Member implements NotificationObserver {
    private final String id;
    private final String name;
    private final String email;
    private final Profile profile;
    private final Set<Member> connections = new HashSet<>();
    private final List<Notification> notifications = new ArrayList<>();

    private Member(String id, String name, String email, Profile profile) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profile = profile;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Set<Member> getConnections() { return connections; }
    public Profile getProfile() { return profile; }

    public void addConnection(Member member) {
        connections.add(member);
    }

    public void displayProfile() {
        System.out.println("\n--- Profile for " + name + " (" + email + ") ---");
        profile.display();
        System.out.println("  Connections: " + connections.size());
    }

    public void viewNotifications() {
        System.out.println("\n--- Notifications for " + name + " ---");
        if (notifications.isEmpty()) {
            System.out.println("  No new notifications.");
            return;
        }
        notifications.stream()
                .filter(n -> !n.isRead())
                .forEach(n -> {
                    System.out.println("  - " + n.getContent());
                    n.markAsRead(); // Mark as read after viewing
                });
    }

    @Override
    public void update(Notification notification) {
        this.notifications.add(notification);
        System.out.printf("Notification pushed to %s: %s%n", this.name, notification.getContent());
    }

    // Builder Class
    public static class Builder {
        private final String id;
        private final String name;
        private final String email;
        private final Profile profile = new Profile();

        public Builder(String name, String email) {
            this.id = UUID.randomUUID().toString();
            this.name = name;
            this.email = email;
        }

        public Builder withSummary(String summary) {
            this.profile.setSummary(summary);
            return this;
        }

        public Builder addExperience(Experience experience) {
            this.profile.addExperience(experience);
            return this;
        }

        public Builder addEducation(Education education) {
            this.profile.addEducation(education);
            return this;
        }

        public Member build() {
            return new Member(id, name, email, profile);
        }
    }
}






class NewsFeed {
    private final List<Post> posts;

    public NewsFeed(List<Post> posts) {
        this.posts = posts;
    }

    public void display(FeedSortingStrategy strategy) {
        List<Post> sortedPosts = strategy.sort(posts);
        if (sortedPosts.isEmpty()) {
            System.out.println("  Your news feed is empty.");
            return;
        }
        sortedPosts.forEach(post -> {
            System.out.println("----------------------------------------");
            System.out.printf("Post by: %s (at %s)%n", post.getAuthor().getName(), post.getCreatedAt().toLocalDate());
            System.out.println("Content: " + post.getContent());
            System.out.printf("Likes: %d, Comments: %d%n", post.getLikes().size(), post.getComments().size());
            System.out.println("----------------------------------------");
        });
    }
}





class Notification {
    private final String id;
    private final String memberId; // The ID of the member to notify
    private final NotificationType type;
    private final String content;
    private final LocalDateTime createdAt;
    private boolean isRead = false;

    public Notification(String memberId, NotificationType type, String content) {
        this.id = UUID.randomUUID().toString();
        this.memberId = memberId;
        this.type = type;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public String getContent() { return content; }
    public void markAsRead() { this.isRead = true; }
    public boolean isRead() { return isRead; }
}





class Post extends Subject {
    private final String id;
    private final Member author;
    private final String content;
    private final LocalDateTime createdAt;
    private final List<Like> likes = new ArrayList<>();
    private final List<Comment> comments = new ArrayList<>();

    public Post(Member author, String content) {
        this.id = UUID.randomUUID().toString();
        this.author = author;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        // The author should be notified of interactions with their own post
        this.addObserver(author);
    }

    public void addLike(Member member) {
        likes.add(new Like(member));
        String notificationContent = member.getName() + " liked your post.";
        Notification notification = new Notification(author.getId(), NotificationType.POST_LIKE, notificationContent);
        notifyObservers(notification);
    }

    public void addComment(Member member, String text) {
        comments.add(new Comment(member, text));
        String notificationContent = member.getName() + " commented on your post: \"" + text + "\"";
        Notification notification = new Notification(author.getId(), NotificationType.POST_COMMENT, notificationContent);
        notifyObservers(notification);
    }

    public String getId() { return id; }
    public Member getAuthor() { return author; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Like> getLikes() { return likes; }
    public List<Comment> getComments() { return comments; }
}






class Profile {
    private String summary;
    private final List<Experience> experiences = new ArrayList<>();
    private final List<Education> educations = new ArrayList<>();

    public void setSummary(String summary) { this.summary = summary; }
    public void addExperience(Experience experience) { experiences.add(experience); }
    public void addEducation(Education education) { educations.add(education); }

    public void display() {
        System.out.println("  Summary: " + (summary != null ? summary : "N/A"));

        System.out.println("  Experience:");
        if (experiences.isEmpty())
            System.out.println("    - None");
        else
            experiences.forEach(exp -> System.out.println("    - " + exp));

        System.out.println("  Education:");
        if (educations.isEmpty())
            System.out.println("    - None");
        else
            educations.forEach(edu -> System.out.println("    - " + edu));
    }
}













interface NotificationObserver {
    void update(Notification notification);
}


abstract class Subject {
    private final List<NotificationObserver> observers = new ArrayList<>();

    public void addObserver(NotificationObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(NotificationObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(Notification notification) {
        for (NotificationObserver observer : observers) {
            observer.update(notification);
        }
    }
}














class ConnectionService {
    private final NotificationService notificationService;
    // Simulates a DB table for connection requests
    private final Map<String, Connection> connectionRequests = new ConcurrentHashMap<>();

    public ConnectionService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public String sendRequest(Member from, Member to) {
        Connection connection = new Connection(from, to);
        String requestId = UUID.randomUUID().toString();
        connectionRequests.put(requestId, connection);

        System.out.printf("%s sent a connection request to %s.%n", from.getName(), to.getName());

        Notification notification = new Notification(
                to.getId(),
                NotificationType.CONNECTION_REQUEST,
                from.getName() + " wants to connect with you. Request ID: " + requestId
        );
        notificationService.sendNotification(to, notification);

        return requestId;
    }

    public void acceptRequest(String requestId) {
        Connection request = connectionRequests.get(requestId);
        if (request != null && request.getStatus() == ConnectionStatus.PENDING) {
            request.setStatus(ConnectionStatus.ACCEPTED);

            Member from = request.getFromMember();
            Member to = request.getToMember();

            from.addConnection(to);
            to.addConnection(from);

            System.out.printf("%s accepted the connection request from %s.%n", to.getName(), from.getName());
            connectionRequests.remove(requestId); // Clean up
        } else {
            System.out.println("Invalid or already handled request ID.");
        }
    }
}






class NewsFeedService {
    private final Map<String, List<Post>> allPosts; // A map of memberId -> list of their posts

    public NewsFeedService() {
        this.allPosts = new ConcurrentHashMap<>();
    }

    public void addPost(Member member, Post post) {
        String memberId = member.getId();
        if(!allPosts.containsKey(memberId)) {
            allPosts.put(memberId, new ArrayList<>());
        }
        allPosts.get(memberId).add(post);
    }

    public List<Post> getMemberPosts(Member member) {
        return allPosts.getOrDefault(member.getId(), new ArrayList<>());
    }

    public void displayFeedForMember(Member member, FeedSortingStrategy feedSortingStrategy) {
        List<Post> feedPosts = new ArrayList<>();
        // Add posts from the member's connections
        for (Member connection : member.getConnections()) {
            List<Post> connectionPosts = allPosts.get(connection.getId());
            if (connectionPosts != null) {
                feedPosts.addAll(connectionPosts);
            }
        }

        NewsFeed feed = new NewsFeed(feedPosts);
        feed.display(feedSortingStrategy);
    }
}





class NotificationService {
    public void sendNotification(Member member, Notification notification) {
        // In a real system, this would push to a queue or a websocket.
        // Here, we directly call the member's update method.
        member.update(notification);
    }
}





class SearchService {
    private final Collection<Member> members;

    public SearchService(Collection<Member> members) {
        this.members = members;
    }

    public List<Member> searchByName(String name) {
        List<Member> result = new ArrayList<>();
        members.stream()
            .filter(member -> member.getName().toLowerCase().contains(name.toLowerCase())) // substring search
            .forEach(result::add);
        return result;
    }
}










interface FeedSortingStrategy {
    List<Post> sort(List<Post> posts);
}



class ChronologicalSortStrategy implements FeedSortingStrategy {
    @Override
    public List<Post> sort(List<Post> posts) {
        List<Post> result = new ArrayList<>();
        posts.stream()
            .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
            .forEach(result::add);
        return result;
    }
}











import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class LinkedInDemo {
    public static void main(String[] args) {
        LinkedInSystem system = LinkedInSystem.getInstance();

        // --- 1. Create Members using the Builder Pattern ---
        System.out.println("--- 1. Member Registration ---");
        Member alice = new Member.Builder("Alice", "alice@example.com")
                .withSummary("Senior Software Engineer with 10 years of experience.")
                .addExperience(new Experience("Sr. Software Engineer", "Google", LocalDate.of(2018, 1, 1), null))
                .addExperience(new Experience("Software Engineer", "Microsoft", LocalDate.of(2014, 6, 1), LocalDate.of(2017, 12, 31)))
                .addEducation(new Education("Princeton University", "M.S. in Computer Science", 2012, 2014))
                .build();

        Member bob = new Member.Builder("Bob", "bob@example.com")
                .withSummary("Product Manager at Stripe.")
                .addExperience(new Experience("Product Manager", "Stripe", LocalDate.of(2020, 2, 1), null))
                .addEducation(new Education("MIT", "B.S. in Business Analytics", 2015, 2019))
                .build();

        Member charlie = new Member.Builder("Charlie", "charlie@example.com").build();

        system.registerMember(alice);
        system.registerMember(bob);
        system.registerMember(charlie);

        alice.displayProfile();

        // --- 2. Connection Management ---
        System.out.println("\n--- 2. Connection Management ---");
        // Alice sends requests to Bob and Charlie
        String requestId1 = system.sendConnectionRequest(alice, bob);
        String requestId2 = system.sendConnectionRequest(alice, charlie);

        bob.viewNotifications(); // Bob sees Alice's request.

        System.out.println("\nBob accepts Alice's request.");
        system.acceptConnectionRequest(requestId1);
        System.out.println("Alice and Bob are now connected.");

        // --- 3. Posting and News Feed ---
        System.out.println("\n--- 3. Posting & News Feed ---");
        bob.displayProfile(); // Bob has 1 connection
        system.createPost(bob.getId(), "Excited to share we've launched our new feature! #productmanagement");

        // Alice views her news feed. She should see Bob's post.
        system.viewNewsFeed(alice.getId());

        // Charlie views his feed. It should be empty as he is not connected to anyone.
        system.viewNewsFeed(charlie.getId());

        // --- 4. Interacting with a Post (Observer Pattern in action) ---
        System.out.println("\n--- 4. Post Interaction & Notifications ---");
        Post bobsPost = system.getLatestPostByMember(bob.getId());
        if (bobsPost != null) {
            bobsPost.addLike(alice);
            bobsPost.addComment(alice, "This looks amazing! Great work!");
        }

        // Bob checks his notifications. He should see a like and a comment from Alice.
        bob.viewNotifications();

        // --- 5. Searching for Members ---
        System.out.println("\n--- 5. Member Search ---");
        List<Member> searchResults = system.searchMemberByName("ali");
        System.out.println("Search results for 'ali':");
        searchResults.forEach(m -> System.out.println(" - " + m.getName()));
    }
}












class LinkedInSystem {
    private static volatile LinkedInSystem instance;

    // Data stores (simulating databases)
    private final Map<String, Member> members = new ConcurrentHashMap<>();

    // Services
    private final ConnectionService connectionService;
    private final NewsFeedService newsFeedService;
    private final SearchService searchService;

    private LinkedInSystem() {
        // Initialize services
        this.connectionService = new ConnectionService(new NotificationService());
        this.newsFeedService = new NewsFeedService();
        this.searchService = new SearchService(members.values());
    }

    public static LinkedInSystem getInstance() {
        if (instance == null) {
            synchronized (LinkedInSystem.class) {
                if (instance == null) {
                    instance = new LinkedInSystem();
                }
            }
        }
        return instance;
    }

    public void registerMember(Member member) {
        members.put(member.getId(), member);
        System.out.println("New member registered: " + member.getName());
    }

    public Member getMember(String name) {
        return members.values().stream().filter(m -> m.getName().equals(name)).findFirst().orElse(null);
    }

    public String sendConnectionRequest(Member from, Member to) {
        return connectionService.sendRequest(from, to);
    }

    public void acceptConnectionRequest(String requestId) {
        connectionService.acceptRequest(requestId);
    }

    public void createPost(String memberId, String content) {
        Member author = members.get(memberId);
        Post post = new Post(author, content);
        newsFeedService.addPost(author, post);
        System.out.printf("%s created a new post.%n", author.getName());
    }

    public Post getLatestPostByMember(String memberId) {
        List<Post> memberPosts = newsFeedService.getMemberPosts(members.get(memberId));
        if (memberPosts == null || memberPosts.isEmpty()) return null;
        return memberPosts.get(memberPosts.size() - 1);
    }

    public void viewNewsFeed(String memberId) {
        Member member = members.get(memberId);
        System.out.println("\n--- News Feed for " + member.getName() + " ---");
        // Using the default chronological strategy
        newsFeedService.displayFeedForMember(member, new ChronologicalSortStrategy());
    }

    public List<Member> searchMemberByName(String name) {
        return searchService.searchByName(name);
    }
}




















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































