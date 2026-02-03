class Book : LibraryItem
{
    private readonly string author;

    public Book(string id, string title, string author) : base(id, title)
    {
        this.author = author;
    }

    public override string GetAuthorOrPublisher() { return author; }
}










class BookCopy
{
    private readonly string id;
    private readonly LibraryItem item;
    private IItemState currentState;

    public BookCopy(string id, LibraryItem item)
    {
        this.id = id;
        this.item = item;
        this.currentState = new AvailableState();
        item.AddCopy(this);
    }

    public void Checkout(Member member) { currentState.Checkout(this, member); }
    public void ReturnItem() { currentState.ReturnItem(this); }
    public void PlaceHold(Member member) { currentState.PlaceHold(this, member); }

    public void SetState(IItemState state) { this.currentState = state; }
    public string GetId() { return id; }
    public LibraryItem GetItem() { return item; }
    public bool IsAvailable() { return currentState is AvailableState; }
}



abstract class LibraryItem
{
    private readonly string id;
    private readonly string title;
    protected readonly List<BookCopy> copies = new List<BookCopy>();
    private readonly List<Member> observers = new List<Member>();

    public LibraryItem(string id, string title)
    {
        this.id = id;
        this.title = title;
    }

    public void AddCopy(BookCopy copy) { copies.Add(copy); }
    public void AddObserver(Member member) { observers.Add(member); }
    public void RemoveObserver(Member member) { observers.Remove(member); }

    public void NotifyObservers()
    {
        Console.WriteLine($"Notifying {observers.Count} observers for '{title}'...");
        var observersCopy = new List<Member>(observers);
        foreach (var observer in observersCopy)
        {
            observer.Update(this);
        }
    }

    public BookCopy GetAvailableCopy()
    {
        return copies.FirstOrDefault(copy => copy.IsAvailable());
    }

    public string GetId() { return id; }
    public string GetTitle() { return title; }
    public List<BookCopy> GetCopies() { return copies; }

    public abstract string GetAuthorOrPublisher();

    public long GetAvailableCopyCount()
    {
        return copies.Count(copy => copy.IsAvailable());
    }

    public bool HasObservers() { return observers.Count > 0; }
    public bool IsObserver(Member member) { return observers.Contains(member); }
}




class Loan
{
    private readonly BookCopy copy;
    private readonly Member member;
    private readonly DateTime checkoutDate;

    public Loan(BookCopy copy, Member member)
    {
        this.copy = copy;
        this.member = member;
        this.checkoutDate = DateTime.Now;
    }

    public BookCopy GetCopy() { return copy; }
    public Member GetMember() { return member; }
}





class Magazine : LibraryItem
{
    private readonly string publisher;

    public Magazine(string id, string title, string publisher) : base(id, title)
    {
        this.publisher = publisher;
    }

    public override string GetAuthorOrPublisher() { return publisher; }
}







class Member
{
    private readonly string id;
    private readonly string name;
    private readonly List<Loan> loans = new List<Loan>();

    public Member(string id, string name)
    {
        this.id = id;
        this.name = name;
    }

    public void Update(LibraryItem item)
    {
        Console.WriteLine($"NOTIFICATION for {name}: The book '{item.GetTitle()}' you placed a hold on is now available!");
    }

    public void AddLoan(Loan loan) { loans.Add(loan); }
    public void RemoveLoan(Loan loan) { loans.Remove(loan); }
    public string GetId() { return id; }
    public string GetName() { return name; }
    public List<Loan> GetLoans() { return loans; }
}





enum ItemType
{
    BOOK,
    MAGAZINE
}







class ItemFactory
{
    public static LibraryItem CreateItem(ItemType type, string id, string title, string author)
    {
        switch (type)
        {
            case ItemType.BOOK:
                return new Book(id, title, author);
            case ItemType.MAGAZINE:
                return new Magazine(id, title, author);
            default:
                throw new ArgumentException("Unknown item type.");
        }
    }
}





class TransactionService
{
    private static readonly TransactionService instance = new TransactionService();
    private readonly Dictionary<string, Loan> activeLoans = new Dictionary<string, Loan>();

    private TransactionService() { }
    public static TransactionService GetInstance() { return instance; }

    public void CreateLoan(BookCopy copy, Member member)
    {
        if (activeLoans.ContainsKey(copy.GetId()))
        {
            throw new InvalidOperationException("This copy is already on loan.");
        }
        
        var loan = new Loan(copy, member);
        activeLoans[copy.GetId()] = loan;
        member.AddLoan(loan);
    }

    public void EndLoan(BookCopy copy)
    {
        if (activeLoans.TryGetValue(copy.GetId(), out var loan))
        {
            activeLoans.Remove(copy.GetId());
            loan.GetMember().RemoveLoan(loan);
        }
    }
}




class AvailableState : IItemState
{
    public void Checkout(BookCopy copy, Member member)
    {
        TransactionService.GetInstance().CreateLoan(copy, member);
        copy.SetState(new CheckedOutState());
        Console.WriteLine($"{copy.GetId()} checked out by {member.GetName()}");
    }

    public void ReturnItem(BookCopy copy)
    {
        Console.WriteLine("Cannot return an item that is already available.");
    }

    public void PlaceHold(BookCopy copy, Member member)
    {
        Console.WriteLine("Cannot place hold on an available item. Please check it out.");
    }
}




class CheckedOutState : IItemState
{
    public void Checkout(BookCopy copy, Member member)
    {
        Console.WriteLine($"{copy.GetId()} is already checked out.");
    }

    public void ReturnItem(BookCopy copy)
    {
        TransactionService.GetInstance().EndLoan(copy);
        Console.WriteLine($"{copy.GetId()} returned.");
        
        if (copy.GetItem().HasObservers())
        {
            copy.SetState(new OnHoldState());
            copy.GetItem().NotifyObservers();
        }
        else
        {
            copy.SetState(new AvailableState());
        }
    }

    public void PlaceHold(BookCopy copy, Member member)
    {
        copy.GetItem().AddObserver(member);
        Console.WriteLine($"{member.GetName()} placed a hold on '{copy.GetItem().GetTitle()}'");
    }
}




interface IItemState
{
    void Checkout(BookCopy copy, Member member);
    void ReturnItem(BookCopy copy);
    void PlaceHold(BookCopy copy, Member member);
}




class OnHoldState : IItemState
{
    public void Checkout(BookCopy copy, Member member)
    {
        if (copy.GetItem().IsObserver(member))
        {
            TransactionService.GetInstance().CreateLoan(copy, member);
            copy.GetItem().RemoveObserver(member);
            copy.SetState(new CheckedOutState());
            Console.WriteLine($"Hold fulfilled. {copy.GetId()} checked out by {member.GetName()}");
        }
        else
        {
            Console.WriteLine("This item is on hold for another member.");
        }
    }

    public void ReturnItem(BookCopy copy)
    {
        Console.WriteLine("Invalid action. Item is on hold, not checked out.");
    }

    public void PlaceHold(BookCopy copy, Member member)
    {
        Console.WriteLine("Item is already on hold.");
    }
}






interface ISearchStrategy
{
    List<LibraryItem> Search(string query, List<LibraryItem> items);
}


class SearchByAuthorStrategy : ISearchStrategy
{
    public List<LibraryItem> Search(string query, List<LibraryItem> items)
    {
        return items.Where(item => item.GetAuthorOrPublisher().ToLower().Contains(query.ToLower())).ToList();
    }
}




class SearchByTitleStrategy : ISearchStrategy
{
    public List<LibraryItem> Search(string query, List<LibraryItem> items)
    {
        return items.Where(item => item.GetTitle().ToLower().Contains(query.ToLower())).ToList();
    }
}








using System;
using System.Collections.Generic;
using System.Linq;

public class LibraryManagementDemo
{
    public static void Main(string[] args)
    {
        var library = LibraryManagementSystem.GetInstance();

        // === Setting up the Library ===
        Console.WriteLine("=== Setting up the Library ===");

        var hobbitCopies = library.AddItem(ItemType.BOOK, "B001", "The Hobbit", "J.R.R. Tolkien", 2);
        var duneCopies = library.AddItem(ItemType.BOOK, "B002", "Dune", "Frank Herbert", 1);
        var natGeoCopies = library.AddItem(ItemType.MAGAZINE, "M001", "National Geographic", "NatGeo Society", 3);

        var alice = library.AddMember("MEM01", "Alice");
        var bob = library.AddMember("MEM02", "Bob");
        var charlie = library.AddMember("MEM03", "Charlie");
        library.PrintCatalog();

        // === Scenario 1: Searching (Strategy Pattern) ===
        Console.WriteLine("\n=== Scenario 1: Searching for Items ===");
        Console.WriteLine("Searching for title 'Dune':");
        var titleResults = library.Search("Dune", new SearchByTitleStrategy());
        foreach (var item in titleResults)
        {
            Console.WriteLine($"Found: {item.GetTitle()}");
        }

        Console.WriteLine("\nSearching for author 'Tolkien':");
        var authorResults = library.Search("Tolkien", new SearchByAuthorStrategy());
        foreach (var item in authorResults)
        {
            Console.WriteLine($"Found: {item.GetTitle()}");
        }

        // === Scenario 2: Checkout and Return (State Pattern) ===
        Console.WriteLine("\n\n=== Scenario 2: Checkout and Return ===");
        library.Checkout(alice.GetId(), hobbitCopies[0].GetId());
        library.Checkout(bob.GetId(), duneCopies[0].GetId());
        library.PrintCatalog();

        Console.WriteLine("Attempting to checkout an already checked-out book:");
        library.Checkout(charlie.GetId(), hobbitCopies[0].GetId());

        Console.WriteLine("\nAlice returns The Hobbit:");
        library.ReturnItem(hobbitCopies[0].GetId());
        library.PrintCatalog();

        // === Scenario 3: Holds and Notifications (Observer Pattern) ===
        Console.WriteLine("\n\n=== Scenario 3: Placing a Hold ===");
        Console.WriteLine("Dune is checked out by Bob. Charlie places a hold.");
        library.PlaceHold(charlie.GetId(), "B002");

        Console.WriteLine("\nBob returns Dune. Charlie should be notified.");
        library.ReturnItem(duneCopies[0].GetId());

        Console.WriteLine("\nCharlie checks out the book that was on hold for him.");
        library.Checkout(charlie.GetId(), duneCopies[0].GetId());

        Console.WriteLine("\nTrying to check out the same on-hold item by another member (Alice):");
        library.Checkout(alice.GetId(), duneCopies[0].GetId());

        library.PrintCatalog();
    }
}








class LibraryManagementSystem
{
    private static readonly LibraryManagementSystem instance = new LibraryManagementSystem();
    private readonly Dictionary<string, LibraryItem> catalog = new Dictionary<string, LibraryItem>();
    private readonly Dictionary<string, Member> members = new Dictionary<string, Member>();
    private readonly Dictionary<string, BookCopy> copies = new Dictionary<string, BookCopy>();

    private LibraryManagementSystem() { }
    public static LibraryManagementSystem GetInstance() { return instance; }

    public List<BookCopy> AddItem(ItemType type, string id, string title, string author, int numCopies)
    {
        var bookCopies = new List<BookCopy>();
        var item = ItemFactory.CreateItem(type, id, title, author);
        catalog[id] = item;
        
        for (int i = 0; i < numCopies; i++)
        {
            string copyId = $"{id}-c{i + 1}";
            var copy = new BookCopy(copyId, item);
            copies[copyId] = copy;
            bookCopies.Add(copy);
        }
        
        Console.WriteLine($"Added {numCopies} copies of '{title}'");
        return bookCopies;
    }

    public Member AddMember(string id, string name)
    {
        var member = new Member(id, name);
        members[id] = member;
        return member;
    }

    public void Checkout(string memberId, string copyId)
    {
        if (members.TryGetValue(memberId, out var member) && copies.TryGetValue(copyId, out var copy))
        {
            copy.Checkout(member);
        }
        else
        {
            Console.WriteLine("Error: Invalid member or copy ID.");
        }
    }

    public void ReturnItem(string copyId)
    {
        if (copies.TryGetValue(copyId, out var copy))
        {
            copy.ReturnItem();
        }
        else
        {
            Console.WriteLine("Error: Invalid copy ID.");
        }
    }

    public void PlaceHold(string memberId, string itemId)
    {
        if (members.TryGetValue(memberId, out var member) && catalog.TryGetValue(itemId, out var item))
        {
            var checkedOutCopy = item.GetCopies().FirstOrDefault(c => !c.IsAvailable());
            checkedOutCopy?.PlaceHold(member);
        }
    }

    public List<LibraryItem> Search(string query, ISearchStrategy strategy)
    {
        return strategy.Search(query, catalog.Values.ToList());
    }

    public void PrintCatalog()
    {
        Console.WriteLine("\n--- Library Catalog ---");
        foreach (var item in catalog.Values)
        {
            Console.WriteLine($"ID: {item.GetId()}, Title: {item.GetTitle()}, " +
                            $"Author/Publisher: {item.GetAuthorOrPublisher()}, " +
                            $"Available: {item.GetAvailableCopyCount()}");
        }
        Console.WriteLine("-----------------------\n");
    }
}

























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































