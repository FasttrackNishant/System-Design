namespace COR_Pattern.ATM;

public class ATM
{
    private CashDispenserHandler chain;
    
    public ATM()
    {
        CashDispenserHandler twoThousand = new TwoHundredDispenser();
        CashDispenserHandler fiveHundred = new FiveHundredDispenser();
        CashDispenserHandler twoHundred = new TwoHundredDispenser();
        CashDispenserHandler oneHundred = new OneHundredDispenser();

        twoThousand.SetNextHandler(fiveHundred);
        fiveHundred.SetNextHandler(twoHundred);
        twoHundred.SetNextHandler(oneHundred);

        this.chain = twoThousand;
    }
    
    public void Withdraw(int amount)
    {
        if (amount % 100 != 0)
        {
            Console.WriteLine("Amount should be multiple of 100");
            return;
        }

        chain.Dispense(amount);
    }
}