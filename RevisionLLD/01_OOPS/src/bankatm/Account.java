package bankatm;

public interface Account {
    void depositMoney(int amount);
    void withdrawMoney(int amount);
    int getBalance();
}
