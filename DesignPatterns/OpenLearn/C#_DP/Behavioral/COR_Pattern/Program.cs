namespace COR_Pattern;

class Program
{
    static void Main(string[] args)
    {
        ATM.ATM atm = new ATM.ATM();

        Console.WriteLine("Withdraw 3700:");
        atm.Withdraw(3700);

        Console.WriteLine("\nWithdraw 800:");
        atm.Withdraw(800);
    }
}