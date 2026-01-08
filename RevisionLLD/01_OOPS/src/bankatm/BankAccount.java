package bankatm;

public class BankAccount implements Account {

    private int balance;

    public BankAccount(int initialBalance) {
        if (initialBalance < 0) {
            throw new InvalidAmountException("Initial balance cannot be negative");
        }
        this.balance = initialBalance;
    }

    @Override
    public void depositMoney(int amount) {
        validateAmount(amount);
        balance += amount;
    }

    @Override
    public void withdrawMoney(int amount) {
        validateAmount(amount);

        if (amount > balance) {
            throw new InsufficientBalanceException(
                    "Withdrawal failed: insufficient balance"
            );
        }

        balance -= amount;
    }

    @Override
    public int getBalance() {
        return balance;
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "Amount must be greater than zero"
            );
        }
    }
}
