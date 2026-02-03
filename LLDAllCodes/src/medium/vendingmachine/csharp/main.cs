
class Inventory
{
    private readonly Dictionary<string, Item> itemMap = new Dictionary<string, Item>();
    private readonly Dictionary<string, int> stockMap = new Dictionary<string, int>();

    public void AddItem(string code, Item item, int quantity)
    {
        itemMap[code] = item;
        stockMap[code] = quantity;
    }

    public Item GetItem(string code)
    {
        return itemMap.GetValueOrDefault(code);
    }

    public bool IsAvailable(string code)
    {
        return stockMap.GetValueOrDefault(code, 0) > 0;
    }

    public void ReduceStock(string code)
    {
        if (stockMap.ContainsKey(code))
        {
            stockMap[code] = stockMap[code] - 1;
        }
    }
}






class Item
{
    private string code;
    private string name;
    private int price;

    public Item(string code, string name, int price)
    {
        this.code = code;
        this.name = name;
        this.price = price;
    }

    public string GetName()
    {
        return name;
    }

    public int GetPrice()
    {
        return price;
    }
}





enum Coin
{
    PENNY = 1,
    NICKEL = 5,
    DIME = 10,
    QUARTER = 25
}






class DispensingState : VendingMachineState
{
    public DispensingState(VendingMachine machine) : base(machine)
    {
    }

    public override void InsertCoin(Coin coin)
    {
        Console.WriteLine("Currently dispensing. Please wait.");
    }

    public override void SelectItem(string code)
    {
        Console.WriteLine("Currently dispensing. Please wait.");
    }

    public override void Dispense()
    {
        // already triggered by HasMoneyState
    }

    public override void Refund()
    {
        Console.WriteLine("Dispensing in progress. Refund not allowed.");
    }
}





class HasMoneyState : VendingMachineState
{
    public HasMoneyState(VendingMachine machine) : base(machine)
    {
    }

    public override void InsertCoin(Coin coin)
    {
        Console.WriteLine("Already received full amount.");
    }

    public override void SelectItem(string code)
    {
        Console.WriteLine("Item already selected.");
    }

    public override void Dispense()
    {
        machine.SetState(new DispensingState(machine));
        machine.DispenseItem();
    }

    public override void Refund()
    {
        machine.RefundBalance();
        machine.Reset();
        machine.SetState(new IdleState(machine));
    }
}





class IdleState : VendingMachineState
{
    public IdleState(VendingMachine machine) : base(machine)
    {
    }

    public override void InsertCoin(Coin coin)
    {
        Console.WriteLine("Please select an item before inserting money.");
    }

    public override void SelectItem(string code)
    {
        if (!machine.GetInventory().IsAvailable(code))
        {
            Console.WriteLine("Item not available.");
            return;
        }
        machine.SetSelectedItemCode(code);
        machine.SetState(new ItemSelectedState(machine));
        Console.WriteLine("Item selected: " + code);
    }

    public override void Dispense()
    {
        Console.WriteLine("No item selected.");
    }

    public override void Refund()
    {
        Console.WriteLine("No money to refund.");
    }
}





class ItemSelectedState : VendingMachineState
{
    public ItemSelectedState(VendingMachine machine) : base(machine)
    {
    }

    public override void InsertCoin(Coin coin)
    {
        machine.AddBalance((int)coin);
        Console.WriteLine("Coin Inserted: " + (int)coin);
        int price = machine.GetSelectedItem().GetPrice();
        if (machine.GetBalance() >= price)
        {
            Console.WriteLine("Sufficient money received.");
            machine.SetState(new HasMoneyState(machine));
        }
    }

    public override void SelectItem(string code)
    {
        Console.WriteLine("Item already selected.");
    }

    public override void Dispense()
    {
        Console.WriteLine("Please insert sufficient money.");
    }

    public override void Refund()
    {
        machine.Reset();
        machine.SetState(new IdleState(machine));
    }
}





abstract class VendingMachineState
{
    protected VendingMachine machine;

    public VendingMachineState(VendingMachine machine)
    {
        this.machine = machine;
    }

    public abstract void InsertCoin(Coin coin);
    public abstract void SelectItem(string code);
    public abstract void Dispense();
    public abstract void Refund();
}




class VendingMachine
{
    private static readonly VendingMachine INSTANCE = new VendingMachine();
    private readonly Inventory inventory = new Inventory();
    private VendingMachineState currentState;
    private int balance = 0;
    private string selectedItemCode;

    public VendingMachine()
    {
        currentState = new IdleState(this);
    }

    public static VendingMachine GetInstance()
    {
        return INSTANCE;
    }

    public void InsertCoin(Coin coin)
    {
        currentState.InsertCoin(coin);
    }

    public Item AddItem(string code, string name, int price, int quantity)
    {
        Item item = new Item(code, name, price);
        inventory.AddItem(code, item, quantity);
        return item;
    }

    public void SelectItem(string code)
    {
        currentState.SelectItem(code);
    }

    public void Dispense()
    {
        currentState.Dispense();
    }

    public void DispenseItem()
    {
        Item item = inventory.GetItem(selectedItemCode);
        if (balance >= item.GetPrice())
        {
            inventory.ReduceStock(selectedItemCode);
            balance -= item.GetPrice();
            Console.WriteLine("Dispensed: " + item.GetName());
            if (balance > 0)
            {
                Console.WriteLine("Returning change: " + balance);
            }
        }
        Reset();
        SetState(new IdleState(this));
    }

    public void RefundBalance()
    {
        Console.WriteLine("Refunding: " + balance);
        balance = 0;
    }

    public void Reset()
    {
        selectedItemCode = null;
        balance = 0;
    }

    public void AddBalance(int value)
    {
        balance += value;
    }

    public Item GetSelectedItem()
    {
        return inventory.GetItem(selectedItemCode);
    }

    public void SetSelectedItemCode(string code)
    {
        this.selectedItemCode = code;
    }

    public void SetState(VendingMachineState state)
    {
        this.currentState = state;
    }

    public Inventory GetInventory() { return inventory; }
    public int GetBalance() { return balance; }
}




using System;
using System.Collections.Generic;

public class VendingMachineDemo
{
    public static void Main()
    {
        VendingMachine vendingMachine = VendingMachine.GetInstance();

        // Add products to the inventory
        vendingMachine.AddItem("A1", "Coke", 25, 3);
        vendingMachine.AddItem("A2", "Pepsi", 25, 2);
        vendingMachine.AddItem("B1", "Water", 10, 5);

        // Select a product
        Console.WriteLine("\n--- Step 1: Select an item ---");
        vendingMachine.SelectItem("A1");

        // Insert coins
        Console.WriteLine("\n--- Step 2: Insert coins ---");
        vendingMachine.InsertCoin(Coin.DIME); // 10
        vendingMachine.InsertCoin(Coin.DIME); // 10
        vendingMachine.InsertCoin(Coin.NICKEL); // 5

        // Dispense the product
        Console.WriteLine("\n--- Step 3: Dispense item ---");
        vendingMachine.Dispense(); // Should dispense Coke

        // Select another item
        Console.WriteLine("\n--- Step 4: Select another item ---");
        vendingMachine.SelectItem("B1");

        // Insert more amount
        Console.WriteLine("\n--- Step 5: Insert more than needed ---");
        vendingMachine.InsertCoin(Coin.QUARTER); // 25

        // Try to dispense the product
        Console.WriteLine("\n--- Step 6: Dispense and return change ---");
        vendingMachine.Dispense();
    }
}












































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































