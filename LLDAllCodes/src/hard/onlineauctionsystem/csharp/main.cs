enum AuctionState
{
    PENDING,
    ACTIVE,
    CLOSED
}






class Auction
{
    private readonly string id;
    private readonly string itemName;
    private readonly string description;
    private readonly decimal startingPrice;
    private readonly DateTime endTime;
    private readonly List<Bid> bids;
    private readonly HashSet<IAuctionObserver> observers;
    private AuctionState state;
    private Bid winningBid;
    private readonly object lockObject = new object();

    public Auction(string itemName, string description, decimal startingPrice, DateTime endTime)
    {
        this.id = Guid.NewGuid().ToString();
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
        this.bids = new List<Bid>();
        this.observers = new HashSet<IAuctionObserver>();
        this.state = AuctionState.ACTIVE;
    }

    public void PlaceBid(User bidder, decimal amount)
    {
        lock (lockObject)
        {
            if (state != AuctionState.ACTIVE)
            {
                throw new InvalidOperationException("Auction is not active.");
            }
            if (DateTime.Now > endTime)
            {
                EndAuction();
                throw new InvalidOperationException("Auction has already ended.");
            }

            Bid highestBid = GetHighestBid();
            decimal currentMaxAmount = (highestBid == null) ? startingPrice : highestBid.GetAmount();

            if (amount <= currentMaxAmount)
            {
                throw new ArgumentException("Bid must be higher than the current highest bid.");
            }

            User previousHighestBidder = (highestBid != null) ? highestBid.GetBidder() : null;

            Bid newBid = new Bid(bidder, amount);
            bids.Add(newBid);
            AddObserver(bidder);

            Console.WriteLine($"SUCCESS: {bidder.GetName()} placed a bid of ${amount:F2} on '{itemName}'.");

            if (previousHighestBidder != null && !previousHighestBidder.Equals(bidder))
            {
                NotifyObserver(previousHighestBidder, $"You have been outbid on '{itemName}'! The new highest bid is ${amount:F2}.");
            }
        }
    }

    public void EndAuction()
    {
        lock (lockObject)
        {
            if (state != AuctionState.ACTIVE)
            {
                return;
            }

            state = AuctionState.CLOSED;
            winningBid = GetHighestBid();

            string endMessage;
            if (winningBid != null)
            {
                endMessage = $"Auction for '{itemName}' has ended. Winner is {winningBid.GetBidder().GetName()} with a bid of ${winningBid.GetAmount():F2}!";
            }
            else
            {
                endMessage = $"Auction for '{itemName}' has ended. There were no bids.";
            }

            Console.WriteLine($"\n{endMessage.ToUpper()}");
            NotifyAllObservers(endMessage);
        }
    }

    public Bid GetHighestBid()
    {
        if (bids.Count == 0)
        {
            return null;
        }
        return bids.Max();
    }

    public bool IsActive()
    {
        return state == AuctionState.ACTIVE;
    }

    private void AddObserver(IAuctionObserver observer)
    {
        observers.Add(observer);
    }

    private void NotifyAllObservers(string message)
    {
        foreach (IAuctionObserver observer in observers)
        {
            observer.OnUpdate(this, message);
        }
    }

    private void NotifyObserver(IAuctionObserver observer, string message)
    {
        observer.OnUpdate(this, message);
    }

    public string GetId() { return id; }
    public string GetItemName() { return itemName; }
    public List<Bid> GetBidHistory() { return new List<Bid>(bids); }
    public AuctionState GetState() { return state; }
    public Bid GetWinningBid() { return winningBid; }
}







class Bid : IComparable<Bid>
{
    private readonly User bidder;
    private readonly decimal amount;
    private readonly DateTime timestamp;

    public Bid(User bidder, decimal amount)
    {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = DateTime.Now;
    }

    public User GetBidder()
    {
        return bidder;
    }

    public decimal GetAmount()
    {
        return amount;
    }

    public DateTime GetTimestamp()
    {
        return timestamp;
    }

    public int CompareTo(Bid other)
    {
        int amountComparison = amount.CompareTo(other.amount);
        if (amountComparison != 0)
        {
            return amountComparison;
        }
        return other.timestamp.CompareTo(timestamp);
    }

    public override string ToString()
    {
        return $"Bidder: {bidder.GetName()}, Amount: {amount:F2}, Time: {timestamp}";
    }
}








class User : IAuctionObserver
{
    private readonly string id;
    private readonly string name;

    public User(string name)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
    }

    public string GetId()
    {
        return id;
    }

    public string GetName()
    {
        return name;
    }

    public void OnUpdate(Auction auction, string message)
    {
        Console.WriteLine($"--- Notification for {name} ---");
        Console.WriteLine($"Auction: {auction.GetItemName()}");
        Console.WriteLine($"Message: {message}");
        Console.WriteLine("---------------------------\n");
    }

    public override bool Equals(object obj)
    {
        if (this == obj) return true;
        if (obj == null || GetType() != obj.GetType()) return false;
        User user = (User)obj;
        return id.Equals(user.id);
    }

    public override int GetHashCode()
    {
        return id.GetHashCode();
    }
}








interface IAuctionObserver
{
    void OnUpdate(Auction auction, string message);
}









class AuctionService
{
    private static AuctionService instance;
    private static readonly object lockObject = new object();
    private readonly ConcurrentDictionary<string, User> users;
    private readonly ConcurrentDictionary<string, Auction> auctions;
    private readonly List<Task> scheduledTasks;
    private bool shutdown;

    private AuctionService()
    {
        users = new ConcurrentDictionary<string, User>();
        auctions = new ConcurrentDictionary<string, Auction>();
        scheduledTasks = new List<Task>();
        shutdown = false;
    }

    public static AuctionService GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new AuctionService();
                }
            }
        }
        return instance;
    }

    public User CreateUser(string name)
    {
        User user = new User(name);
        users.TryAdd(user.GetId(), user);
        return user;
    }

    public User GetUser(string userId)
    {
        users.TryGetValue(userId, out User user);
        return user;
    }

    public Auction CreateAuction(string itemName, string description, decimal startingPrice, DateTime endTime)
    {
        Auction auction = new Auction(itemName, description, startingPrice, endTime);
        auctions.TryAdd(auction.GetId(), auction);

        TimeSpan delay = endTime - DateTime.Now;
        if (delay.TotalMilliseconds > 0)
        {
            Task scheduledTask = Task.Run(async () =>
            {
                await Task.Delay(delay);
                if (!shutdown)
                {
                    EndAuction(auction.GetId());
                }
            });
            scheduledTasks.Add(scheduledTask);
        }

        Console.WriteLine($"New auction created for '{itemName}' (ID: {auction.GetId()}), ending at {endTime}.");
        return auction;
    }

    public List<Auction> ViewActiveAuctions()
    {
        return auctions.Values.Where(auction => auction.IsActive()).ToList();
    }

    public void PlaceBid(string auctionId, string bidderId, decimal amount)
    {
        Auction auction = GetAuction(auctionId);
        auction.PlaceBid(users[bidderId], amount);
    }

    public void EndAuction(string auctionId)
    {
        Auction auction = GetAuction(auctionId);
        auction.EndAuction();
    }

    public Auction GetAuction(string auctionId)
    {
        if (!auctions.TryGetValue(auctionId, out Auction auction))
        {
            throw new KeyNotFoundException($"Auction with ID {auctionId} not found.");
        }
        return auction;
    }

    public void Shutdown()
    {
        shutdown = true;
        Task.WaitAll(scheduledTasks.ToArray());
    }
}





using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

public class AuctionSystemDemo
{
    public static void Main(string[] args)
    {
        AuctionService auctionService = AuctionService.GetInstance();

        User alice = auctionService.CreateUser("Alice");
        User bob = auctionService.CreateUser("Bob");
        User carol = auctionService.CreateUser("Carol");

        Console.WriteLine("=============================================");
        Console.WriteLine("        Online Auction System Demo           ");
        Console.WriteLine("=============================================");

        DateTime endTime = DateTime.Now.AddSeconds(10);
        Auction laptopAuction = auctionService.CreateAuction(
            "Vintage Laptop",
            "A rare 1990s laptop, in working condition.",
            100.00m,
            endTime
        );
        Console.WriteLine();

        try
        {
            auctionService.PlaceBid(laptopAuction.GetId(), alice.GetId(), 110.00m);
            Thread.Sleep(500);

            auctionService.PlaceBid(laptopAuction.GetId(), bob.GetId(), 120.00m);
            Thread.Sleep(500);

            auctionService.PlaceBid(laptopAuction.GetId(), carol.GetId(), 125.00m);
            Thread.Sleep(500);

            auctionService.PlaceBid(laptopAuction.GetId(), alice.GetId(), 150.00m);

            Console.WriteLine("\n--- Waiting for auction to end automatically... ---");
            Thread.Sleep(2000);
        }
        catch (Exception e)
        {
            Console.WriteLine($"An error occurred during bidding: {e.Message}");
        }

        Console.WriteLine("\n--- Post-Auction Information ---");
        Auction endedAuction = auctionService.GetAuction(laptopAuction.GetId());

        if (endedAuction.GetWinningBid() != null)
        {
            Console.WriteLine($"Final Winner: {endedAuction.GetWinningBid().GetBidder().GetName()}");
            Console.WriteLine($"Winning Price: ${endedAuction.GetWinningBid().GetAmount():F2}");
        }
        else
        {
            Console.WriteLine("The auction ended with no winner.");
        }

        Console.WriteLine("\nFull Bid History:");
        foreach (Bid bid in endedAuction.GetBidHistory())
        {
            Console.WriteLine(bid.ToString());
        }

        Console.WriteLine("\n--- Attempting to bid on an ended auction ---");
        try
        {
            auctionService.PlaceBid(laptopAuction.GetId(), bob.GetId(), 200.00m);
        }
        catch (Exception e)
        {
            Console.WriteLine($"CAUGHT EXPECTED ERROR: {e.Message}");
        }

        auctionService.Shutdown();
    }
}



















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































