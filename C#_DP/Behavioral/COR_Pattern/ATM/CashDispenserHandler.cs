namespace COR_Pattern.ATM;

public abstract class CashDispenserHandler
{
    protected CashDispenserHandler nextHandler;

    public void SetNextHandler(CashDispenserHandler cashDispenseHandler)
    {
        this.nextHandler = cashDispenseHandler;
    }

    public abstract void Dispense(int amount);
    
}