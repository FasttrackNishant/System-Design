package medium.splitwise;

import javax.swing.*;
import java.sql.DriverPropertyInfo;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;

enum SplitType {
    EXACT,
    PERCENTAGE,
    public User(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.balanceSheet = new BalanceSheet(this);
    }

    public BalanceSheet getBalanceSheet() {
        return balanceSheet;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setBalanceSheet(BalanceSheet balanceSheet) {
        this.balanceSheet = balanceSheet;
    }
}

class Group {

    private String id;
    private String name;
    private List<User> members;

    public Group(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.members = new ArrayList<>();
    }

    public void addMemeber(User newUser) {
        members.add(newUser);
    }
}

class Split {
    private User user;
    private double amount;

    public Split(User user, double amount) {
        this.user = user;
        this.amount = amount;
    }

    public User getUser() {
        return user;
    }

    public double getAmount() {
        return amount;
    }
}

class Expense {
    private String id;
    private String name;
    private String description;
    private double totalAmount;
    private SplitType splitType;
    private List<Split> splitList;
    private User paidBy;
    private LocalDateTime timestamp;

    public Expense(String name, String description, double totalAmount, User owner, SplitType type) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.totalAmount = totalAmount;
        this.paidBy = owner;
        this.splitList = new ArrayList<>();
        this.splitType = type;
    }

    public void addSplits(List<Split> splitList) {
        this.splitList = splitList;
    }


}

class BalanceSheet {

    private User owner;
    private Map<User, Double> balances = new ConcurrentHashMap<>();

    public BalanceSheet(User owner) {
        this.owner = owner;
    }

    public synchronized void adjustBalance(User otherUser, double amount) {
        Double currentBal = getCurrentBalance(otherUser);

        if (currentBal == null) {
            balances.put(otherUser, amount);
        } else {
            balances.put(otherUser, amount + currentBal);
        }
    }

    private Double getCurrentBalance(User user) {
        return balances.get(user);
    }

    public void showBalances() {
        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            System.out.println("User :-> " + entry.getKey().getName() + " Balance :- " + entry.getValue());
        }
    }

}

interface SplitStrategy {
    List<Split> calculateSplit(double amount, List<User> users, List<Double> splitvalues);
}

class ExactSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> calculateSplit(double amount, List<User> users, List<Double> splitvalues) {
        return null;

    }
}

class PerecentageSplitStrategy implements SplitStrategy {


    @Override
    public List<Split> calculateSplit(double amount, List<User> users, List<Double> splitvalues) {
        return null;

    }
}

class EqualSplitStrategy implements SplitStrategy {


    @Override
    public List<Split> calculateSplit(double amount, List<User> users, List<Double> splitvalues) {

        int size = users.size();

        List<Split> splitList = new ArrayList<>();
        double contribution = amount / size;

        for (User user : users) {
            splitList.add(new Split(user, contribution));
        }
        return splitList;
    }
}

class SplitFactory {

    public static SplitStrategy getSplitStrategy(SplitType type) {

        return switch (type) {
            case EXACT -> new ExactSplitStrategy();
            case EQUAL -> new EqualSplitStrategy();
            case PERCENTAGE -> new PerecentageSplitStrategy();
            default -> throw new IllegalArgumentException("No Valid Split Type");
        };
    }
}

class SplitWiseService {

    private static SplitWiseService instance;
    private Map<String, User> users = new ConcurrentHashMap<>();
    private Map<String, Group> groups = new ConcurrentHashMap<>();

    private SplitWiseService() {
    }

    public static synchronized SplitWiseService getInstance() {
        if (instance == null) {
            instance = new SplitWiseService();
        }

        return instance;
    }

    public void addUser(User newUser) {
        users.put(newUser.getId(), newUser);
    }

    public void createExpense(String name, String desc, double amount, User paidBy, SplitType type,
                              List<User> participants, List<Double> splitValues) {

        List<Split> splitList = createSplits(amount, type, splitValues, participants);
        Expense expense = new Expense(name, desc, amount, paidBy, type);
        expense.addSplits(splitList);

        for (Split split : splitList) {
            User participant = split.getUser();
            double userAmount = split.getAmount();

            if (!paidBy.equals(participant)) {
                paidBy.getBalanceSheet().adjustBalance(participant, userAmount);
                participant.getBalanceSheet().adjustBalance(paidBy, -userAmount);
            }
        }

        System.out.println("Expense created successfully");

    }

    public void showBalanceSheet(User user) {
        BalanceSheet balanceSheet = user.getBalanceSheet();
        balanceSheet.showBalances();
    }


    private List<Split> createSplits(double amount, SplitType type, List<Double> splitValues, List<User> users) {

        SplitStrategy splitStrategy = SplitFactory.getSplitStrategy(type);
        return splitStrategy.calculateSplit(amount, users, splitValues);
    }

}

class Main {
    public static void main(String[] args) {

        SplitWiseService service = SplitWiseService.getInstance();

        User alice = new User("Alice");
        User bob = new User("bob");

        service.addUser(alice);
        service.addUser(bob);
        service.createExpense("Trip", "Goa Trip", 500, alice, SplitType.EQUAL, Arrays.asList(alice, bob), null);


        service.showBalanceSheet(bob);

    }
}

