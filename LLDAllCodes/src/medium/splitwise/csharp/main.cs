
class BalanceSheet
{
    private readonly User owner;
    private readonly Dictionary<User, double> balances = new Dictionary<User, double>();
    private readonly object balanceLock = new object();

    public BalanceSheet(User owner)
    {
        this.owner = owner;
    }

    public Dictionary<User, double> GetBalances() => balances;

    public void AdjustBalance(User otherUser, double amount)
    {
        lock (balanceLock)
        {
            if (owner.Equals(otherUser))
            {
                return; // Cannot owe yourself
            }

            if (balances.ContainsKey(otherUser))
            {
                balances[otherUser] += amount;
            }
            else
            {
                balances[otherUser] = amount;
            }
        }
    }

    public void ShowBalances()
    {
        Console.WriteLine($"--- Balance Sheet for {owner.GetName()} ---");
        if (balances.Count == 0)
        {
            Console.WriteLine("All settled up!");
            return;
        }

        double totalOwedToMe = 0;
        double totalIOwe = 0;

        foreach (var entry in balances)
        {
            User otherUser = entry.Key;
            double amount = entry.Value;

            if (amount > 0.01)
            {
                Console.WriteLine($"{otherUser.GetName()} owes {owner.GetName()} ${amount:F2}");
                totalOwedToMe += amount;
            }
            else if (amount < -0.01)
            {
                Console.WriteLine($"{owner.GetName()} owes {otherUser.GetName()} ${-amount:F2}");
                totalIOwe += (-amount);
            }
        }

        Console.WriteLine($"Total Owed to {owner.GetName()}: ${totalOwedToMe:F2}");
        Console.WriteLine($"Total {owner.GetName()} Owes: ${totalIOwe:F2}");
        Console.WriteLine("---------------------------------");
    }
}







class Expense
{
    private readonly string id;
    private readonly string description;
    private readonly double amount;
    private readonly User paidBy;
    private readonly List<Split> splits;
    private readonly DateTime timestamp;

    public Expense(ExpenseBuilder builder)
    {
        this.id = builder.Id;
        this.description = builder.Description;
        this.amount = builder.Amount;
        this.paidBy = builder.PaidBy;
        this.timestamp = DateTime.Now;

        // Use the strategy to calculate splits
        this.splits = builder.SplitStrategy.CalculateSplits(
            builder.Amount, builder.PaidBy, builder.Participants, builder.SplitValues);
    }

    public string GetId() => id;
    public string GetDescription() => description;
    public double GetAmount() => amount;
    public User GetPaidBy() => paidBy;
    public List<Split> GetSplits() => splits;
}

class ExpenseBuilder
{
    public string Id { get; set; }
    public string Description { get; set; }
    public double Amount { get; set; }
    public User PaidBy { get; set; }
    public List<User> Participants { get; set; }
    public SplitStrategy SplitStrategy { get; set; }
    public List<double> SplitValues { get; set; }

    public ExpenseBuilder SetId(string id)
    {
        this.Id = id;
        return this;
    }

    public ExpenseBuilder SetDescription(string description)
    {
        this.Description = description;
        return this;
    }

    public ExpenseBuilder SetAmount(double amount)
    {
        this.Amount = amount;
        return this;
    }

    public ExpenseBuilder SetPaidBy(User paidBy)
    {
        this.PaidBy = paidBy;
        return this;
    }

    public ExpenseBuilder SetParticipants(List<User> participants)
    {
        this.Participants = participants;
        return this;
    }

    public ExpenseBuilder SetSplitStrategy(SplitStrategy splitStrategy)
    {
        this.SplitStrategy = splitStrategy;
        return this;
    }

    public ExpenseBuilder SetSplitValues(List<double> splitValues)
    {
        this.SplitValues = splitValues;
        return this;
    }

    public Expense Build()
    {
        if (SplitStrategy == null)
        {
            throw new InvalidOperationException("Split strategy is required.");
        }
        return new Expense(this);
    }
}








class Group
{
    private readonly string id;
    private readonly string name;
    private readonly List<User> members;

    public Group(string name, List<User> members)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.members = members;
    }

    public string GetId() => id;
    public string GetName() => name;
    public List<User> GetMembers() => new List<User>(members);
}







class Split
{
    private readonly User user;
    private readonly double amount;

    public Split(User user, double amount)
    {
        this.user = user;
        this.amount = amount;
    }

    public User GetUser() => user;
    public double GetAmount() => amount;
}






class Transaction
{
    private readonly User from;
    private readonly User to;
    private readonly double amount;

    public Transaction(User from, User to, double amount)
    {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public override string ToString()
    {
        return $"{from.GetName()} should pay {to.GetName()} ${amount:F2}";
    }
}




class User
{
    private readonly string id;
    private readonly string name;
    private readonly string email;
    private readonly BalanceSheet balanceSheet;

    public User(string name, string email)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.email = email;
        this.balanceSheet = new BalanceSheet(this);
    }

    public string GetId() => id;
    public string GetName() => name;
    public BalanceSheet GetBalanceSheet() => balanceSheet;
}








class EqualSplitStrategy : SplitStrategy
{
    public override List<Split> CalculateSplits(double totalAmount, User paidBy, List<User> participants, List<double> splitValues)
    {
        List<Split> splits = new List<Split>();
        double amountPerPerson = totalAmount / participants.Count;
        foreach (User participant in participants)
        {
            splits.Add(new Split(participant, amountPerPerson));
        }
        return splits;
    }
}





class ExactSplitStrategy : SplitStrategy
{
    public override List<Split> CalculateSplits(double totalAmount, User paidBy, List<User> participants, List<double> splitValues)
    {
        if (participants.Count != splitValues.Count)
        {
            throw new ArgumentException("Number of participants and split values must match.");
        }

        if (Math.Abs(splitValues.Sum() - totalAmount) > 0.01)
        {
            throw new ArgumentException("Sum of exact amounts must equal the total expense amount.");
        }

        List<Split> splits = new List<Split>();
        for (int i = 0; i < participants.Count; i++)
        {
            splits.Add(new Split(participants[i], splitValues[i]));
        }
        return splits;
    }
}






class PercentageSplitStrategy : SplitStrategy
{
    public override List<Split> CalculateSplits(double totalAmount, User paidBy, List<User> participants, List<double> splitValues)
    {
        if (participants.Count != splitValues.Count)
        {
            throw new ArgumentException("Number of participants and split values must match.");
        }

        if (Math.Abs(splitValues.Sum() - 100.0) > 0.01)
        {
            throw new ArgumentException("Sum of percentages must be 100.");
        }

        List<Split> splits = new List<Split>();
        for (int i = 0; i < participants.Count; i++)
        {
            double amount = (totalAmount * splitValues[i]) / 100.0;
            splits.Add(new Split(participants[i], amount));
        }
        return splits;
    }
}





abstract class SplitStrategy
{
    public abstract List<Split> CalculateSplits(double totalAmount, User paidBy, List<User> participants, List<double> splitValues);
}










using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class SplitwiseDemo
{
    public static void Main(string[] args)
    {
        // 1. Setup the service
        SplitwiseService service = SplitwiseService.GetInstance();

        // 2. Create users and groups
        User alice = service.AddUser("Alice", "alice@a.com");
        User bob = service.AddUser("Bob", "bob@b.com");
        User charlie = service.AddUser("Charlie", "charlie@c.com");
        User david = service.AddUser("David", "david@d.com");

        Group friendsGroup = service.AddGroup("Friends Trip", new List<User> { alice, bob, charlie, david });

        Console.WriteLine("--- System Setup Complete ---\n");

        // 3. Use Case 1: Equal Split
        Console.WriteLine("--- Use Case 1: Equal Split ---");
        service.CreateExpense(new ExpenseBuilder()
                              .SetDescription("Dinner")
                              .SetAmount(1000)
                              .SetPaidBy(alice)
                              .SetParticipants(new List<User> { alice, bob, charlie, david })
                              .SetSplitStrategy(new EqualSplitStrategy()));

        service.ShowBalanceSheet(alice.GetId());
        service.ShowBalanceSheet(bob.GetId());
        Console.WriteLine();

        // 4. Use Case 2: Exact Split
        Console.WriteLine("--- Use Case 2: Exact Split ---");
        service.CreateExpense(new ExpenseBuilder()
                              .SetDescription("Movie Tickets")
                              .SetAmount(370)
                              .SetPaidBy(alice)
                              .SetParticipants(new List<User> { bob, charlie })
                              .SetSplitStrategy(new ExactSplitStrategy())
                              .SetSplitValues(new List<double> { 120.0, 250.0 }));

        service.ShowBalanceSheet(alice.GetId());
        service.ShowBalanceSheet(bob.GetId());
        Console.WriteLine();

        // 5. Use Case 3: Percentage Split
        Console.WriteLine("--- Use Case 3: Percentage Split ---");
        service.CreateExpense(new ExpenseBuilder()
                              .SetDescription("Groceries")
                              .SetAmount(500)
                              .SetPaidBy(david)
                              .SetParticipants(new List<User> { alice, bob, charlie })
                              .SetSplitStrategy(new PercentageSplitStrategy())
                              .SetSplitValues(new List<double> { 40.0, 30.0, 30.0 })); // 40%, 30%, 30%

        Console.WriteLine("--- Balances After All Expenses ---");
        service.ShowBalanceSheet(alice.GetId());
        service.ShowBalanceSheet(bob.GetId());
        service.ShowBalanceSheet(charlie.GetId());
        service.ShowBalanceSheet(david.GetId());
        Console.WriteLine();

        // 6. Use Case 4: Simplify Group Debts
        Console.WriteLine("--- Use Case 4: Simplify Group Debts for 'Friends Trip' ---");
        List<Transaction> simplifiedDebts = service.SimplifyGroupDebts(friendsGroup.GetId());
        if (simplifiedDebts.Count == 0)
        {
            Console.WriteLine("All debts are settled within the group!");
        }
        else
        {
            foreach (Transaction debt in simplifiedDebts)
            {
                Console.WriteLine(debt.ToString());
            }
        }
        Console.WriteLine();

        service.ShowBalanceSheet(bob.GetId());

        // 7. Use Case 5: Partial Settlement
        Console.WriteLine("--- Use Case 5: Partial Settlement ---");
        // From the simplified debts, we see Bob should pay Alice. Let's say Bob pays 100.
        service.SettleUp(bob.GetId(), alice.GetId(), 100);

        Console.WriteLine("--- Balances After Partial Settlement ---");
        service.ShowBalanceSheet(alice.GetId());
        service.ShowBalanceSheet(bob.GetId());
    }
}















class SplitwiseService
{
    private static SplitwiseService instance;
    private static readonly object lockObject = new object();
    private readonly Dictionary<string, User> users = new Dictionary<string, User>();
    private readonly Dictionary<string, Group> groups = new Dictionary<string, Group>();

    private SplitwiseService() { }

    public static SplitwiseService GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new SplitwiseService();
                }
            }
        }
        return instance;
    }

    public User AddUser(string name, string email)
    {
        User user = new User(name, email);
        users[user.GetId()] = user;
        return user;
    }

    public Group AddGroup(string name, List<User> members)
    {
        Group group = new Group(name, members);
        groups[group.GetId()] = group;
        return group;
    }

    public User GetUser(string id)
    {
        return users.TryGetValue(id, out User user) ? user : null;
    }

    public Group GetGroup(string id)
    {
        return groups.TryGetValue(id, out Group group) ? group : null;
    }

    public void CreateExpense(ExpenseBuilder builder)
    {
        lock (lockObject)
        {
            Expense expense = builder.Build();
            User paidBy = expense.GetPaidBy();

            foreach (Split split in expense.GetSplits())
            {
                User participant = split.GetUser();
                double amount = split.GetAmount();

                if (!paidBy.Equals(participant))
                {
                    paidBy.GetBalanceSheet().AdjustBalance(participant, amount);
                    participant.GetBalanceSheet().AdjustBalance(paidBy, -amount);
                }
            }

            Console.WriteLine($"Expense '{expense.GetDescription()}' of amount {expense.GetAmount()} created.");
        }
    }

    public void SettleUp(string payerId, string payeeId, double amount)
    {
        lock (lockObject)
        {
            User payer = users[payerId];
            User payee = users[payeeId];
            Console.WriteLine($"{payer.GetName()} is settling up {amount} with {payee.GetName()}");

            // Settlement is like a reverse expense. payer owes less to payee.
            payee.GetBalanceSheet().AdjustBalance(payer, -amount);
            payer.GetBalanceSheet().AdjustBalance(payee, amount);
        }
    }

    public void ShowBalanceSheet(string userId)
    {
        User user = users[userId];
        user.GetBalanceSheet().ShowBalances();
    }

    public List<Transaction> SimplifyGroupDebts(string groupId)
    {
        Group group = groups[groupId];
        if (group == null)
        {
            throw new ArgumentException("Group not found");
        }

        // Calculate net balance for each member within the group context
        Dictionary<User, double> netBalances = new Dictionary<User, double>();
        foreach (User member in group.GetMembers())
        {
            double balance = 0;
            foreach (var entry in member.GetBalanceSheet().GetBalances())
            {
                // Consider only balances with other group members
                if (group.GetMembers().Contains(entry.Key))
                {
                    balance += entry.Value;
                }
            }
            netBalances[member] = balance;
        }

        // Separate into creditors and debtors
        var creditors = netBalances.Where(e => e.Value > 0).OrderByDescending(e => e.Value).ToList();
        var debtors = netBalances.Where(e => e.Value < 0).OrderBy(e => e.Value).ToList();

        List<Transaction> transactions = new List<Transaction>();
        int i = 0, j = 0;

        while (i < creditors.Count && j < debtors.Count)
        {
            var creditor = creditors[i];
            var debtor = debtors[j];

            double amountToSettle = Math.Min(creditor.Value, -debtor.Value);
            transactions.Add(new Transaction(debtor.Key, creditor.Key, amountToSettle));

            // Update the values
            creditors[i] = new KeyValuePair<User, double>(creditor.Key, creditor.Value - amountToSettle);
            debtors[j] = new KeyValuePair<User, double>(debtor.Key, debtor.Value + amountToSettle);

            if (Math.Abs(creditors[i].Value) < 0.01) i++;
            if (Math.Abs(debtors[j].Value) < 0.01) j++;
        }

        return transactions;
    }
}







































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































