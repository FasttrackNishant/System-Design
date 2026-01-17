package medium.pubsubsystem;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/* ===================== MESSAGE ===================== */

class Message {
    private final String payload;
    private final Instant timestamp;

    public Message(String payload) {
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public String getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

/* ===================== SUBSCRIBER ===================== */

interface Subscriber {
    void onMessage(Message message);
    String getId();
}

/* ===================== SAMPLE SUBSCRIBER ===================== */

class AlertSubscriber implements Subscriber {

    private final String id;

    public AlertSubscriber() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void onMessage(Message message) {
        System.out.println(
                "Alert for Subscriber " + id + " -> " + message.getPayload()
        );
    }

    @Override
    public String getId() {
        return id;
    }
}

class LoggingSubscriber implements Subscriber {

    private final String id;

    public LoggingSubscriber() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void onMessage(Message message) {
        System.out.println(
                "[LOG] Subscriber " + id + " received message: " + message.getPayload()
        );
    }

    @Override
    public String getId() {
        return id;
    }
}

/* ===================== TOPIC ===================== */

class Topic {

    private final String name;
    private final Set<Subscriber> subscribers;

    public Topic(String name) {
        this.name = name;
        this.subscribers = new CopyOnWriteArraySet<>();
    }

    public String getName() {
        return name;
    }

    // Idempotent subscription
    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void removeSubscriber(Subscriber subscriber) {
        subscribers.remove(subscriber);
    }

    // Simple synchronous broadcast
    public void broadcast(Message message) {
        for (Subscriber subscriber : subscribers) {
            subscriber.onMessage(message);
        }
    }
}

/* ===================== PUB-SUB SERVICE ===================== */

class PubSubService {

    private final Map<String, Topic> topicRegistry;

    public PubSubService() {
        this.topicRegistry = new ConcurrentHashMap<>();
    }

    public void createTopic(String topicName) {
        topicRegistry.putIfAbsent(topicName, new Topic(topicName));
        System.out.println("Topic created: " + topicName);
    }

    public void subscribe(String topicName, Subscriber subscriber) {
        Topic topic = topicRegistry.get(topicName);
        if (topic == null) {
            throw new IllegalArgumentException("Topic not found: " + topicName);
        }
        topic.addSubscriber(subscriber);
        System.out.println(
                "Subscriber " + subscriber.getId() + " subscribed to " + topicName
        );
    }

    public void unsubscribe(String topicName, Subscriber subscriber) {
        Topic topic = topicRegistry.get(topicName);
        if (topic != null) {
            topic.removeSubscriber(subscriber);
        }
    }

    public void publish(String topicName, Message message) {
        Topic topic = topicRegistry.get(topicName);
        if (topic == null) {
            throw new IllegalArgumentException("Topic not found: " + topicName);
        }
        topic.broadcast(message);
    }
}

/* ===================== DRIVER ===================== */

class Main {

    public static void main(String[] args) {

        PubSubService service = new PubSubService();
        service.createTopic("Business");

        Subscriber alertSub = new AlertSubscriber();
        Subscriber logSub = new LoggingSubscriber();

        service.subscribe("Business", alertSub);
        service.subscribe("Business", logSub);

        Message message = new Message("Quarterly revenue increased");
        service.publish("Business", message);
    }
}
