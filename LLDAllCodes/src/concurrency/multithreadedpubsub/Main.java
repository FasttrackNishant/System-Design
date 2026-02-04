package concurrency.multithreadedpubsub;




public enum MessagePriority {
    LOW, NORMAL, HIGH
}

public enum SubscriberState {
    ACTIVE,      // Actively consuming messages
    PAUSED,      // Temporarily stopped
    TERMINATED   // Permanently stopped, cleanup pending
}








public class Message {
    private final String id;
    private final String topic;
    private final Object payload;
    private final long timestamp;
    private final MessagePriority priority;

    public Message(String topic, Object payload) {
        this(topic, payload, MessagePriority.NORMAL);
    }

    public Message(String topic, Object payload, MessagePriority priority) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
    }

    // Getters
    public String getId() { return id; }
    public String getTopic() { return topic; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public MessagePriority getPriority() { return priority; }

    @Override
    public String toString() {
        return String.format("Message[id=%s, topic=%s, payload=%s]", id, topic, payload);
    }
}







public interface Subscriber {
    String getId();
    void onMessage(Message message);
    void shutdown();
}

public class AsyncSubscriber implements Subscriber {
    private final String id;
    private final BlockingQueue<Message> messageQueue;
    private final Consumer<Message> messageHandler;
    private final Thread consumerThread;
    // Step: Use volatile for cross-thread visibility of state changes
    // Thread-safety: volatile ensures TERMINATED is visible to consumer thread immediately
    private volatile SubscriberState state;

    public AsyncSubscriber(String id, Consumer<Message> messageHandler, int queueCapacity) {
        this.id = id;
        // Step: Create bounded queue for backpressure control
        // Why: Prevents unbounded memory growth when consumer is slower than producer
        this.messageQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.messageHandler = messageHandler;
        this.state = SubscriberState.ACTIVE;

        // Step: Start dedicated consumer thread with descriptive name
        // Why: Thread isolation means slow handlers don't affect other subscribers
        this.consumerThread = new Thread(this::consumeLoop, "subscriber-" + id);
        this.consumerThread.setDaemon(true);  // Won't prevent JVM shutdown
        this.consumerThread.start();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void onMessage(Message message) {
        // Step: Early exit if not accepting messages
        // Thread-safety: volatile read of state is atomic
        if (state != SubscriberState.ACTIVE) {
            return;
        }

        // Step: Non-blocking offer with timeout for backpressure
        // Why: Publisher doesn't block forever; returns quickly even if queue is full
        // Thread-safety: BlockingQueue.offer is thread-safe
        boolean accepted = false;
        try {
            accepted = messageQueue.offer(message, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!accepted) {
            // Backpressure policy: log and drop
            System.err.println("Subscriber " + id + " queue full, dropping message: " + message.getId());
        }
    }

    private void consumeLoop() {
        // Step: Loop until explicitly terminated
        // Thread-safety: volatile read sees writes from shutdown()
        while (state != SubscriberState.TERMINATED) {
            try {
                // Step: Poll with timeout instead of blocking take()
                // Why: Allows checking state flag every 100ms for graceful shutdown
                Message message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message != null && state == SubscriberState.ACTIVE) {
                    try {
                        // Step: Invoke user-provided handler
                        // Why: try-catch prevents one bad message from killing the consumer
                        messageHandler.accept(message);
                    } catch (Exception e) {
                        System.err.println("Error handling message: " + e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                // Step: Respect interruption for shutdown
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        // Step: Signal consumer thread to stop
        // Thread-safety: volatile write is immediately visible to consumer
        state = SubscriberState.TERMINATED;
        // Step: Interrupt in case consumer is blocked in poll()
        consumerThread.interrupt();
        try {
            // Step: Wait for consumer thread to finish (with timeout)
            // Why: Ensures cleanup completes before shutdown returns
            consumerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getQueueSize() {
        return messageQueue.size();
    }
}






public class Topic {
    private final String name;
    // Step: Use CopyOnWriteArrayList for lock-free iteration during publish
    // Why: Reads (publishes) are frequent; writes (subscribe/unsubscribe) are rare
    // Thread-safety: COW creates snapshot on write; iteration never sees concurrent modification
    private final CopyOnWriteArrayList<Subscriber> subscribers;
    // Step: Use volatile for cross-thread visibility of active flag
    // Thread-safety: Publishers see deactivation immediately without locking
    private volatile boolean active;

    public Topic(String name) {
        this.name = name;
        this.subscribers = new CopyOnWriteArrayList<>();
        this.active = true;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void addSubscriber(Subscriber subscriber) {
        // Step: Check active flag before adding
        // Why: Prevents adding subscribers to a topic being deleted
        if (active) {
            // Thread-safety: CopyOnWriteArrayList.add() is thread-safe
            subscribers.add(subscriber);
        }
    }

    public void removeSubscriber(Subscriber subscriber) {
        // Thread-safety: CopyOnWriteArrayList.remove() is thread-safe
        subscribers.remove(subscriber);
    }

    public void publish(Message message) {
        // Step: Fail fast if topic is inactive
        // Why: Clear error rather than silent message loss
        if (!active) {
            throw new IllegalStateException("Cannot publish to inactive topic: " + name);
        }

        // Step: Iterate subscriber list and deliver to each
        // Thread-safety: CopyOnWriteArrayList returns a snapshot array reference
        // Even if subscribers.add/remove happens during this loop, we iterate a consistent snapshot
        for (Subscriber subscriber : subscribers) {
            // Step: Non-blocking enqueue to subscriber's queue
            // Thread-safety: onMessage only enqueues; actual processing is async
            subscriber.onMessage(message);
        }
    }

    public int getSubscriberCount() {
        return subscribers.size();
    }

    public void deactivate() {
        // Step: Mark inactive first to stop new publishes
        // Thread-safety: volatile write is immediately visible
        active = false;
        // Step: Shutdown all subscriber consumer threads
        // Why: Prevents resource leaks (threads, memory)
        for (Subscriber subscriber : subscribers) {
            subscriber.shutdown();
        }
        // Step: Clear references for garbage collection
        subscribers.clear();
    }
}








public class MessageBroker {
    // Step: Use ConcurrentHashMap for thread-safe topic registry
    // Thread-safety: Provides atomic get-or-create operations, avoiding TOCTOU races
    private final ConcurrentHashMap<String, Topic> topics;
    private final int defaultQueueCapacity;

    public MessageBroker() {
        this(1000);
    }

    public MessageBroker(int defaultQueueCapacity) {
        this.topics = new ConcurrentHashMap<>();
        this.defaultQueueCapacity = defaultQueueCapacity;
    }

    public Topic createTopic(String topicName) {
        // Step: Atomic get-or-create to prevent duplicate Topic objects
        // Thread-safety: computeIfAbsent is atomic - only one Topic created per name
        // Why: Avoids the check-then-act race condition
        return topics.computeIfAbsent(topicName, Topic::new);
    }

    public void deleteTopic(String topicName) {
        // Step: Atomically remove from registry
        // Thread-safety: remove() returns the removed value atomically
        Topic topic = topics.remove(topicName);
        if (topic != null) {
            // Step: Clean up subscriber threads and resources
            // Why: Prevents resource leaks after topic deletion
            topic.deactivate();
        }
    }

    public Subscriber subscribe(String topicName, String subscriberId, Consumer<Message> handler) {
        // Step: Look up topic (lock-free read)
        // Thread-safety: ConcurrentHashMap.get() is thread-safe
        Topic topic = topics.get(topicName);
        if (topic == null || !topic.isActive()) {
            throw new IllegalArgumentException("Topic does not exist or is inactive: " + topicName);
        }

        // Step: Create subscriber with bounded queue for backpressure
        // Why: Each subscriber gets isolated queue and consumer thread
        AsyncSubscriber subscriber = new AsyncSubscriber(subscriberId, handler, defaultQueueCapacity);
        // Thread-safety: Topic.addSubscriber uses CopyOnWriteArrayList
        topic.addSubscriber(subscriber);
        return subscriber;
    }

    public void unsubscribe(String topicName, Subscriber subscriber) {
        Topic topic = topics.get(topicName);
        if (topic != null) {
            // Step: Remove from topic's subscriber list
            topic.removeSubscriber(subscriber);
            // Step: Stop subscriber's consumer thread
            // Why: Prevents resource leak from orphaned threads
            subscriber.shutdown();
        }
    }

    public void publish(String topicName, Object payload) {
        publish(topicName, payload, MessagePriority.NORMAL);
    }

    public void publish(String topicName, Object payload, MessagePriority priority) {
        // Step: Look up topic (lock-free read)
        // Thread-safety: get() is thread-safe; topic reference is safe to hold
        Topic topic = topics.get(topicName);
        if (topic == null) {
            throw new IllegalArgumentException("Topic does not exist: " + topicName);
        }

        // Step: Create immutable message and publish
        // Thread-safety: Message is immutable, safe to share across threads
        Message message = new Message(topicName, payload, priority);
        // Thread-safety: Topic.publish uses CopyOnWriteArrayList for lock-free iteration
        topic.publish(message);
    }

    public Set<String> getTopicNames() {
        // Thread-safety: keySet() returns a live view, thread-safe to iterate
        return topics.keySet();
    }

    public int getSubscriberCount(String topicName) {
        Topic topic = topics.get(topicName);
        return topic != null ? topic.getSubscriberCount() : 0;
    }

    public void shutdown() {
        // Step: Iterate all topics and deactivate each
        // Why: Graceful shutdown stops all consumer threads
        for (Topic topic : topics.values()) {
            topic.deactivate();
        }
        // Step: Clear registry for garbage collection
        topics.clear();
    }
}


















































































































