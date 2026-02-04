
interface IDispenseChain
{
    void SetNextChain(IDispenseChain nextChain);
    void Dispense(int amount);
    bool CanDispense(int amount);
}









class NoteDispenser100 : NoteDispenser
{
    public NoteDispenser100(int numNotes) : base(100, numNotes) { }
}



class NoteDispenser20 : NoteDispenser
{
    public NoteDispenser20(int numNotes) : base(20, numNotes) { }
}





class NoteDispenser50 : NoteDispenser
{
    public NoteDispenser50(int numNotes) : base(50, numNotes) { }
}







class Account
{
    private readonly string accountNumber;
    private double balance;
    private readonly Dictionary<string, Card> cards;
    private readonly object accountLock = new object();

    public Account(string accountNumber, double balance)
    {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.cards = new Dictionary<string, Card>();
    }

    public string GetAccountNumber() => accountNumber;
    public double GetBalance() => balance;
    public Dictionary<string, Card> GetCards() => cards;

    public void Deposit(double amount)
    {
        lock (accountLock)
        {
            balance += amount;
        }
    }

    public bool Withdraw(double amount)
    {
        lock (accountLock)
        {
            if (balance >= amount)
            {
                balance -= amount;
                return true;
            }
            return false;
        }
    }
}






class Card
{
    private readonly string cardNumber;
    private readonly string pin;

    public Card(string cardNumber, string pin)
    {
        this.cardNumber = cardNumber;
        this.pin = pin;
    }

    public string GetCardNumber() => cardNumber;
    public string GetPin() => pin;
}






class CashDispenser
{
    private readonly IDispenseChain chain;
    private readonly object dispenserLock = new object();

    public CashDispenser(IDispenseChain chain)
    {
        this.chain = chain;
    }

    public void DispenseCash(int amount)
    {
        lock (dispenserLock)
        {
            chain.Dispense(amount);
        }
    }

    public bool CanDispenseCash(int amount)
    {
        lock (dispenserLock)
        {
            if (amount % 10 != 0)
            {
                return false;
            }
            return chain.CanDispense(amount);
        }
    }
}








abstract class NoteDispenser : IDispenseChain
{
    private IDispenseChain nextChain;
    private readonly int noteValue;
    private int numNotes;
    private readonly object dispenserLock = new object();

    public NoteDispenser(int noteValue, int numNotes)
    {
        this.noteValue = noteValue;
        this.numNotes = numNotes;
    }

    public void SetNextChain(IDispenseChain nextChain)
    {
        this.nextChain = nextChain;
    }

    public void Dispense(int amount)
    {
        lock (dispenserLock)
        {
            if (amount >= noteValue)
            {
                int numToDispense = Math.Min(amount / noteValue, numNotes);
                int remainingAmount = amount - (numToDispense * noteValue);

                if (numToDispense > 0)
                {
                    Console.WriteLine($"Dispensing {numToDispense} x ${noteValue} note(s)");
                    numNotes -= numToDispense;
                }

                if (remainingAmount > 0 && nextChain != null)
                {
                    nextChain.Dispense(remainingAmount);
                }
            }
            else if (nextChain != null)
            {
                nextChain.Dispense(amount);
            }
        }
    }

    public bool CanDispense(int amount)
    {
        lock (dispenserLock)
        {
            if (amount < 0) return false;
            if (amount == 0) return true;

            int numToUse = Math.Min(amount / noteValue, numNotes);
            int remainingAmount = amount - (numToUse * noteValue);

            if (remainingAmount == 0) return true;
            if (nextChain != null)
            {
                return nextChain.CanDispense(remainingAmount);
            }
            return false;
        }
    }
}







enum OperationType
{
    CHECK_BALANCE,
    WITHDRAW_CASH,
    DEPOSIT_CASH
}







class BankService
{
    private readonly Dictionary<string, Account> accounts = new Dictionary<string, Account>();
    private readonly Dictionary<string, Card> cards = new Dictionary<string, Card>();
    private readonly Dictionary<Card, Account> cardAccountMap = new Dictionary<Card, Account>();

    public BankService()
    {
        // Create sample accounts and cards
        Account account1 = CreateAccount("1234567890", 1000.0);
        Card card1 = CreateCard("1234-5678-9012-3456", "1234");
        LinkCardToAccount(card1, account1);

        Account account2 = CreateAccount("9876543210", 500.0);
        Card card2 = CreateCard("9876-5432-1098-7654", "4321");
        LinkCardToAccount(card2, account2);
    }

    public Account CreateAccount(string accountNumber, double initialBalance)
    {
        Account account = new Account(accountNumber, initialBalance);
        accounts[accountNumber] = account;
        return account;
    }

    public Card CreateCard(string cardNumber, string pin)
    {
        Card card = new Card(cardNumber, pin);
        cards[cardNumber] = card;
        return card;
    }

    public bool Authenticate(Card card, string pin)
    {
        return card.GetPin() == pin;
    }

    public Card AuthenticateCard(string cardNumber)
    {
        return cards.TryGetValue(cardNumber, out Card card) ? card : null;
    }

    public double GetBalance(Card card)
    {
        return cardAccountMap[card].GetBalance();
    }

    public void WithdrawMoney(Card card, double amount)
    {
        cardAccountMap[card].Withdraw(amount);
    }

    public void DepositMoney(Card card, double amount)
    {
        cardAccountMap[card].Deposit(amount);
    }

    public void LinkCardToAccount(Card card, Account account)
    {
        account.GetCards()[card.GetCardNumber()] = card;
        cardAccountMap[card] = account;
    }
}










interface IATMState
{
    void InsertCard(ATM atm, string cardNumber);
    void EnterPin(ATM atm, string pin);
    void SelectOperation(ATM atm, OperationType op, int amount = 0);
    void EjectCard(ATM atm);
}











class AuthenticatedState : IATMState
{
    public void InsertCard(ATM atm, string cardNumber)
    {
        Console.WriteLine("Error: A card is already inserted and a session is active.");
    }

    public void EnterPin(ATM atm, string pin)
    {
        Console.WriteLine("Error: PIN has already been entered and authenticated.");
    }

    public void SelectOperation(ATM atm, OperationType op, int amount = 0)
    {
        switch (op)
        {
            case OperationType.CHECK_BALANCE:
                atm.CheckBalance();
                break;

            case OperationType.WITHDRAW_CASH:
                if (amount <= 0)
                {
                    Console.WriteLine("Error: Invalid withdrawal amount specified.");
                    break;
                }

                double accountBalance = atm.GetBankService().GetBalance(atm.GetCurrentCard());
                if (amount > accountBalance)
                {
                    Console.WriteLine("Error: Insufficient balance.");
                    break;
                }

                Console.WriteLine($"Processing withdrawal for ${amount}");
                atm.WithdrawCash(amount);
                break;

            case OperationType.DEPOSIT_CASH:
                if (amount <= 0)
                {
                    Console.WriteLine("Error: Invalid deposit amount specified.");
                    break;
                }
                Console.WriteLine($"Processing deposit for ${amount}");
                atm.DepositCash(amount);
                break;

            default:
                Console.WriteLine("Error: Invalid operation selected.");
                break;
        }

        // End the session after one transaction
        Console.WriteLine("Transaction complete.");
        EjectCard(atm);
    }

    public void EjectCard(ATM atm)
    {
        Console.WriteLine("Ending session. Card has been ejected. Thank you for using our ATM.");
        atm.SetCurrentCard(null);
        atm.ChangeState(new IdleState());
    }
}










class HasCardState : IATMState
{
    public void InsertCard(ATM atm, string cardNumber)
    {
        Console.WriteLine("Error: A card is already inserted. Cannot insert another card.");
    }

    public void EnterPin(ATM atm, string pin)
    {
        Console.WriteLine("Authenticating PIN...");
        Card card = atm.GetCurrentCard();
        bool isAuthenticated = atm.GetBankService().Authenticate(card, pin);

        if (isAuthenticated)
        {
            Console.WriteLine("Authentication successful.");
            atm.ChangeState(new AuthenticatedState());
        }
        else
        {
            Console.WriteLine("Authentication failed: Incorrect PIN.");
            EjectCard(atm);
        }
    }

    public void SelectOperation(ATM atm, OperationType op, int amount = 0)
    {
        Console.WriteLine("Error: Please enter your PIN first to select an operation.");
    }

    public void EjectCard(ATM atm)
    {
        Console.WriteLine("Card has been ejected. Thank you for using our ATM.");
        atm.SetCurrentCard(null);
        atm.ChangeState(new IdleState());
    }
}








class IdleState : IATMState
{
    public void InsertCard(ATM atm, string cardNumber)
    {
        Console.WriteLine("\nCard has been inserted.");
        Card card = atm.GetBankService().AuthenticateCard(cardNumber);

        if (card == null)
        {
            EjectCard(atm);
        }
        else
        {
            atm.SetCurrentCard(card);
            atm.ChangeState(new HasCardState());
        }
    }

    public void EnterPin(ATM atm, string pin)
    {
        Console.WriteLine("Error: Please insert a card first.");
    }

    public void SelectOperation(ATM atm, OperationType op, int amount = 0)
    {
        Console.WriteLine("Error: Please insert a card first.");
    }

    public void EjectCard(ATM atm)
    {
        Console.WriteLine("Error: Card not found.");
        atm.SetCurrentCard(null);
    }
}







class ATM
{
    private static ATM instance;
    private static readonly object lockObject = new object();
    private readonly BankService bankService;
    private readonly CashDispenser cashDispenser;
    private static long transactionCounter = 0;
    private IATMState currentState;
    private Card currentCard;

    private ATM()
    {
        currentState = new IdleState();
        bankService = new BankService();

        // Setup the dispenser chain
        IDispenseChain c1 = new NoteDispenser100(10); // 10 x $100 notes
        IDispenseChain c2 = new NoteDispenser50(20);  // 20 x $50 notes
        IDispenseChain c3 = new NoteDispenser20(30);  // 30 x $20 notes
        c1.SetNextChain(c2);
        c2.SetNextChain(c3);
        cashDispenser = new CashDispenser(c1);
    }

    public static ATM GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new ATM();
                }
            }
        }
        return instance;
    }

    public void ChangeState(IATMState newState)
    {
        currentState = newState;
    }

    public void SetCurrentCard(Card card)
    {
        currentCard = card;
    }

    public void InsertCard(string cardNumber)
    {
        currentState.InsertCard(this, cardNumber);
    }

    public void EnterPin(string pin)
    {
        currentState.EnterPin(this, pin);
    }

    public void SelectOperation(OperationType op, int amount = 0)
    {
        currentState.SelectOperation(this, op, amount);
    }

    public void CheckBalance()
    {
        double balance = bankService.GetBalance(currentCard);
        Console.WriteLine($"Your current account balance is: ${balance:F2}");
    }

    public void WithdrawCash(int amount)
    {
        if (!cashDispenser.CanDispenseCash(amount))
        {
            throw new InvalidOperationException("Insufficient cash available in the ATM.");
        }

        bankService.WithdrawMoney(currentCard, amount);

        try
        {
            cashDispenser.DispenseCash(amount);
        }
        catch (Exception)
        {
            bankService.DepositMoney(currentCard, amount); // Deposit back if dispensing fails
            throw;
        }
    }

    public void DepositCash(int amount)
    {
        bankService.DepositMoney(currentCard, amount);
    }

    public Card GetCurrentCard() => currentCard;
    public BankService GetBankService() => bankService;
}













using System;
using System.Collections.Generic;
using System.Threading;

public class ATMDemo
{
    public static void Main(string[] args)
    {
        ATM atm = ATM.GetInstance();

        // Perform Check Balance operation
        atm.InsertCard("1234-5678-9012-3456");
        atm.EnterPin("1234");
        atm.SelectOperation(OperationType.CHECK_BALANCE); // $1000

        // Perform Withdraw Cash operation
        atm.InsertCard("1234-5678-9012-3456");
        atm.EnterPin("1234");
        atm.SelectOperation(OperationType.WITHDRAW_CASH, 570);

        // Perform Deposit Cash operation
        atm.InsertCard("1234-5678-9012-3456");
        atm.EnterPin("1234");
        atm.SelectOperation(OperationType.DEPOSIT_CASH, 200);

        // Perform Check Balance operation
        atm.InsertCard("1234-5678-9012-3456");
        atm.EnterPin("1234");
        atm.SelectOperation(OperationType.CHECK_BALANCE); // $630

        // Perform Withdraw Cash more than balance
        atm.InsertCard("1234-5678-9012-3456");
        atm.EnterPin("1234");
        atm.SelectOperation(OperationType.WITHDRAW_CASH, 700); // Insufficient balance

        // Insert Incorrect PIN
        atm.InsertCard("1234-5678-9012-3456");
        atm.EnterPin("3425");
    }
}































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































