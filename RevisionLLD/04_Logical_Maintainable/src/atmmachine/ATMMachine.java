package atmmachine;

// ===================== LOGGER =====================
class ATMLogger {
    static void info(String msg) {
        System.out.println("[INFO] " + msg);
    }

    static void error(String msg) {
        System.out.println("[ERROR] " + msg);
    }
}

// ===================== DOMAIN =====================
class Card {
    private final String cardNumber;
    private final String pin;

    public Card(String cardNumber, String pin) {
        this.cardNumber = cardNumber;
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }
}

class Account {
    private double balance;
    private final Card card;

    public Account(double balance, Card card) {
        this.balance = balance;
        this.card = card;
    }

    boolean validatePin(String pin) {
        return card.getPin().equals(pin);
    }

    synchronized boolean hasSufficientBalance(double amount) {
        return balance >= amount;
    }

    synchronized void debit(double amount) {
        balance -= amount;
    }

    synchronized void credit(double amount) {
        balance += amount;
    }

    synchronized double getBalance() {
        return balance;
    }
}

// ===================== CHAIN OF RESPONSIBILITY =====================
interface DispenseChain {
    void setNext(DispenseChain next);
    boolean canDispense(int amount);
    void dispense(int amount);
}

abstract class NoteDispenser implements DispenseChain {
    protected DispenseChain next;
    protected int noteValue;
    protected int noteCount;

    public NoteDispenser(int noteValue, int noteCount) {
        this.noteValue = noteValue;
        this.noteCount = noteCount;
    }

    public void setNext(DispenseChain next) {
        this.next = next;
    }

    public boolean canDispense(int amount) {
        int used = Math.min(amount / noteValue, noteCount);
        int remaining = amount - used * noteValue;
        if (remaining == 0) return true;
        return next != null && next.canDispense(remaining);
    }

    public void dispense(int amount) {
        int used = Math.min(amount / noteValue, noteCount);
        if (used > 0) {
            noteCount -= used;
            ATMLogger.info("Dispensing " + used + " x " + noteValue);
        }
        int remaining = amount - used * noteValue;
        if (remaining > 0 && next != null) {
            next.dispense(remaining);
        }
    }
}

class Dispenser2000 extends NoteDispenser {
    public Dispenser2000(int count) { super(2000, count); }
}
class Dispenser500 extends NoteDispenser {
    public Dispenser500(int count) { super(500, count); }
}
class Dispenser100 extends NoteDispenser {
    public Dispenser100(int count) { super(100, count); }
}

// ===================== STATE =====================
interface ATMState {
    void insertCard(Card card);
    void enterPin(String pin);
    void checkBalance();
    void withdraw(int amount);
    void deposit(int amount);
    void ejectCard();
}

// ===================== STATES =====================
class IdleState implements ATMState {
    private final ATMMachine atm;

    public IdleState(ATMMachine atm) {
        this.atm = atm;
    }

    public void insertCard(Card card) {
        ATMLogger.info("Card inserted");
        atm.setState(new HasCardState(atm));
    }

    public void enterPin(String pin) { ATMLogger.error("Insert card first"); }
    public void checkBalance() { ATMLogger.error("Insert card first"); }
    public void withdraw(int amount) { ATMLogger.error("Insert card first"); }
    public void deposit(int amount) { ATMLogger.error("Insert card first"); }
    public void ejectCard() { ATMLogger.error("No card to eject"); }
}

class HasCardState implements ATMState {
    private final ATMMachine atm;

    public HasCardState(ATMMachine atm) {
        this.atm = atm;
    }

    public void insertCard(Card card) {
        ATMLogger.error("Card already inserted");
    }

    public void enterPin(String pin) {
        if (atm.validatePin(pin)) {
            ATMLogger.info("PIN authenticated");
            atm.setState(new AuthenticatedState(atm));
        } else {
            ATMLogger.error("Invalid PIN");
        }
    }

    public void checkBalance() { ATMLogger.error("Enter PIN first"); }
    public void withdraw(int amount) { ATMLogger.error("Enter PIN first"); }
    public void deposit(int amount) { ATMLogger.error("Enter PIN first"); }

    public void ejectCard() {
        ATMLogger.info("Card ejected");
        atm.setState(new IdleState(atm));
    }
}

class AuthenticatedState implements ATMState {
    private final ATMMachine atm;

    public AuthenticatedState(ATMMachine atm) {
        this.atm = atm;
    }

    public void insertCard(Card card) {
        ATMLogger.error("Session already active");
    }

    public void enterPin(String pin) {
        ATMLogger.error("Already authenticated");
    }

    public void checkBalance() {
        ATMLogger.info("Balance: " + atm.getBalanceInternal());
    }

    public void withdraw(int amount) {
        atm.processWithdrawalInternal(amount);
    }

    public void deposit(int amount) {
        atm.processDepositInternal(amount);
    }

    public void ejectCard() {
        ATMLogger.info("Card ejected");
        atm.setState(new IdleState(atm));
    }
}

// ===================== FACADE =====================
class ATMMachine {

    private ATMState currentState;
    private final Account account;
    private final DispenseChain dispenser;

    public ATMMachine(Account account) {
        this.account = account;
        this.currentState = new IdleState(this);

        DispenseChain d2000 = new Dispenser2000(10);
        DispenseChain d500 = new Dispenser500(10);
        DispenseChain d100 = new Dispenser100(10);

        d2000.setNext(d500);
        d500.setNext(d100);

        this.dispenser = d2000;
    }

    void setState(ATMState state) {
        this.currentState = state;
    }

    boolean validatePin(String pin) {
        return account.validatePin(pin);
    }

    double getBalanceInternal() {
        return account.getBalance();
    }

    void processWithdrawalInternal(int amount) {
        if (!account.hasSufficientBalance(amount)) {
            ATMLogger.error("Insufficient balance");
            return;
        }
        if (!dispenser.canDispense(amount)) {
            ATMLogger.error("ATM out of cash");
            return;
        }
        account.debit(amount);
        dispenser.dispense(amount);
        ATMLogger.info("Please collect your cash");
    }

    void processDepositInternal(int amount) {
        account.credit(amount);
        ATMLogger.info("Deposit successful: " + amount);
    }

    // Public API
    public void insertCard(Card card) { currentState.insertCard(card); }
    public void enterPin(String pin) { currentState.enterPin(pin); }
    public void checkBalance() { currentState.checkBalance(); }
    public void withdraw(int amount) { currentState.withdraw(amount); }
    public void deposit(int amount) { currentState.deposit(amount); }
    public void ejectCard() { currentState.ejectCard(); }
}

// ===================== CLIENT =====================
 class Main {
    public static void main(String[] args) {

        Card card = new Card("123456", "1010");
        Account account = new Account(10000, card);

        ATMMachine atm = new ATMMachine(account);

        atm.insertCard(card);
        atm.enterPin("1010");
        atm.checkBalance();
        atm.deposit(2000);
        atm.checkBalance();
        atm.withdraw(38700);
        atm.checkBalance();
        atm.ejectCard();
    }
}
