




using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Linq;
using System.Threading;

public class StackOverflowDemo
{
    public static void Main(string[] args)
    {
        StackOverflowService service = new StackOverflowService();

        // 1. Create Users
        User alice = service.CreateUser("Alice");
        User bob = service.CreateUser("Bob");
        User charlie = service.CreateUser("Charlie");

        // 2. Alice posts a question
        Console.WriteLine("--- Alice posts a question ---");
        Tag javaTag = new Tag("java");
        Tag designPatternsTag = new Tag("design-patterns");
        HashSet<Tag> tags = new HashSet<Tag> { javaTag, designPatternsTag };
        Question question = service.PostQuestion(alice.GetId(), "How to implement Observer Pattern?", "Details about Observer Pattern...", tags);
        PrintReputations(alice, bob, charlie);

        // 3. Bob and Charlie post answers
        Console.WriteLine("\n--- Bob and Charlie post answers ---");
        Answer bobAnswer = service.PostAnswer(bob.GetId(), question.GetId(), "You can use the java.util.Observer interface.");
        Answer charlieAnswer = service.PostAnswer(charlie.GetId(), question.GetId(), "A better way is to create your own Observer interface.");
        PrintReputations(alice, bob, charlie);

        // 4. Voting happens
        Console.WriteLine("\n--- Voting Occurs ---");
        service.VoteOnPost(alice.GetId(), question.GetId(), VoteType.UPVOTE);
        service.VoteOnPost(bob.GetId(), charlieAnswer.GetId(), VoteType.UPVOTE);
        service.VoteOnPost(alice.GetId(), bobAnswer.GetId(), VoteType.DOWNVOTE);
        PrintReputations(alice, bob, charlie);

        // 5. Alice accepts Charlie's answer
        Console.WriteLine("\n--- Alice accepts Charlie's answer ---");
        service.AcceptAnswer(question.GetId(), charlieAnswer.GetId());
        PrintReputations(alice, bob, charlie);

        // 6. Search for questions
        Console.WriteLine("\n--- (C) Combined Search: Questions by 'Alice' with tag 'java' ---");
        List<ISearchStrategy> filtersC = new List<ISearchStrategy>
        {
            new UserSearchStrategy(alice),
            new TagSearchStrategy(javaTag)
        };
        List<Question> searchResults = service.SearchQuestions(filtersC);
        foreach (var q in searchResults)
        {
            Console.WriteLine($"  - Found: {q.GetTitle()}");
        }
    }

    private static void PrintReputations(params User[] users)
    {
        Console.WriteLine("--- Current Reputations ---");
        foreach (User user in users)
        {
            Console.WriteLine($"{user.GetName()}: {user.GetReputation()}");
        }
    }
}






class Comment : Content
{
    public Comment(string body, User author)
        : base(Guid.NewGuid().ToString(), body, author)
    {
    }
}







abstract class Content
{
    protected readonly string id;
    protected readonly string body;
    protected readonly User author;
    protected readonly DateTime creationTime;

    public Content(string id, string body, User author)
    {
        this.id = id;
        this.body = body;
        this.author = author;
        this.creationTime = DateTime.Now;
    }

    public string GetId() { return id; }
    public string GetBody() { return body; }
    public User GetAuthor() { return author; }
}





abstract class Post : Content
{
    private int voteCount = 0;
    private readonly ConcurrentDictionary<string, VoteType> voters = new ConcurrentDictionary<string, VoteType>();
    private readonly List<Comment> comments = new List<Comment>();
    private readonly List<IPostObserver> observers = new List<IPostObserver>();
    private readonly object postLock = new object();

    public Post(string id, string body, User author) : base(id, body, author)
    {
    }

    public void AddObserver(IPostObserver observer)
    {
        observers.Add(observer);
    }

    protected void NotifyObservers(Event eventObj)
    {
        foreach (var observer in observers)
        {
            observer.OnPostEvent(eventObj);
        }
    }

    public void Vote(User user, VoteType voteType)
    {
        lock (postLock)
        {
            string userId = user.GetId();
            if (voters.TryGetValue(userId, out VoteType existingVote) && existingVote == voteType)
                return; // Already voted

            int scoreChange = 0;
            if (voters.ContainsKey(userId)) // User is changing their vote
            {
                scoreChange = (voteType == VoteType.UPVOTE) ? 2 : -2;
            }
            else // New vote
            {
                scoreChange = (voteType == VoteType.UPVOTE) ? 1 : -1;
            }

            voters[userId] = voteType;
            voteCount += scoreChange;

            EventType eventType;
            if (this is Question)
            {
                eventType = (voteType == VoteType.UPVOTE) ? EventType.UPVOTE_QUESTION : EventType.DOWNVOTE_QUESTION;
            }
            else
            {
                eventType = (voteType == VoteType.UPVOTE) ? EventType.UPVOTE_ANSWER : EventType.DOWNVOTE_ANSWER;
            }

            NotifyObservers(new Event(eventType, user, this));
        }
    }
}











class Question : Post
{
    private readonly string title;
    private readonly HashSet<Tag> tags;
    private readonly List<Answer> answers = new List<Answer>();
    private Answer acceptedAnswer;

    public Question(string title, string body, User author, HashSet<Tag> tags)
        : base(Guid.NewGuid().ToString(), body, author)
    {
        this.title = title;
        this.tags = tags;
    }

    public void AddAnswer(Answer answer)
    {
        answers.Add(answer);
    }

    public void AcceptAnswer(Answer answer)
    {
        lock (this)
        {
            if (!author.GetId().Equals(answer.GetAuthor().GetId()) && acceptedAnswer == null)
            {
                acceptedAnswer = answer;
                answer.SetAccepted(true);
                NotifyObservers(new Event(EventType.ACCEPT_ANSWER, answer.GetAuthor(), answer));
            }
        }
    }

    public string GetTitle() { return title; }
    public HashSet<Tag> GetTags() { return tags; }
    public List<Answer> GetAnswers() { return answers; }
}








class Tag : IComparable<Tag>
{
    private readonly string name;

    public Tag(string name)
    {
        this.name = name;
    }

    public string GetName() { return name; }

    public int CompareTo(Tag other)
    {
        return string.Compare(name, other.name, StringComparison.Ordinal);
    }

    public override bool Equals(object obj)
    {
        if (obj is Tag other)
        {
            return name.Equals(other.name, StringComparison.OrdinalIgnoreCase);
        }
        return false;
    }

    public override int GetHashCode()
    {
        return name.ToLower().GetHashCode();
    }
}












class User
{
    private readonly string id;
    private readonly string name;
    private int reputation;
    private readonly object reputationLock = new object();

    public User(string name)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.reputation = 0;
    }

    public void UpdateReputation(int change)
    {
        lock (reputationLock)
        {
            reputation += change;
        }
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
    public int GetReputation() 
    { 
        lock (reputationLock)
        {
            return reputation;
        }
    }
}












enum EventType
{
    UPVOTE_QUESTION,
    DOWNVOTE_QUESTION,
    UPVOTE_ANSWER,
    DOWNVOTE_ANSWER,
    ACCEPT_ANSWER
}






enum VoteType
{
    UPVOTE,
    DOWNVOTE
}















class Event
{
    private readonly EventType type;
    private readonly User actor;
    private readonly Post targetPost;

    public Event(EventType type, User actor, Post targetPost)
    {
        this.type = type;
        this.actor = actor;
        this.targetPost = targetPost;
    }

    public EventType GetEventType() { return type; }
    public User GetActor() { return actor; }
    public Post GetTargetPost() { return targetPost; }
}






interface IPostObserver
{
    void OnPostEvent(Event eventObj);
}






class ReputationManager : IPostObserver
{
    private const int QUESTION_UPVOTE_REP = 5;
    private const int ANSWER_UPVOTE_REP = 10;
    private const int ACCEPTED_ANSWER_REP = 15;
    private const int DOWNVOTE_REP_PENALTY = -1;
    private const int POST_DOWNVOTED_REP_PENALTY = -2;

    public void OnPostEvent(Event eventObj)
    {
        User postAuthor = eventObj.GetTargetPost().GetAuthor();

        switch (eventObj.GetEventType())
        {
            case EventType.UPVOTE_QUESTION:
                postAuthor.UpdateReputation(QUESTION_UPVOTE_REP);
                break;
            case EventType.DOWNVOTE_QUESTION:
                postAuthor.UpdateReputation(DOWNVOTE_REP_PENALTY);
                eventObj.GetActor().UpdateReputation(POST_DOWNVOTED_REP_PENALTY);
                break;
            case EventType.UPVOTE_ANSWER:
                postAuthor.UpdateReputation(ANSWER_UPVOTE_REP);
                break;
            case EventType.DOWNVOTE_ANSWER:
                postAuthor.UpdateReputation(DOWNVOTE_REP_PENALTY);
                eventObj.GetActor().UpdateReputation(POST_DOWNVOTED_REP_PENALTY);
                break;
            case EventType.ACCEPT_ANSWER:
                postAuthor.UpdateReputation(ACCEPTED_ANSWER_REP);
                break;
        }
    }
}







interface ISearchStrategy
{
    List<Question> Filter(List<Question> questions);
}






class KeywordSearchStrategy : ISearchStrategy
{
    private readonly string keyword;

    public KeywordSearchStrategy(string keyword)
    {
        this.keyword = keyword.ToLower();
    }

    public List<Question> Filter(List<Question> questions)
    {
        return questions
            .Where(q => q.GetTitle().ToLower().Contains(keyword) || q.GetBody().ToLower().Contains(keyword))
            .ToList();
    }
}







class TagSearchStrategy : ISearchStrategy
{
    private readonly Tag tag;

    public TagSearchStrategy(Tag tag)
    {
        this.tag = tag;
    }

    public List<Question> Filter(List<Question> questions)
    {
        return questions
            .Where(q => q.GetTags().Any(t => t.GetName().Equals(tag.GetName(), StringComparison.OrdinalIgnoreCase)))
            .ToList();
    }
}








class UserSearchStrategy : ISearchStrategy
{
    private readonly User user;

    public UserSearchStrategy(User user)
    {
        this.user = user;
    }

    public List<Question> Filter(List<Question> questions)
    {
        return questions
            .Where(q => q.GetAuthor().GetId().Equals(user.GetId()))
            .ToList();
    }
}










using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Linq;
using System.Threading;

public class StackOverflowDemo
{
    public static void Main(string[] args)
    {
        StackOverflowService service = new StackOverflowService();

        // 1. Create Users
        User alice = service.CreateUser("Alice");
        User bob = service.CreateUser("Bob");
        User charlie = service.CreateUser("Charlie");

        // 2. Alice posts a question
        Console.WriteLine("--- Alice posts a question ---");
        Tag javaTag = new Tag("java");
        Tag designPatternsTag = new Tag("design-patterns");
        HashSet<Tag> tags = new HashSet<Tag> { javaTag, designPatternsTag };
        Question question = service.PostQuestion(alice.GetId(), "How to implement Observer Pattern?", "Details about Observer Pattern...", tags);
        PrintReputations(alice, bob, charlie);

        // 3. Bob and Charlie post answers
        Console.WriteLine("\n--- Bob and Charlie post answers ---");
        Answer bobAnswer = service.PostAnswer(bob.GetId(), question.GetId(), "You can use the java.util.Observer interface.");
        Answer charlieAnswer = service.PostAnswer(charlie.GetId(), question.GetId(), "A better way is to create your own Observer interface.");
        PrintReputations(alice, bob, charlie);

        // 4. Voting happens
        Console.WriteLine("\n--- Voting Occurs ---");
        service.VoteOnPost(alice.GetId(), question.GetId(), VoteType.UPVOTE);
        service.VoteOnPost(bob.GetId(), charlieAnswer.GetId(), VoteType.UPVOTE);
        service.VoteOnPost(alice.GetId(), bobAnswer.GetId(), VoteType.DOWNVOTE);
        PrintReputations(alice, bob, charlie);

        // 5. Alice accepts Charlie's answer
        Console.WriteLine("\n--- Alice accepts Charlie's answer ---");
        service.AcceptAnswer(question.GetId(), charlieAnswer.GetId());
        PrintReputations(alice, bob, charlie);

        // 6. Search for questions
        Console.WriteLine("\n--- (C) Combined Search: Questions by 'Alice' with tag 'java' ---");
        List<ISearchStrategy> filtersC = new List<ISearchStrategy>
        {
            new UserSearchStrategy(alice),
            new TagSearchStrategy(javaTag)
        };
        List<Question> searchResults = service.SearchQuestions(filtersC);
        foreach (var q in searchResults)
        {
            Console.WriteLine($"  - Found: {q.GetTitle()}");
        }
    }

    private static void PrintReputations(params User[] users)
    {
        Console.WriteLine("--- Current Reputations ---");
        foreach (User user in users)
        {
            Console.WriteLine($"{user.GetName()}: {user.GetReputation()}");
        }
    }
}








class StackOverflowService
{
    private readonly ConcurrentDictionary<string, User> users = new ConcurrentDictionary<string, User>();
    private readonly ConcurrentDictionary<string, Question> questions = new ConcurrentDictionary<string, Question>();
    private readonly ConcurrentDictionary<string, Answer> answers = new ConcurrentDictionary<string, Answer>();
    private readonly IPostObserver reputationManager = new ReputationManager();

    public User CreateUser(string name)
    {
        User user = new User(name);
        users.TryAdd(user.GetId(), user);
        return user;
    }

    public Question PostQuestion(string userId, string title, string body, HashSet<Tag> tags)
    {
        User author = users[userId];
        Question question = new Question(title, body, author, tags);
        question.AddObserver(reputationManager);
        questions.TryAdd(question.GetId(), question);
        return question;
    }

    public Answer PostAnswer(string userId, string questionId, string body)
    {
        User author = users[userId];
        Question question = questions[questionId];
        Answer answer = new Answer(body, author);
        answer.AddObserver(reputationManager);
        question.AddAnswer(answer);
        answers.TryAdd(answer.GetId(), answer);
        return answer;
    }

    public void VoteOnPost(string userId, string postId, VoteType voteType)
    {
        User user = users[userId];
        Post post = FindPostById(postId);
        post.Vote(user, voteType);
    }

    public void AcceptAnswer(string questionId, string answerId)
    {
        Question question = questions[questionId];
        Answer answer = answers[answerId];
        question.AcceptAnswer(answer);
    }

    public List<Question> SearchQuestions(List<ISearchStrategy> strategies)
    {
        List<Question> results = questions.Values.ToList();

        foreach (var strategy in strategies)
        {
            results = strategy.Filter(results);
        }

        return results;
    }

    public User GetUser(string userId)
    {
        return users[userId];
    }

    private Post FindPostById(string postId)
    {
        if (questions.TryGetValue(postId, out Question question))
        {
            return question;
        }
        else if (answers.TryGetValue(postId, out Answer answer))
        {
            return answer;
        }

        throw new KeyNotFoundException("Post not found");
    }
}


































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































