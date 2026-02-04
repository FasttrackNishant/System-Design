


class Answer : public Post {
private:
    bool isAccepted;

public:
    Answer(const string& body, shared_ptr<User> author)
        : Post(generateUUID(), body, author), isAccepted(false) {}

    void setAccepted(bool accepted) {
        isAccepted = accepted;
    }

    bool isAcceptedAnswer() const { return isAccepted; }
    
    bool isQuestion() const override { return false; }
};





class Comment : public Content {
public:
    Comment(const string& body, shared_ptr<User> author)
        : Content(generateUUID(), body, author) {}
};










class Content {
protected:
    string id;
    string body;
    shared_ptr<User> author;
    time_t creationTime;

public:
    Content(const string& id, const string& body, shared_ptr<User> author)
        : id(id), body(body), author(author) {
        creationTime = time(NULL);
    }

    virtual ~Content() {}

    string getId() const { return id; }
    string getBody() const { return body; }
    shared_ptr<User> getAuthor() const { return author; }
};








class Event {
private:
    EventType type;
    shared_ptr<User> actor;
    shared_ptr<Post> targetPost;

public:
    Event(EventType type, shared_ptr<User> actor, shared_ptr<Post> targetPost)
        : type(type), actor(actor), targetPost(targetPost) {}

    EventType getType() const { return type; }
    shared_ptr<User> getActor() const { return actor; }
    shared_ptr<Post> getTargetPost() const { return targetPost; }
};










class Post : public Content {
protected:
    int voteCount;
    unordered_map<string, VoteType> voters;
    vector<shared_ptr<Content>> comments;
    vector<shared_ptr<PostObserver>> observers;

public:
    Post(const string& id, const string& body, shared_ptr<User> author)
        : Content(id, body, author), voteCount(0) {}

    virtual ~Post() {}

    void addObserver(shared_ptr<PostObserver> observer) {
        observers.push_back(observer);
    }

    void notifyObservers(const Event& event) {
        for (const auto& observer : observers) {
            observer->onPostEvent(event);
        }
    }

    // Virtual method to be overridden by derived classes
    virtual bool isQuestion() const = 0;

    void vote(shared_ptr<User> user, VoteType voteType) {
        string userId = user->getId();
        
        auto it = voters.find(userId);
        if (it != voters.end() && it->second == voteType) {
            return; // Already voted
        }

        int scoreChange = 0;
        if (it != voters.end()) { // User is changing their vote
            scoreChange = (voteType == VoteType::UPVOTE) ? 2 : -2;
        } else { // New vote
            scoreChange = (voteType == VoteType::UPVOTE) ? 1 : -1;
        }

        voters[userId] = voteType;
        voteCount += scoreChange;

        EventType eventType;
        if (isQuestion()) {
            eventType = (voteType == VoteType::UPVOTE) ? EventType::UPVOTE_QUESTION : EventType::DOWNVOTE_QUESTION;
        } else {
            eventType = (voteType == VoteType::UPVOTE) ? EventType::UPVOTE_ANSWER : EventType::DOWNVOTE_ANSWER;
        }

        shared_ptr<Post> thisPost = shared_ptr<Post>(this, [](Post*) {}); // Non-owning shared_ptr
        notifyObservers(Event(eventType, user, thisPost));
    }

    int getVoteCount() const { return voteCount; }
};










class Question : public Post {
private:
    string title;
    set<Tag> tags;
    vector<shared_ptr<Answer>> answers;
    shared_ptr<Answer> acceptedAnswer;

public:
    Question(const string& title, const string& body, shared_ptr<User> author, const set<Tag>& tags)
        : Post(generateUUID(), body, author), title(title), tags(tags), acceptedAnswer(NULL) {}

    void addAnswer(shared_ptr<Answer> answer) {
        answers.push_back(answer);
    }

    void acceptAnswer(shared_ptr<Answer> answer) {
        if (author->getId() != answer->getAuthor()->getId() && acceptedAnswer == NULL) {
            acceptedAnswer = answer;
            answer->setAccepted(true);
            shared_ptr<Post> answerPost = dynamic_pointer_cast<Post>(answer);
            notifyObservers(Event(EventType::ACCEPT_ANSWER, answer->getAuthor(), answerPost));
        }
    }

    string getTitle() const { return title; }
    set<Tag> getTags() const { return tags; }
    vector<shared_ptr<Answer>> getAnswers() const { return answers; }
    
    bool isQuestion() const override { return true; }
};











class Tag {
private:
    string name;

public:
    Tag(const string& name) : name(name) {}

    string getName() const { return name; }

    bool operator<(const Tag& other) const {
        return name < other.name;
    }
};











class User {
private:
    string id;
    string name;
    int reputation;

public:
    User(const string& name) : name(name), reputation(0) {
        id = generateUUID();
    }

    void updateReputation(int change) {
        reputation += change;
    }

    string getId() const { return id; }
    string getName() const { return name; }
    int getReputation() const { return reputation; }
};









enum class EventType {
    UPVOTE_QUESTION,
    DOWNVOTE_QUESTION,
    UPVOTE_ANSWER,
    DOWNVOTE_ANSWER,
    ACCEPT_ANSWER
};



enum class VoteType {
    UPVOTE,
    DOWNVOTE
};








class PostObserver {
public:
    virtual ~PostObserver() {}
    virtual void onPostEvent(const Event& event) = 0;
};







class ReputationManager : public PostObserver {
private:
    static const int QUESTION_UPVOTE_REP = 5;
    static const int ANSWER_UPVOTE_REP = 10;
    static const int ACCEPTED_ANSWER_REP = 15;
    static const int DOWNVOTE_REP_PENALTY = -1;
    static const int POST_DOWNVOTED_REP_PENALTY = -2;

public:
    void onPostEvent(const Event& event) {
        auto postAuthor = event.getTargetPost()->getAuthor();
        
        switch (event.getType()) {
            case EventType::UPVOTE_QUESTION:
                postAuthor->updateReputation(QUESTION_UPVOTE_REP);
                break;
            case EventType::DOWNVOTE_QUESTION:
                postAuthor->updateReputation(DOWNVOTE_REP_PENALTY);
                event.getActor()->updateReputation(POST_DOWNVOTED_REP_PENALTY);
                break;
            case EventType::UPVOTE_ANSWER:
                postAuthor->updateReputation(ANSWER_UPVOTE_REP);
                break;
            case EventType::DOWNVOTE_ANSWER:
                postAuthor->updateReputation(DOWNVOTE_REP_PENALTY);
                event.getActor()->updateReputation(POST_DOWNVOTED_REP_PENALTY);
                break;
            case EventType::ACCEPT_ANSWER:
                postAuthor->updateReputation(ACCEPTED_ANSWER_REP);
                break;
        }
    }
};














class KeywordSearchStrategy : public SearchStrategy {
private:
    string keyword;

public:
    KeywordSearchStrategy(const string& keyword) {
        this->keyword = keyword;
        transform(this->keyword.begin(), this->keyword.end(), this->keyword.begin(), ::tolower);
    }

    vector<shared_ptr<Question>> filter(const vector<shared_ptr<Question>>& questions) {
        vector<shared_ptr<Question>> result;
        for (const auto& q : questions) {
            string title = q->getTitle();
            string body = q->getBody();
            transform(title.begin(), title.end(), title.begin(), ::tolower);
            transform(body.begin(), body.end(), body.begin(), ::tolower);
            
            if (title.find(keyword) != string::npos || body.find(keyword) != string::npos) {
                result.push_back(q);
            }
        }
        return result;
    }
};




class SearchStrategy {
public:
    virtual ~SearchStrategy() {}
    virtual vector<shared_ptr<Question>> filter(const vector<shared_ptr<Question>>& questions) = 0;
};




class TagSearchStrategy : public SearchStrategy {
private:
    Tag tag;

public:
    TagSearchStrategy(const Tag& tag) : tag(tag) {}

    vector<shared_ptr<Question>> filter(const vector<shared_ptr<Question>>& questions) {
        vector<shared_ptr<Question>> result;
        for (const auto& q : questions) {
            auto tags = q->getTags();
            for (const auto& t : tags) {
                string tagName = t.getName();
                string searchTagName = tag.getName();
                transform(tagName.begin(), tagName.end(), tagName.begin(), ::tolower);
                transform(searchTagName.begin(), searchTagName.end(), searchTagName.begin(), ::tolower);
                
                if (tagName == searchTagName) {
                    result.push_back(q);
                    break;
                }
            }
        }
        return result;
    }
};





class UserSearchStrategy : public SearchStrategy {
private:
    shared_ptr<User> user;

public:
    UserSearchStrategy(shared_ptr<User> user) : user(user) {}

    vector<shared_ptr<Question>> filter(const vector<shared_ptr<Question>>& questions) {
        vector<shared_ptr<Question>> result;
        for (const auto& q : questions) {
            if (q->getAuthor()->getId() == user->getId()) {
                result.push_back(q);
            }
        }
        return result;
    }
};












class StackOverflowDemo {
public:
    static void main() {
        StackOverflowService service;

        // 1. Create Users
        auto alice = service.createUser("Alice");
        auto bob = service.createUser("Bob");
        auto charlie = service.createUser("Charlie");

        // 2. Alice posts a question
        cout << "--- Alice posts a question ---" << endl;
        Tag javaTag("java");
        Tag designPatternsTag("design-patterns");
        set<Tag> tags = {javaTag, designPatternsTag};
        auto question = service.postQuestion(alice->getId(), "How to implement Observer Pattern?", "Details about Observer Pattern...", tags);
        printReputations({alice, bob, charlie});

        // 3. Bob and Charlie post answers
        cout << "\n--- Bob and Charlie post answers ---" << endl;
        auto bobAnswer = service.postAnswer(bob->getId(), question->getId(), "You can use the java.util.Observer interface.");
        auto charlieAnswer = service.postAnswer(charlie->getId(), question->getId(), "A better way is to create your own Observer interface.");
        printReputations({alice, bob, charlie});

        // 4. Voting happens
        cout << "\n--- Voting Occurs ---" << endl;
        service.voteOnPost(alice->getId(), question->getId(), VoteType::UPVOTE);
        service.voteOnPost(bob->getId(), charlieAnswer->getId(), VoteType::UPVOTE);
        service.voteOnPost(alice->getId(), bobAnswer->getId(), VoteType::DOWNVOTE);
        printReputations({alice, bob, charlie});

        // 5. Alice accepts Charlie's answer
        cout << "\n--- Alice accepts Charlie's answer ---" << endl;
        service.acceptAnswer(question->getId(), charlieAnswer->getId());
        printReputations({alice, bob, charlie});

        // 6. Search for questions
        cout << "\n--- Combined Search: Questions by 'Alice' with tag 'java' ---" << endl;
        vector<shared_ptr<SearchStrategy>> filtersC;
        filtersC.push_back(make_shared<UserSearchStrategy>(alice));
        filtersC.push_back(make_shared<TagSearchStrategy>(javaTag));
        auto searchResults = service.searchQuestions(filtersC);
        for (const auto& q : searchResults) {
            cout << "  - Found: " << q->getTitle() << endl;
        }
    }

private:
    static void printReputations(const vector<shared_ptr<User>>& users) {
        cout << "--- Current Reputations ---" << endl;
        for (const auto& user : users) {
            cout << user->getName() << ": " << user->getReputation() << endl;
        }
    }
};

int main() {
    StackOverflowDemo::main();
    return 0;
}
















class StackOverflowService {
private:
    unordered_map<string, shared_ptr<User>> users;
    unordered_map<string, shared_ptr<Question>> questions;
    unordered_map<string, shared_ptr<Answer>> answers;
    shared_ptr<PostObserver> reputationManager;

public:
    StackOverflowService() {
        reputationManager = make_shared<ReputationManager>();
    }

    shared_ptr<User> createUser(const string& name) {
        auto user = make_shared<User>(name);
        users[user->getId()] = user;
        return user;
    }

    shared_ptr<Question> postQuestion(const string& userId, const string& title, const string& body, const set<Tag>& tags) {
        auto author = users[userId];
        auto question = make_shared<Question>(title, body, author, tags);
        question->addObserver(reputationManager);
        questions[question->getId()] = question;
        return question;
    }

    shared_ptr<Answer> postAnswer(const string& userId, const string& questionId, const string& body) {
        auto author = users[userId];
        auto question = questions[questionId];
        auto answer = make_shared<Answer>(body, author);
        answer->addObserver(reputationManager);
        question->addAnswer(answer);
        answers[answer->getId()] = answer;
        return answer;
    }

    void voteOnPost(const string& userId, const string& postId, VoteType voteType) {
        auto user = users[userId];
        auto post = findPostById(postId);
        post->vote(user, voteType);
    }

    void acceptAnswer(const string& questionId, const string& answerId) {
        auto question = questions[questionId];
        auto answer = answers[answerId];
        question->acceptAnswer(answer);
    }

    vector<shared_ptr<Question>> searchQuestions(const vector<shared_ptr<SearchStrategy>>& strategies) {
        vector<shared_ptr<Question>> results;
        for (const auto& pair : questions) {
            results.push_back(pair.second);
        }

        for (const auto& strategy : strategies) {
            results = strategy->filter(results);
        }

        return results;
    }

    shared_ptr<User> getUser(const string& userId) {
        return users[userId];
    }

private:
    shared_ptr<Post> findPostById(const string& postId) {
        auto questionIt = questions.find(postId);
        if (questionIt != questions.end()) {
            return dynamic_pointer_cast<Post>(questionIt->second);
        }
        
        auto answerIt = answers.find(postId);
        if (answerIt != answers.end()) {
            return dynamic_pointer_cast<Post>(answerIt->second);
        }

        throw runtime_error("Post not found");
    }
};





























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































