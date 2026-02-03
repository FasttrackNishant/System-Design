package easy.snakeandladder.java;


class CashDispenser {
    private final DispenseChain chain;

    public CashDispenser(DispenseChain chain) {
        this.chain = chain;
    }

    public synchronized void dispenseCash(int amount) {
        chain.dispense(amount);
    }

    public synchronized boolean canDispenseCash(int amount) {
        if (amount % 10 != 0) {
            return false;
        }
        return chain.canDispense(amount);
    }
}



interface DispenseChain {
    void setNextChain(DispenseChain nextChain);
    void dispense(int amount);
    boolean canDispense(int amount);
}





abstract class NoteDispenser implements DispenseChain {
    private DispenseChain nextChain;
    private final int noteValue;
    private int numNotes;

    public NoteDispenser(int noteValue, int numNotes) {
        this.noteValue = noteValue;
        this.numNotes = numNotes;
    }

    @Override
    public void setNextChain(DispenseChain nextChain) {
        this.nextChain = nextChain;
    }

    @Override
    public synchronized void dispense(int amount) {
        if (amount >= noteValue) {
            int numToDispense = Math.min(amount / noteValue, this.numNotes);
            int remainingAmount = amount - (numToDispense * noteValue);

            if (numToDispense > 0) {
                System.out.println("Dispensing " + numToDispense + " x $" + noteValue + " note(s)");
                this.numNotes -= numToDispense;
            }

            if (remainingAmount > 0 && this.nextChain != null) {
                this.nextChain.dispense(remainingAmount);
            }
        } else if (this.nextChain != null) {
            this.nextChain.dispense(amount);
        }
    }

    @Override
    public synchronized boolean canDispense(int amount) {
        if (amount < 0) return false;
        if (amount == 0) return true;

        int numToUse = Math.min(amount / noteValue, this.numNotes);
        int remainingAmount = amount - (numToUse * noteValue);

        if (remainingAmount == 0) return true;
        if (this.nextChain != null) {
            return this.nextChain.canDispense(remainingAmount);
        }
        return false;
    }
}





class NoteDispenser100 extends NoteDispenser{
    public NoteDispenser100(int numNotes) {
        super(100, numNotes);
    }
}





class NoteDispenser20 extends NoteDispenser{
    public NoteDispenser20(int numNotes) { super(20, numNotes); }
}




class NoteDispenser50 extends NoteDispenser{
    public NoteDispenser50(int numNotes) { super(50, numNotes); }
}






class Account {
    private final String accountNumber;
    private double balance;
    private Map<String, Card> cards;

    public Account(String accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.cards = new HashMap<>();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public Map<String, Card> getCards() {
        return cards;
    }

    public synchronized void deposit(double amount) {
        balance += amount;
    }

    public synchronized boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
}




class Card {
    private final String cardNumber;
    private final String pin;

    public Card(String cardNumber, String pin) {
        this.cardNumber = cardNumber;
        this.pin = pin;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getPin() {
        return pin;
    }
}






enum OperationType {
    CHECK_BALANCE,
    WITHDRAW_CASH,
    DEPOSIT_CASH
}



class BankService {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Card> cards = new ConcurrentHashMap<>();
    private final Map<Card, Account> cardAccountMap = new ConcurrentHashMap<>();

    public BankService() {
        // Create sample accounts and cards
        Account account1 = createAccount("1234567890", 1000.0);
        Card card1 = createCard("1234-5678-9012-3456", "1234");
        linkCardToAccount(card1, account1);

        Account account2 = createAccount("9876543210", 500.0);
        Card card2 = createCard("9876-5432-1098-7654", "4321");
        linkCardToAccount(card2, account2);
    }

    public Account createAccount(String accountNumber, double initialBalance) {
        Account account = new Account(accountNumber, initialBalance);
        accounts.put(accountNumber, account);
        return account;
    }

    public Card createCard(String cardNumber, String pin) {
        Card card = new Card(cardNumber, pin);
        cards.put(cardNumber, card);
        return card;
    }

    public boolean authenticate(Card card, String pin) {
        return card.getPin().equals(pin);
    }

    public Card authenticate(String cardNumber) {
        return cards.getOrDefault(cardNumber, null);
    }

    public double getBalance(Card card) {
        return cardAccountMap.get(card).getBalance();
    }

    public void withdrawMoney(Card card, double amount) {
        cardAccountMap.get(card).withdraw(amount);
    }

    public void depositMoney(Card card, double amount) {
        cardAccountMap.get(card).deposit(amount);
    }

    public void linkCardToAccount(Card card, Account account) {
        account.getCards().put(card.getCardNumber(), card);
        cardAccountMap.put(card, account);
    }
}



interface ATMState {
    void insertCard(ATM atm, String cardNumber);
    void enterPin(ATM atm, String pin);
    void selectOperation(ATM atm, OperationType op, int... args);
    void ejectCard(ATM atm);
}






class AuthenticatedState implements ATMState {
    @Override
    public void insertCard(ATM atm, String cardNumber) {
        System.out.println("Error: A card is already inserted and a session is active.");
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("Error: PIN has already been entered and authenticated.");
    }

    @Override
    public void selectOperation(ATM atm, OperationType op, int... args) {
        // In a real UI, this would be a menu. Here we use a switch.
        switch (op) {
            case CHECK_BALANCE:
                atm.checkBalance();
                break;

            case WITHDRAW_CASH:
                if (args.length == 0 || args[0] <= 0) {
                    System.out.println("Error: Invalid withdrawal amount specified.");
                    break;
                }
                int amountToWithdraw = args[0];

                double accountBalance = atm.getBankService().getBalance(atm.getCurrentCard());

                if (amountToWithdraw > accountBalance) {
                    System.out.println("Error: Insufficient balance.");
                    break;
                }

                System.out.println("Processing withdrawal for $" + amountToWithdraw);
                // Delegate the complex withdrawal logic to the ATM's dedicated method
                atm.withdrawCash(amountToWithdraw);
                break;

            case DEPOSIT_CASH:
                if (args.length == 0 || args[0] <= 0) {
                    System.out.println("Error: Invalid withdrawal amount specified.");
                    break;
                }
                int amountToDeposit = args[0];
                System.out.println("Processing deposit for $" + amountToDeposit);
                atm.depositCash(amountToDeposit);
                break;

            default:
                System.out.println("Error: Invalid operation selected.");
                break;
        }

        // End the session after one transaction
        System.out.println("Transaction complete.");
        ejectCard(atm);
    }

    @Override
    public void ejectCard(ATM atm) {
        System.out.println("Ending session. Card has been ejected. Thank you for using our ATM.");
        atm.setCurrentCard(null);
        atm.changeState(new IdleState());
    }
}












class HasCardState implements ATMState {
    @Override
    public void insertCard(ATM atm, String cardNumber) {
        System.out.println("Error: A card is already inserted. Cannot insert another card.");
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("Authenticating PIN...");
        Card card = atm.getCurrentCard();
        boolean isAuthenticated = atm.getBankService().authenticate(card, pin);

        if (isAuthenticated) {
            System.out.println("Authentication successful.");
            atm.changeState(new AuthenticatedState());
        } else {
            System.out.println("Authentication failed: Incorrect PIN.");
            ejectCard(atm);
        }
    }

    @Override
    public void selectOperation(ATM atm, OperationType op, int... args) {
        System.out.println("Error: Please enter your PIN first to select an operation.");
    }

    @Override
    public void ejectCard(ATM atm) {
        System.out.println("Card has been ejected. Thank you for using our ATM.");
        atm.setCurrentCard(null);
        atm.changeState(new IdleState());
    }
}







class IdleState implements ATMState {
    @Override
    public void insertCard(ATM atm, String cardNumber) {
        System.out.println("\nCard has been inserted.");
        Card card = atm.getBankService().authenticate(cardNumber);

        if (card == null) {
            ejectCard(atm);
        } else {
            atm.setCurrentCard(card);
            atm.changeState(new HasCardState());
        }
    }

    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("Error: Please insert a card first.");
    }

    @Override
    public void selectOperation(ATM atm, OperationType op, int... args) {
        System.out.println("Error: Please insert a card first.");
    }

    @Override
    public void ejectCard(ATM atm) {
        System.out.println("Error: Card not found.");
        atm.setCurrentCard(null);
    }
}











class ATM {
    private static ATM INSTANCE;
    private final BankService bankService;
    private final CashDispenser cashDispenser;
    private static final AtomicLong transactionCounter = new AtomicLong(0);
    private ATMState currentState;
    private Card currentCard;

    private ATM() {
        this.currentState = new IdleState();
        this.bankService = new BankService();

        // Setup the dispenser chain
        DispenseChain c1 = new NoteDispenser100(10); // 10 x $100 notes
        DispenseChain c2 = new NoteDispenser50(20); // 20 x $50 notes
        DispenseChain c3 = new NoteDispenser20(30); // 30 x $20 notes
        c1.setNextChain(c2);
        c2.setNextChain(c3);
        this.cashDispenser = new CashDispenser(c1);
    }

    public static ATM getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ATM();
        }
        return INSTANCE;
    }

    public void changeState(ATMState newState) { this.currentState = newState; }
    public void setCurrentCard(Card card) { this.currentCard = card; }

    public void insertCard(String cardNumber) {
        currentState.insertCard(this, cardNumber);
    }

    public void enterPin(String pin) {
        currentState.enterPin(this, pin);
    }

    public void selectOperation(OperationType op, int... args) { currentState.selectOperation(this, op, args); }

    public void checkBalance() {
        double balance = bankService.getBalance(currentCard);
        System.out.printf("Your current account balance is: $%.2f%n", balance);
    }

    public void withdrawCash(int amount) {
        if (!cashDispenser.canDispenseCash(amount)) {
            throw new IllegalStateException("Insufficient cash available in the ATM.");
        }

        bankService.withdrawMoney(currentCard, amount);

        try {
            cashDispenser.dispenseCash(amount);
        } catch (Exception e) {
            bankService.depositMoney(currentCard, amount); // Deposit back if dispensing fails
        }
    }

    public void depositCash(int amount) {
        bankService.depositMoney(currentCard, amount);
    }

    public Card getCurrentCard() {
        return currentCard;
    }

    public BankService getBankService() {
        return bankService;
    }
}


















import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ATMDemo {
    public static void main(String[] args) {
        ATM atm = ATM.getInstance();

        // Perform Check Balance operation
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.CHECK_BALANCE); // $1000

        // Perform Withdraw Cash operation
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.WITHDRAW_CASH, 570);

        // Perform Deposit Cash operation
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.DEPOSIT_CASH, 200);

        // Perform Check Balance operation
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.CHECK_BALANCE); // $630

        // Perform Withdraw Cash more than balance
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("1234");
        atm.selectOperation(OperationType.WITHDRAW_CASH, 700); // Insufficient balance

        // Insert Incorrect PIN
        atm.insertCard("1234-5678-9012-3456");
        atm.enterPin("3425");
    }
}





















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































