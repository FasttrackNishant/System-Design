class Comment {
private:
    Member* author;
    string text;
    time_t createdAt;

public:
    Comment(Member* author, const string& text) : author(author), text(text) {
        createdAt = time(nullptr);
    }

    Member* getAuthor() const { return author; }
    string getText() const { return text; }
};




class Connection {
private:
    Member* fromMember;
    Member* toMember;
    ConnectionStatus status;
    time_t requestedAt;
    time_t acceptedAt;

public:
    Connection(Member* fromMember, Member* toMember)
        : fromMember(fromMember), toMember(toMember), status(ConnectionStatus::PENDING) {
        requestedAt = time(nullptr);
        acceptedAt = 0;
    }

    Member* getFromMember() const { return fromMember; }
    Member* getToMember() const { return toMember; }
    ConnectionStatus getStatus() const { return status; }

    void setStatus(ConnectionStatus status) {
        this->status = status;
        if (status == ConnectionStatus::ACCEPTED) {
            acceptedAt = time(nullptr);
        }
    }
};




class Education {
private:
    string school;
    string degree;
    int startYear;
    int endYear;

public:
    Education(const string& school, const string& degree, int startYear, int endYear)
        : school(school), degree(degree), startYear(startYear), endYear(endYear) {}

    string toString() const {
        return degree + ", " + school + " (" + to_string(startYear) + " - " + to_string(endYear) + ")";
    }
};




class Experience {
private:
    string title;
    string company;
    string startDate;
    string endDate; // Empty string for current job

public:
    Experience(const string& title, const string& company, const string& startDate, const string& endDate)
        : title(title), company(company), startDate(startDate), endDate(endDate) {}

    string toString() const {
        string end = endDate.empty() ? "Present" : endDate;
        return title + " at " + company + " (" + startDate + " to " + end + ")";
    }
};




class Like {
private:
    Member* member;
    time_t createdAt;

public:
    Like(Member* member) : member(member) {
        createdAt = time(nullptr);
    }

    Member* getMember() const { return member; }
};





class Member : public NotificationObserver {
private:
    string id;
    string name;
    string email;
    Profile* profile;
    set<Member*> connections;
    vector<Notification*> notifications;

    Member(const string& id, const string& name, const string& email, Profile* profile)
        : id(id), name(name), email(email), profile(profile) {}

public:
    ~Member() {
        delete profile;
        for (Notification* notif : notifications) {
            delete notif;
        }
    }

    string getId() const { return id; }
    string getName() const { return name; }
    string getEmail() const { return email; }
    set<Member*> getConnections() const { return connections; }
    Profile* getProfile() const { return profile; }

    void addConnection(Member* member) {
        connections.insert(member);
    }

    void displayProfile() const {
        cout << "\n--- Profile for " << name << " (" << email << ") ---" << endl;
        profile->display();
        cout << "  Connections: " << connections.size() << endl;
    }

    void viewNotifications() {
        cout << "\n--- Notifications for " << name << " ---" << endl;
        
        vector<Notification*> unreadNotifications;
        for (Notification* notif : notifications) {
            if (!notif->isRead()) {
                unreadNotifications.push_back(notif);
            }
        }

        if (unreadNotifications.empty()) {
            cout << "  No new notifications." << endl;
            return;
        }

        for (Notification* notif : unreadNotifications) {
            cout << "  - " << notif->getContent() << endl;
            notif->markAsRead();
        }
    }

    void update(Notification* notification) override {
        notifications.push_back(notification);
        cout << "Notification pushed to " << name << ": " << notification->getContent() << endl;
    }

    class Builder {
    private:
        string id;
        string name;
        string email;
        Profile* profile;

    public:
        Builder(const string& name, const string& email) : name(name), email(email) {
            static int counter = 1000;
            id = "MEMBER-" + to_string(counter++);
            profile = new Profile();
        }

        Builder& withSummary(const string& summary) {
            profile->setSummary(summary);
            return *this;
        }

        Builder& addExperience(Experience* experience) {
            profile->addExperience(experience);
            return *this;
        }

        Builder& addEducation(Education* education) {
            profile->addEducation(education);
            return *this;
        }

        Member* build() {
            return new Member(id, name, email, profile);
        }
    };
};




class NewsFeed {
private:
    vector<Post*> posts;

public:
    NewsFeed(const vector<Post*>& posts) : posts(posts) {}

    void display(FeedSortingStrategy* strategy) {
        vector<Post*> sortedPosts = strategy->sort(posts);
        if (sortedPosts.empty()) {
            cout << "  Your news feed is empty." << endl;
            return;
        }

        for (const Post* post : sortedPosts) {
            cout << "----------------------------------------" << endl;
            cout << "Post by: " << post->getAuthor()->getName() << " (at " << post->getCreatedAt() << ")" << endl;
            cout << "Content: " << post->getContent() << endl;
            cout << "Likes: " << post->getLikes().size() << ", Comments: " << post->getComments().size() << endl;
            cout << "----------------------------------------" << endl;
        }
    }
};




class Notification {
private:
    string id;
    string memberId;
    NotificationType type;
    string content;
    time_t createdAt;
    bool readStatus;

public:
    Notification(const string& memberId, NotificationType type, const string& content)
        : memberId(memberId), type(type), content(content), readStatus(false) {
        static int counter = 1000;
        id = "NOTIF-" + to_string(counter++);
        createdAt = time(nullptr);
    }

    string getContent() const { return content; }
    void markAsRead() { readStatus = true; }
    bool isRead() const { return readStatus; }
};





class Post : public Subject {
private:
    string id;
    Member* author;
    string content;
    time_t createdAt;
    vector<Like*> likes;
    vector<Comment*> comments;

public:
    Post(Member* author, const string& content) : author(author), content(content) {
        static int counter = 1000;
        id = "POST-" + to_string(counter++);
        createdAt = time(nullptr);
        addObserver(author);
    }

    ~Post() {
        for (Like* like : likes) delete like;
        for (Comment* comment : comments) delete comment;
    }

    void addLike(Member* member) {
        likes.push_back(new Like(member));
        string notificationContent = member->getName() + " liked your post.";
        Notification* notification = new Notification(author->getId(), NotificationType::POST_LIKE, notificationContent);
        notifyObservers(notification);
    }

    void addComment(Member* member, const string& text) {
        comments.push_back(new Comment(member, text));
        string notificationContent = member->getName() + " commented on your post: \"" + text + "\"";
        Notification* notification = new Notification(author->getId(), NotificationType::POST_COMMENT, notificationContent);
        notifyObservers(notification);
    }

    string getId() const { return id; }
    Member* getAuthor() const { return author; }
    string getContent() const { return content; }
    time_t getCreatedAt() const { return createdAt; }
    vector<Like*> getLikes() const { return likes; }
    vector<Comment*> getComments() const { return comments; }
};




class Profile {
private:
    string summary;
    vector<Experience*> experiences;
    vector<Education*> educations;

public:
    ~Profile() {
        for (Experience* exp : experiences) delete exp;
        for (Education* edu : educations) delete edu;
    }

    void setSummary(const string& summary) { this->summary = summary; }
    
    void addExperience(Experience* experience) { experiences.push_back(experience); }
    
    void addEducation(Education* education) { educations.push_back(education); }

    void display() const {
        cout << "  Summary: " << (summary.empty() ? "N/A" : summary) << endl;

        cout << "  Experience:" << endl;
        if (experiences.empty()) {
            cout << "    - None" << endl;
        } else {
            for (const Experience* exp : experiences) {
                cout << "    - " << exp->toString() << endl;
            }
        }

        cout << "  Education:" << endl;
        if (educations.empty()) {
            cout << "    - None" << endl;
        } else {
            for (const Education* edu : educations) {
                cout << "    - " << edu->toString() << endl;
            }
        }
    }
};










enum class ConnectionStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
};




enum class NotificationType {
    CONNECTION_REQUEST,
    POST_LIKE,
    POST_COMMENT
};









class NotificationObserver {
public:
    virtual ~NotificationObserver() = default;
    virtual void update(Notification* notification) = 0;
};



class Subject {
protected:
    vector<NotificationObserver*> observers;

public:
    virtual ~Subject() = default;

    void addObserver(NotificationObserver* observer) {
        observers.push_back(observer);
    }

    void removeObserver(NotificationObserver* observer) {
        observers.erase(remove(observers.begin(), observers.end(), observer), observers.end());
    }

    void notifyObservers(Notification* notification) {
        for (NotificationObserver* observer : observers) {
            observer->update(notification);
        }
    }
};







class ConnectionService {
private:
    NotificationService* notificationService;
    map<string, Connection*> connectionRequests;
    mutable mutex mtx;

public:
    ConnectionService(NotificationService* notificationService) 
        : notificationService(notificationService) {}

    ~ConnectionService() {
        for (auto& pair : connectionRequests) {
            delete pair.second;
        }
    }

    string sendRequest(Member* from, Member* to) {
        Connection* connection = new Connection(from, to);
        static int counter = 1000;
        string requestId = "REQ-" + to_string(counter++);
        
        lock_guard<mutex> lock(mtx);
        connectionRequests[requestId] = connection;

        cout << from->getName() << " sent a connection request to " << to->getName() << "." << endl;

        Notification* notification = new Notification(
            to->getId(),
            NotificationType::CONNECTION_REQUEST,
            from->getName() + " wants to connect with you. Request ID: " + requestId
        );
        notificationService->sendNotification(to, notification);

        return requestId;
    }

    void acceptRequest(const string& requestId) {
        lock_guard<mutex> lock(mtx);
        auto it = connectionRequests.find(requestId);
        
        if (it != connectionRequests.end() && it->second->getStatus() == ConnectionStatus::PENDING) {
            Connection* request = it->second;
            request->setStatus(ConnectionStatus::ACCEPTED);

            Member* from = request->getFromMember();
            Member* to = request->getToMember();

            from->addConnection(to);
            to->addConnection(from);

            cout << to->getName() << " accepted the connection request from " << from->getName() << "." << endl;
            
            delete request;
            connectionRequests.erase(it);
        } else {
            cout << "Invalid or already handled request ID." << endl;
        }
    }
};





class NewsFeedService {
private:
    map<string, vector<Post*>> allPosts;
    mutable mutex mtx;

public:
    void addPost(Member* member, Post* post) {
        lock_guard<mutex> lock(mtx);
        allPosts[member->getId()].push_back(post);
    }

    vector<Post*> getMemberPosts(Member* member) {
        lock_guard<mutex> lock(mtx);
        auto it = allPosts.find(member->getId());
        if (it != allPosts.end()) {
            return it->second;
        }
        return vector<Post*>();
    }

    void displayFeedForMember(Member* member, FeedSortingStrategy* feedSortingStrategy) {
        vector<Post*> feedPosts;
        
        for (Member* connection : member->getConnections()) {
            vector<Post*> connectionPosts = getMemberPosts(connection);
            feedPosts.insert(feedPosts.end(), connectionPosts.begin(), connectionPosts.end());
        }

        NewsFeed feed(feedPosts);
        feed.display(feedSortingStrategy);
    }
};





class NotificationService {
public:
    void sendNotification(Member* member, Notification* notification) {
        member->update(notification);
    }
};







class SearchService {
private:
    const map<string, Member*>* members;

public:
    SearchService(const map<string, Member*>* members) : members(members) {}

    vector<Member*> searchByName(const string& name) {
        vector<Member*> results;
        string lowerName = name;
        transform(lowerName.begin(), lowerName.end(), lowerName.begin(), ::tolower);

        for (const auto& pair : *members) {
            string memberName = pair.second->getName();
            transform(memberName.begin(), memberName.end(), memberName.begin(), ::tolower);
            if (memberName.find(lowerName) != string::npos) {
                results.push_back(pair.second);
            }
        }
        return results;
    }
};








class ChronologicalSortStrategy : public FeedSortingStrategy {
public:
    vector<Post*> sort(const vector<Post*>& posts) override {
        vector<Post*> sortedPosts = posts;
        std::sort(sortedPosts.begin(), sortedPosts.end(), 
                 [](const Post* a, const Post* b) {
                     return a->getCreatedAt() > b->getCreatedAt();
                 });
        return sortedPosts;
    }
};



class FeedSortingStrategy {
public:
    virtual ~FeedSortingStrategy() = default;
    virtual vector<Post*> sort(const vector<Post*>& posts) = 0;
};

















int main() {
    LinkedInSystem* system = LinkedInSystem::getInstance();

    // 1. Create Members using the Builder Pattern
    cout << "--- 1. Member Registration ---" << endl;
    Member* alice = Member::Builder("Alice", "alice@example.com")
        .withSummary("Senior Software Engineer with 10 years of experience.")
        .addExperience(new Experience("Sr. Software Engineer", "Google", "2018-01-01", ""))
        .addExperience(new Experience("Software Engineer", "Microsoft", "2014-06-01", "2017-12-31"))
        .addEducation(new Education("Princeton University", "M.S. in Computer Science", 2012, 2014))
        .build();

    Member* bob = Member::Builder("Bob", "bob@example.com")
        .withSummary("Product Manager at Stripe.")
        .addExperience(new Experience("Product Manager", "Stripe", "2020-02-01", ""))
        .addEducation(new Education("MIT", "B.S. in Business Analytics", 2015, 2019))
        .build();

    Member* charlie = Member::Builder("Charlie", "charlie@example.com").build();

    system->registerMember(alice);
    system->registerMember(bob);
    system->registerMember(charlie);

    alice->displayProfile();

    // 2. Connection Management
    cout << "\n--- 2. Connection Management ---" << endl;
    string requestId1 = system->sendConnectionRequest(alice, bob);
    string requestId2 = system->sendConnectionRequest(alice, charlie);

    bob->viewNotifications();

    cout << "\nBob accepts Alice's request." << endl;
    system->acceptConnectionRequest(requestId1);
    cout << "Alice and Bob are now connected." << endl;

    // 3. Posting and News Feed
    cout << "\n--- 3. Posting & News Feed ---" << endl;
    bob->displayProfile();
    system->createPost(bob->getId(), "Excited to share we've launched our new feature! #productmanagement");

    system->viewNewsFeed(alice->getId());
    system->viewNewsFeed(charlie->getId());

    // 4. Interacting with a Post
    cout << "\n--- 4. Post Interaction & Notifications ---" << endl;
    Post* bobsPost = system->getLatestPostByMember(bob->getId());
    if (bobsPost != nullptr) {
        bobsPost->addLike(alice);
        bobsPost->addComment(alice, "This looks amazing! Great work!");
    }

    bob->viewNotifications();

    // 5. Searching for Members
    cout << "\n--- 5. Member Search ---" << endl;
    vector<Member*> searchResults = system->searchMemberByName("ali");
    cout << "Search results for 'ali':" << endl;
    for (Member* member : searchResults) {
        cout << " - " << member->getName() << endl;
    }

    return 0;
}

















class LinkedInSystem {
private:
    static LinkedInSystem* instance;
    static mutex instanceMutex;

    map<string, Member*> members;
    ConnectionService* connectionService;
    NewsFeedService* newsFeedService;
    SearchService* searchService;

    LinkedInSystem() {
        connectionService = new ConnectionService(new NotificationService());
        newsFeedService = new NewsFeedService();
        searchService = new SearchService(&members);
    }

public:
    static LinkedInSystem* getInstance() {
        if (instance == nullptr) {
            lock_guard<mutex> lock(instanceMutex);
            if (instance == nullptr) {
                instance = new LinkedInSystem();
            }
        }
        return instance;
    }

    void registerMember(Member* member) {
        members[member->getId()] = member;
        cout << "New member registered: " << member->getName() << endl;
    }

    Member* getMember(const string& name) {
        for (const auto& pair : members) {
            if (pair.second->getName() == name) {
                return pair.second;
            }
        }
        return nullptr;
    }

    string sendConnectionRequest(Member* from, Member* to) {
        return connectionService->sendRequest(from, to);
    }

    void acceptConnectionRequest(const string& requestId) {
        connectionService->acceptRequest(requestId);
    }

    void createPost(const string& memberId, const string& content) {
        Member* author = members[memberId];
        Post* post = new Post(author, content);
        newsFeedService->addPost(author, post);
        cout << author->getName() << " created a new post." << endl;
    }

    Post* getLatestPostByMember(const string& memberId) {
        vector<Post*> memberPosts = newsFeedService->getMemberPosts(members[memberId]);
        if (memberPosts.empty()) return nullptr;
        return memberPosts.back();
    }

    void viewNewsFeed(const string& memberId) {
        Member* member = members[memberId];
        cout << "\n--- News Feed for " << member->getName() << " ---" << endl;
        ChronologicalSortStrategy strategy;
        newsFeedService->displayFeedForMember(member, &strategy);
    }

    vector<Member*> searchMemberByName(const string& name) {
        return searchService->searchByName(name);
    }
};

// Static member definitions
LinkedInSystem* LinkedInSystem::instance = nullptr;
mutex LinkedInSystem::instanceMutex;










































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































