class Comment(CommentableEntity):
    def __init__(self, author: 'User', content: str):
        super().__init__(author, content)

    def get_replies(self) -> List['Comment']:
        return self.get_comments()







class CommentableEntity:
    def __init__(self, author: 'User', content: str):
        self.id = str(uuid.uuid4())
        self.author = author
        self.content = content
        self.timestamp = datetime.now()
        self.likes: Set['User'] = set()
        self.comments: List['Comment'] = []

    def add_like(self, user: 'User'):
        self.likes.add(user)

    def add_comment(self, comment: 'Comment'):
        self.comments.append(comment)

    def get_id(self) -> str:
        return self.id

    def get_author(self) -> 'User':
        return self.author

    def get_content(self) -> str:
        return self.content

    def get_timestamp(self) -> datetime:
        return self.timestamp

    def get_comments(self) -> List['Comment']:
        return self.comments

    def get_likes(self) -> Set['User']:
        return self.likes







class Post(CommentableEntity):
    def __init__(self, author: 'User', content: str):
        super().__init__(author, content)





class User:
    def __init__(self, name: str, email: str):
        self.id = str(uuid.uuid4())
        self.name = name
        self.email = email
        self.friends: Set['User'] = set()
        self.posts: List[Post] = []

    def add_friend(self, friend: 'User'):
        self.friends.add(friend)

    def add_post(self, post: Post):
        self.posts.append(post)

    def get_id(self) -> str:
        return self.id

    def get_name(self) -> str:
        return self.name

    def get_friends(self) -> Set['User']:
        return self.friends

    def get_posts(self) -> List[Post]:
        return self.posts








class PostObserver(ABC):
    @abstractmethod
    def on_post_created(self, post: Post):
        pass

    @abstractmethod
    def on_like(self, post: Post, user: User):
        pass

    @abstractmethod
    def on_comment(self, post: Post, comment: Comment):
        pass






class UserNotifier(PostObserver):
    def on_post_created(self, post: Post):
        author = post.get_author()
        for friend in author.get_friends():
            print(f"Notification for {friend.get_name()}: {author.get_name()} created a new post: {post.get_content()}")

    def on_like(self, post: Post, user: User):
        author = post.get_author()
        print(f"Notification for {author.get_name()}: {user.get_name()} liked your post")

    def on_comment(self, post: Post, comment: Comment):
        author = post.get_author()
        print(f"Notification for {author.get_name()}: {comment.get_author().get_name()} commented on your post")









class PostRepository:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance.posts = {}
        return cls._instance

    @classmethod
    def get_instance(cls):
        return cls()

    def save(self, post: Post):
        self.posts[post.get_id()] = post

    def find_by_id(self, post_id: str) -> Optional[Post]:
        return self.posts.get(post_id)







class UserRepository:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance.users = {}
        return cls._instance

    @classmethod
    def get_instance(cls):
        return cls()

    def save(self, user: User):
        self.users[user.get_id()] = user

    def find_by_id(self, user_id: str) -> Optional[User]:
        return self.users.get(user_id)

class PostRepository:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance.posts = {}
        return cls._instance

    @classmethod
    def get_instance(cls):
        return cls()

    def save(self, post: Post):
        self.posts[post.get_id()] = post

    def find_by_id(self, post_id: str) -> Optional[Post]:
        return self.posts.get(post_id)






class NewsFeedService:
    def __init__(self):
        self.strategy = ChronologicalStrategy()  # Default strategy

    def set_strategy(self, strategy: NewsFeedGenerationStrategy):
        self.strategy = strategy

    def get_news_feed(self, user: User) -> List[Post]:
        return self.strategy.generate_feed(user)



class PostService:
    def __init__(self):
        self.post_repository = PostRepository.get_instance()
        self.observers: List[PostObserver] = []

    def add_observer(self, observer: PostObserver):
        self.observers.append(observer)

    def create_post(self, author: User, content: str) -> Post:
        post = Post(author, content)
        self.post_repository.save(post)
        author.add_post(post)
        for observer in self.observers:
            observer.on_post_created(post)
        return post

    def like_post(self, user: User, post_id: str):
        post = self.post_repository.find_by_id(post_id)
        post.add_like(user)
        for observer in self.observers:
            observer.on_like(post, user)

    def add_comment(self, author: User, commentable_id: str, content: str):
        comment = Comment(author, content)
        post = self.post_repository.find_by_id(commentable_id)
        post.add_comment(comment)
        for observer in self.observers:
            observer.on_comment(post, comment)





class UserService:
    def __init__(self):
        self.user_repository = UserRepository.get_instance()

    def create_user(self, name: str, email: str) -> User:
        user = User(name, email)
        self.user_repository.save(user)
        return user

    def add_friend(self, user_id1: str, user_id2: str):
        user1 = self.user_repository.find_by_id(user_id1)
        user2 = self.user_repository.find_by_id(user_id2)

        user1.add_friend(user2)
        user2.add_friend(user1)

    def get_user_by_id(self, user_id: str) -> Optional[User]:
        return self.user_repository.find_by_id(user_id)








class ChronologicalStrategy(NewsFeedGenerationStrategy):
    def generate_feed(self, user: User) -> List[Post]:
        friends = user.get_friends()
        feed = []

        for friend in friends:
            feed.extend(friend.get_posts())

        # Sort posts by timestamp in reverse (most recent first)
        feed.sort(key=lambda p: p.get_timestamp(), reverse=True)

        return feed



class NewsFeedGenerationStrategy(ABC):
    @abstractmethod
    def generate_feed(self, user: User) -> List[Post]:
        pass








def print_feed(feed: List[Post]):
    if not feed:
        print("  No posts in the feed.")
        return
    
    for post in feed:
        print(f"  Post by {post.get_author().get_name()} at {post.get_timestamp()}")
        print(f"    \"{post.get_content()}\"")
        print(f"    Likes: {len(post.get_likes())}, Comments: {len(post.get_comments())}")

def main():
    social_network = SocialNetworkFacade()

    print("----------- 1. Creating Users -----------")
    alice = social_network.create_user("Alice", "alice@example.com")
    bob = social_network.create_user("Bob", "bob@example.com")
    charlie = social_network.create_user("Charlie", "charlie@example.com")
    print(f"Created users: {alice.get_name()}, {bob.get_name()}, {charlie.get_name()}")

    print("\n----------- 2. Building Friendships -----------")
    social_network.add_friend(alice.get_id(), bob.get_id())
    social_network.add_friend(bob.get_id(), charlie.get_id())
    print(f"{alice.get_name()} and {bob.get_name()} are now friends.")
    print(f"{bob.get_name()} and {charlie.get_name()} are now friends.")

    print("\n----------- 3. Users Create Posts -----------")
    alice_post = social_network.create_post(alice.get_id(), "Hello from Alice!")
    bob_post = social_network.create_post(bob.get_id(), "It's a beautiful day!")
    charlie_post = social_network.create_post(charlie.get_id(), "Thinking about design patterns.")

    print("\n----------- 4. Users Interact with Posts -----------")
    social_network.add_comment(bob.get_id(), alice_post.get_id(), "Hey Alice, nice to see you here!")
    social_network.like_post(charlie.get_id(), alice_post.get_id())

    print("\n----------- 5. Viewing News Feeds (Strategy Pattern) -----------")

    print("\n--- Alice's News Feed (should see Bob's post) ---")
    alices_feed = social_network.get_news_feed(alice.get_id())
    print_feed(alices_feed)

    print("\n--- Bob's News Feed (should see Alice's, and Charlie's post) ---")
    bobs_feed = social_network.get_news_feed(bob.get_id())
    print_feed(bobs_feed)

    print("\n--- Charlie's News Feed (should see Bob's post) ---")
    charlies_feed = social_network.get_news_feed(charlie.get_id())
    print_feed(charlies_feed)

if __name__ == "__main__":
    main()







class SocialNetworkFacade:
    def __init__(self):
        self.user_service = UserService()
        self.post_service = PostService()
        self.news_feed_service = NewsFeedService()
        # Wire up the observer
        self.post_service.add_observer(UserNotifier())

    def create_user(self, name: str, email: str) -> User:
        return self.user_service.create_user(name, email)

    def add_friend(self, user_id1: str, user_id2: str):
        self.user_service.add_friend(user_id1, user_id2)

    def create_post(self, author_id: str, content: str) -> Post:
        author = self.user_service.get_user_by_id(author_id)
        return self.post_service.create_post(author, content)

    def add_comment(self, user_id: str, post_id: str, content: str):
        user = self.user_service.get_user_by_id(user_id)
        self.post_service.add_comment(user, post_id, content)

    def like_post(self, user_id: str, post_id: str):
        user = self.user_service.get_user_by_id(user_id)
        self.post_service.like_post(user, post_id)

    def get_news_feed(self, user_id: str) -> List[Post]:
        user = self.user_service.get_user_by_id(user_id)
        return self.news_feed_service.get_news_feed(user)





























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































