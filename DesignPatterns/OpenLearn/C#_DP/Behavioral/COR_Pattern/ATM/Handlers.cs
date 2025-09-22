namespace COR_Pattern.ATM;

public class TwoThousandDispenser : CashDispenserHandler
{
    public override void Dispense(int amount)
    {
        if (amount >= 2000)
        {
            int count = amount / 2000;
            int remainder = amount % 2000;

            Console.WriteLine("Dispensing " + count + " x 2000 notes");

            if (remainder != 0 && nextHandler != null)
            {
                nextHandler.Dispense(remainder);
            }
        }
        else
        {
            nextHandler.Dispense(amount);
        }
    }
}

public class FiveHundredDispenser : CashDispenserHandler
{
    public override void Dispense(int amount)
    {
        if (amount >= 500)
        {
            int count = amount / 500;
            int remainder = amount % 500;
            Console.WriteLine("Dispensing " + count + " x 500 notes");

            if (remainder != 0 && nextHandler != null)
                nextHandler.Dispense(remainder);
        }
        else if (nextHandler != null)
        {
            nextHandler.Dispense(amount);
        }
    }
}

public class TwoHundredDispenser : CashDispenserHandler
{
    public override void Dispense(int amount)
    {
        if (amount >= 200)
        {
            int count = amount / 200;
            int remainder = amount % 200;
            Console.WriteLine("Dispensing " + count + " x 200 notes");

            if (remainder != 0 && nextHandler != null)
                nextHandler.Dispense(remainder);
        }
        else if (nextHandler != null)
        {
            nextHandler.Dispense(amount);
        }
    }
}

public class OneHundredDispenser : CashDispenserHandler
{
    public override void Dispense(int amount)
    {
        if (amount >= 100)
        {
            int count = amount / 100;
            int remainder = amount % 100;
            Console.WriteLine("Dispensing " + count + " x 100 notes");

            if (remainder != 0 && nextHandler != null)
                nextHandler.Dispense(remainder);
        }
        else if (nextHandler != null)
        {
            nextHandler.Dispense(amount);
        }
    }
}
