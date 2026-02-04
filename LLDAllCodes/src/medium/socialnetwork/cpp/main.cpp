class Comment : public CommentableEntity {
public:
    Comment(shared_ptr<User> author, const string& content)
        : CommentableEntity(author, content) {}

    vector<shared_ptr<Comment>> getReplies() const {
        return getComments();
    }
};



class CommentableEntity {
protected:
    string id;
    shared_ptr<User> author;
    string content;
    string timestamp;
    set<shared_ptr<User>> likes;
    vector<shared_ptr<Comment>> comments;

public:
    CommentableEntity(shared_ptr<User> author, const string& content)
        : id(generateId()), author(author), content(content), timestamp(getCurrentTimestamp()) {}

    virtual ~CommentableEntity() = default;

    void addLike(shared_ptr<User> user) {
        likes.insert(user);
    }

    void addComment(shared_ptr<Comment> comment) {
        comments.push_back(comment);
    }

    string getId() const { return id; }
    shared_ptr<User> getAuthor() const { return author; }
    string getContent() const { return content; }
    string getTimestamp() const { return timestamp; }
    vector<shared_ptr<Comment>> getComments() const { return comments; }
    set<shared_ptr<User>> getLikes() const { return likes; }
};



class Post : public CommentableEntity {
public:
    Post(shared_ptr<User> author, const string& content)
        : CommentableEntity(author, content) {}
};




class User {
private:
    string id;
    string name;
    string email;
    set<shared_ptr<User>> friends;
    vector<shared_ptr<Post>> posts;

public:
    User(const string& name, const string& email)
        : id(generateId()), name(name), email(email) {}

    void addFriend(shared_ptr<User> friendUser) {
        friends.insert(friendUser);
    }

    void addPost(shared_ptr<Post> post) {
        posts.push_back(post);
    }

    string getId() const { return id; }
    string getName() const { return name; }
    set<shared_ptr<User>> getFriends() const { return friends; }
    vector<shared_ptr<Post>> getPosts() const { return posts; }
};





class PostObserver {
public:
    virtual ~PostObserver() = default;
    virtual void onPostCreated(shared_ptr<Post> post) = 0;
    virtual void onLike(shared_ptr<Post> post, shared_ptr<User> user) = 0;
    virtual void onComment(shared_ptr<Post> post, shared_ptr<Comment> comment) = 0;
};


class UserNotifier : public PostObserver {
public:
    void onPostCreated(shared_ptr<Post> post) override {
        shared_ptr<User> author = post->getAuthor();
        for (const auto& friendUser : author->getFriends()) {
            cout << "Notification for " << friendUser->getName() << ": " 
                 << author->getName() << " created a new post: " << post->getContent() << endl;
        }
    }

    void onLike(shared_ptr<Post> post, shared_ptr<User> user) override {
        shared_ptr<User> author = post->getAuthor();
        cout << "Notification for " << author->getName() << ": " 
             << user->getName() << " liked your post" << endl;
    }

    void onComment(shared_ptr<Post> post, shared_ptr<Comment> comment) override {
        shared_ptr<User> author = post->getAuthor();
        cout << "Notification for " << author->getName() << ": " 
             << comment->getAuthor()->getName() << " commented on your post" << endl;
    }
};







class PostRepository {
private:
    static PostRepository* instance;
    static mutex instanceMutex;
    map<string, shared_ptr<Post>> posts;

    PostRepository() {}

public:
    static PostRepository* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new PostRepository();
        }
        return instance;
    }

    void save(shared_ptr<Post> post) {
        posts[post->getId()] = post;
    }

    shared_ptr<Post> findById(const string& id) {
        auto it = posts.find(id);
        return (it != posts.end()) ? it->second : nullptr;
    }
};

// Static member definitions (required for linking)
UserRepository* UserRepository::instance = nullptr;
mutex UserRepository::instanceMutex;

PostRepository* PostRepository::instance = nullptr;
mutex PostRepository::instanceMutex;









class UserRepository {
private:
    static UserRepository* instance;
    static mutex instanceMutex;
    map<string, shared_ptr<User>> users;

    UserRepository() {}

public:
    static UserRepository* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new UserRepository();
        }
        return instance;
    }

    void save(shared_ptr<User> user) {
        users[user->getId()] = user;
    }

    shared_ptr<User> findById(const string& id) {
        auto it = users.find(id);
        return (it != users.end()) ? it->second : nullptr;
    }
};












class NewsFeedService {
private:
    shared_ptr<NewsFeedGenerationStrategy> strategy;

public:
    NewsFeedService() : strategy(make_shared<ChronologicalStrategy>()) {}

    void setStrategy(shared_ptr<NewsFeedGenerationStrategy> newStrategy) {
        strategy = newStrategy;
    }

    vector<shared_ptr<Post>> getNewsFeed(shared_ptr<User> user) {
        return strategy->generateFeed(user);
    }
};





class PostService {
private:
    PostRepository* postRepository;
    vector<shared_ptr<PostObserver>> observers;

public:
    PostService() : postRepository(PostRepository::getInstance()) {}

    void addObserver(shared_ptr<PostObserver> observer) {
        observers.push_back(observer);
    }

    shared_ptr<Post> createPost(shared_ptr<User> author, const string& content) {
        shared_ptr<Post> post = make_shared<Post>(author, content);
        postRepository->save(post);
        author->addPost(post);
        for (const auto& observer : observers) {
            observer->onPostCreated(post);
        }
        return post;
    }

    void likePost(shared_ptr<User> user, const string& postId) {
        shared_ptr<Post> post = postRepository->findById(postId);
        post->addLike(user);
        for (const auto& observer : observers) {
            observer->onLike(post, user);
        }
    }

    void addComment(shared_ptr<User> author, const string& commentableId, const string& content) {
        shared_ptr<Comment> comment = make_shared<Comment>(author, content);
        shared_ptr<Post> post = postRepository->findById(commentableId);
        post->addComment(comment);
        for (const auto& observer : observers) {
            observer->onComment(post, comment);
        }
    }
};







class UserService {
private:
    UserRepository* userRepository;

public:
    UserService() : userRepository(UserRepository::getInstance()) {}

    shared_ptr<User> createUser(const string& name, const string& email) {
        shared_ptr<User> user = make_shared<User>(name, email);
        userRepository->save(user);
        return user;
    }

    void addFriend(const string& userId1, const string& userId2) {
        shared_ptr<User> user1 = userRepository->findById(userId1);
        shared_ptr<User> user2 = userRepository->findById(userId2);

        user1->addFriend(user2);
        user2->addFriend(user1);
    }

    shared_ptr<User> getUserById(const string& userId) {
        return userRepository->findById(userId);
    }
};











class ChronologicalStrategy : public NewsFeedGenerationStrategy {
public:
    vector<shared_ptr<Post>> generateFeed(shared_ptr<User> user) override {
        set<shared_ptr<User>> friends = user->getFriends();
        vector<shared_ptr<Post>> feed;

        for (const auto& friendUser : friends) {
            vector<shared_ptr<Post>> friendPosts = friendUser->getPosts();
            feed.insert(feed.end(), friendPosts.begin(), friendPosts.end());
        }

        // Sort posts by timestamp in reverse (most recent first)
        sort(feed.begin(), feed.end(), [](const shared_ptr<Post>& p1, const shared_ptr<Post>& p2) {
            return p1->getTimestamp() > p2->getTimestamp();
        });

        return feed;
    }
};




class NewsFeedGenerationStrategy {
public:
    virtual ~NewsFeedGenerationStrategy() = default;
    virtual vector<shared_ptr<Post>> generateFeed(shared_ptr<User> user) = 0;
};











void printFeed(const vector<shared_ptr<Post>>& feed) {
    if (feed.empty()) {
        cout << "  No posts in the feed." << endl;
        return;
    }
    
    for (const auto& post : feed) {
        cout << "  Post by " << post->getAuthor()->getName() << " at " << post->getTimestamp() << endl;
        cout << "    \"" << post->getContent() << "\"" << endl;
        cout << "    Likes: " << post->getLikes().size() << ", Comments: " << post->getComments().size() << endl;
    }
}

int main() {
    SocialNetworkFacade socialNetwork;

    cout << "----------- 1. Creating Users -----------" << endl;
    shared_ptr<User> alice = socialNetwork.createUser("Alice", "alice@example.com");
    shared_ptr<User> bob = socialNetwork.createUser("Bob", "bob@example.com");
    shared_ptr<User> charlie = socialNetwork.createUser("Charlie", "charlie@example.com");
    cout << "Created users: " << alice->getName() << ", " << bob->getName() << ", " << charlie->getName() << endl;

    cout << endl << "----------- 2. Building Friendships -----------" << endl;
    socialNetwork.addFriend(alice->getId(), bob->getId());
    socialNetwork.addFriend(bob->getId(), charlie->getId());
    cout << alice->getName() << " and " << bob->getName() << " are now friends." << endl;
    cout << bob->getName() << " and " << charlie->getName() << " are now friends." << endl;

    cout << endl << "----------- 3. Users Create Posts -----------" << endl;
    shared_ptr<Post> alicePost = socialNetwork.createPost(alice->getId(), "Hello from Alice!");
    shared_ptr<Post> bobPost = socialNetwork.createPost(bob->getId(), "It's a beautiful day!");
    shared_ptr<Post> charliePost = socialNetwork.createPost(charlie->getId(), "Thinking about design patterns.");

    cout << endl << "----------- 4. Users Interact with Posts -----------" << endl;
    socialNetwork.addComment(bob->getId(), alicePost->getId(), "Hey Alice, nice to see you here!");
    socialNetwork.likePost(charlie->getId(), alicePost->getId());

    cout << endl << "----------- 5. Viewing News Feeds (Strategy Pattern) -----------" << endl;

    cout << endl << "--- Alice's News Feed (should see Bob's post) ---" << endl;
    vector<shared_ptr<Post>> alicesFeed = socialNetwork.getNewsFeed(alice->getId());
    printFeed(alicesFeed);

    cout << endl << "--- Bob's News Feed (should see Alice's, and Charlie's post) ---" << endl;
    vector<shared_ptr<Post>> bobsFeed = socialNetwork.getNewsFeed(bob->getId());
    printFeed(bobsFeed);

    cout << endl << "--- Charlie's News Feed (should see Bob's post) ---" << endl;
    vector<shared_ptr<Post>> charliesFeed = socialNetwork.getNewsFeed(charlie->getId());
    printFeed(charliesFeed);

    return 0;
}







class SocialNetworkFacade {
private:
    unique_ptr<UserService> userService;
    unique_ptr<PostService> postService;
    unique_ptr<NewsFeedService> newsFeedService;

public:
    SocialNetworkFacade() 
        : userService(make_unique<UserService>()),
          postService(make_unique<PostService>()),
          newsFeedService(make_unique<NewsFeedService>()) {
        // Wire up the observer
        postService->addObserver(make_shared<UserNotifier>());
    }

    shared_ptr<User> createUser(const string& name, const string& email) {
        return userService->createUser(name, email);
    }

    void addFriend(const string& userId1, const string& userId2) {
        userService->addFriend(userId1, userId2);
    }

    shared_ptr<Post> createPost(const string& authorId, const string& content) {
        shared_ptr<User> author = userService->getUserById(authorId);
        return postService->createPost(author, content);
    }

    void addComment(const string& userId, const string& postId, const string& content) {
        shared_ptr<User> user = userService->getUserById(userId);
        postService->addComment(user, postId, content);
    }

    void likePost(const string& userId, const string& postId) {
        shared_ptr<User> user = userService->getUserById(userId);
        postService->likePost(user, postId);
    }

    vector<shared_ptr<Post>> getNewsFeed(const string& userId) {
        shared_ptr<User> user = userService->getUserById(userId);
        return newsFeedService->getNewsFeed(user);
    }
};




























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































