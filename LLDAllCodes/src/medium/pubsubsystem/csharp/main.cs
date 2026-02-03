
class Message
{
    private readonly string payload;
    private readonly DateTime timestamp;

    public Message(string payload)
    {
        this.payload = payload;
        this.timestamp = DateTime.Now;
    }

    public string GetPayload()
    {
        return payload;
    }

    public override string ToString()
    {
        return $"Message{{payload='{payload}'}}";
    }
}










class Topic
{
    private readonly string name;
    private readonly HashSet<ISubscriber> subscribers;
    private readonly object subscribersLock = new object();

    public Topic(string name)
    {
        this.name = name;
        this.subscribers = new HashSet<ISubscriber>();
    }

    public string GetName()
    {
        return name;
    }

    public void AddSubscriber(ISubscriber subscriber)
    {
        lock (subscribersLock)
        {
            subscribers.Add(subscriber);
        }
    }

    public void RemoveSubscriber(ISubscriber subscriber)
    {
        lock (subscribersLock)
        {
            subscribers.Remove(subscriber);
        }
    }

    public void Broadcast(Message message)
    {
        List<ISubscriber> currentSubscribers;
        lock (subscribersLock)
        {
            currentSubscribers = new List<ISubscriber>(subscribers);
        }

        List<Task> deliveryTasks = new List<Task>();

        foreach (ISubscriber subscriber in currentSubscribers)
        {
            deliveryTasks.Add(Task.Run(() =>
            {
                try
                {
                    subscriber.OnMessage(message);
                }
                catch (Exception e)
                {
                    Console.Error.WriteLine($"Error delivering message to subscriber {subscriber.GetId()}: {e.Message}");
                }
            }));
        }

        Task.WaitAll(deliveryTasks.ToArray());
    }
}











class AlertSubscriber : ISubscriber
{
    private readonly string id;

    public AlertSubscriber(string id)
    {
        this.id = id;
    }

    public string GetId()
    {
        return id;
    }

    public void OnMessage(Message message)
    {
        Console.WriteLine($"!!! [ALERT - {id}] : '{message.GetPayload()}' !!!");
    }
}




interface ISubscriber
{
    string GetId();
    void OnMessage(Message message);
}




class NewsSubscriber : ISubscriber
{
    private readonly string id;

    public NewsSubscriber(string id)
    {
        this.id = id;
    }

    public string GetId()
    {
        return id;
    }

    public void OnMessage(Message message)
    {
        Console.WriteLine($"[Subscriber {id}] received message '{message.GetPayload()}'");
    }
}






using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Threading;
using System.Threading.Tasks;

public class PubSubDemo
{
    public static void Main(string[] args)
    {
        PubSubService pubSubService = PubSubService.GetInstance();

        // --- Create Subscribers ---
        ISubscriber sportsFan1 = new NewsSubscriber("SportsFan1");
        ISubscriber sportsFan2 = new NewsSubscriber("SportsFan2");
        ISubscriber techie1 = new NewsSubscriber("Techie1");
        ISubscriber allNewsReader = new NewsSubscriber("AllNewsReader");
        ISubscriber systemAdmin = new AlertSubscriber("SystemAdmin");

        // --- Create Topics and Subscriptions ---
        const string SPORTS_TOPIC = "SPORTS";
        const string TECH_TOPIC = "TECH";
        const string WEATHER_TOPIC = "WEATHER";

        pubSubService.CreateTopic(SPORTS_TOPIC);
        pubSubService.CreateTopic(TECH_TOPIC);
        pubSubService.CreateTopic(WEATHER_TOPIC);

        pubSubService.Subscribe(SPORTS_TOPIC, sportsFan1);
        pubSubService.Subscribe(SPORTS_TOPIC, sportsFan2);
        pubSubService.Subscribe(SPORTS_TOPIC, allNewsReader);
        pubSubService.Subscribe(SPORTS_TOPIC, systemAdmin);

        pubSubService.Subscribe(TECH_TOPIC, techie1);
        pubSubService.Subscribe(TECH_TOPIC, allNewsReader);

        Console.WriteLine("\n--- Publishing Messages ---");

        // --- Publish to SPORTS topic ---
        pubSubService.Publish(SPORTS_TOPIC, new Message("Team A wins the championship!"));
        // Expected: SportsFan1, SportsFan2, AllNewsReader, SystemAdmin receive this.

        // --- Publish to TECH topic ---
        pubSubService.Publish(TECH_TOPIC, new Message("New AI model released."));
        // Expected: Techie1, AllNewsReader receive this.

        // --- Publish to WEATHER topic (no subscribers) ---
        pubSubService.Publish(WEATHER_TOPIC, new Message("Sunny with a high of 75°F."));
        // Expected: Message is dropped.

        // Allow some time for async messages to be processed
        Thread.Sleep(500);

        Console.WriteLine("\n--- Unsubscribing a user and re-publishing ---");

        // SportsFan2 gets tired of sports news
        pubSubService.Unsubscribe(SPORTS_TOPIC, sportsFan2);

        // Publish another message to SPORTS
        pubSubService.Publish(SPORTS_TOPIC, new Message("Major player traded to Team B."));
        // Expected: SportsFan1, AllNewsReader, SystemAdmin receive this. SportsFan2 does NOT.

        // Give messages time to be delivered
        Thread.Sleep(500);

        // --- Shutdown the service ---
        pubSubService.Shutdown();
    }
}














class PubSubService
{
    private static PubSubService instance;
    private static readonly object instanceLock = new object();

    private readonly ConcurrentDictionary<string, Topic> topicRegistry;

    private PubSubService()
    {
        topicRegistry = new ConcurrentDictionary<string, Topic>();
    }

    public static PubSubService GetInstance()
    {
        if (instance == null)
        {
            lock (instanceLock)
            {
                if (instance == null)
                {
                    instance = new PubSubService();
                }
            }
        }
        return instance;
    }

    public void CreateTopic(string topicName)
    {
        topicRegistry.TryAdd(topicName, new Topic(topicName));
        Console.WriteLine($"Topic {topicName} created");
    }

    public void Subscribe(string topicName, ISubscriber subscriber)
    {
        if (!topicRegistry.TryGetValue(topicName, out Topic topic))
        {
            throw new ArgumentException($"Topic not found: {topicName}");
        }
        topic.AddSubscriber(subscriber);
        Console.WriteLine($"Subscriber '{subscriber.GetId()}' subscribed to topic: {topicName}");
    }

    public void Unsubscribe(string topicName, ISubscriber subscriber)
    {
        if (topicRegistry.TryGetValue(topicName, out Topic topic))
        {
            topic.RemoveSubscriber(subscriber);
        }
        Console.WriteLine($"Subscriber '{subscriber.GetId()}' unsubscribed from topic: {topicName}");
    }

    public void Publish(string topicName, Message message)
    {
        Console.WriteLine($"Publishing message to topic: {topicName}");
        if (!topicRegistry.TryGetValue(topicName, out Topic topic))
        {
            throw new ArgumentException($"Topic not found: {topicName}");
        }
        topic.Broadcast(message);
    }

    public void Shutdown()
    {
        Console.WriteLine("PubSubService shutting down...");
        Console.WriteLine("PubSubService shutdown complete.");
    }
}












































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































