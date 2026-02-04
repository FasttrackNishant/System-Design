class CaramelSyrupDecorator : CoffeeDecorator
{
    private const int COST = 30;
    private static readonly Dictionary<Ingredient, int> RECIPE_ADDITION = 
        new Dictionary<Ingredient, int> { { Ingredient.CARAMEL_SYRUP, 10 } };

    public CaramelSyrupDecorator(Coffee coffee) : base(coffee) { }

    public override string GetCoffeeType()
    {
        return decoratedCoffee.GetCoffeeType() + ", Caramel Syrup";
    }

    public override int GetPrice()
    {
        return decoratedCoffee.GetPrice() + COST;
    }

    public override Dictionary<Ingredient, int> GetRecipe()
    {
        var newRecipe = new Dictionary<Ingredient, int>(decoratedCoffee.GetRecipe());
        foreach (var pair in RECIPE_ADDITION)
        {
            if (newRecipe.ContainsKey(pair.Key))
                newRecipe[pair.Key] += pair.Value;
            else
                newRecipe[pair.Key] = pair.Value;
        }
        return newRecipe;
    }

    public override void Prepare()
    {
        base.Prepare();
        Console.WriteLine("- Drizzling Caramel Syrup on top.");
    }
}






abstract class CoffeeDecorator : Coffee
{
    protected Coffee decoratedCoffee;

    public CoffeeDecorator(Coffee coffee)
    {
        decoratedCoffee = coffee;
    }

    public override int GetPrice()
    {
        return decoratedCoffee.GetPrice();
    }

    public override Dictionary<Ingredient, int> GetRecipe()
    {
        return decoratedCoffee.GetRecipe();
    }

    public override void AddCondiments()
    {
        decoratedCoffee.AddCondiments();
    }

    public override void Prepare()
    {
        decoratedCoffee.Prepare();
    }
}







class ExtraSugarDecorator : CoffeeDecorator
{
    private const int COST = 10;
    private static readonly Dictionary<Ingredient, int> RECIPE_ADDITION = 
        new Dictionary<Ingredient, int> { { Ingredient.SUGAR, 1 } };

    public ExtraSugarDecorator(Coffee coffee) : base(coffee) { }

    public override string GetCoffeeType()
    {
        return decoratedCoffee.GetCoffeeType() + ", Extra Sugar";
    }

    public override int GetPrice()
    {
        return decoratedCoffee.GetPrice() + COST;
    }

    public override Dictionary<Ingredient, int> GetRecipe()
    {
        var newRecipe = new Dictionary<Ingredient, int>(decoratedCoffee.GetRecipe());
        foreach (var pair in RECIPE_ADDITION)
        {
            if (newRecipe.ContainsKey(pair.Key))
                newRecipe[pair.Key] += pair.Value;
            else
                newRecipe[pair.Key] = pair.Value;
        }
        return newRecipe;
    }

    public override void Prepare()
    {
        base.Prepare();
        Console.WriteLine("- Stirring in Extra Sugar.");
    }
}








enum CoffeeType
{
    ESPRESSO,
    LATTE,
    CAPPUCCINO
}



enum Ingredient
{
    COFFEE_BEANS,
    MILK,
    SUGAR,
    WATER,
    CARAMEL_SYRUP
}



enum ToppingType
{
    EXTRA_SUGAR,
    CARAMEL_SYRUP
}








class CoffeeFactory
{
    public static Coffee CreateCoffee(CoffeeType type)
    {
        switch (type)
        {
            case CoffeeType.ESPRESSO:
                return new Espresso();
            case CoffeeType.LATTE:
                return new Latte();
            case CoffeeType.CAPPUCCINO:
                return new Cappuccino();
            default:
                throw new ArgumentException($"Unsupported coffee type: {type}");
        }
    }
}




interface IVendingMachineState
{
    void SelectCoffee(CoffeeVendingMachine machine, Coffee coffee);
    void InsertMoney(CoffeeVendingMachine machine, int amount);
    void DispenseCoffee(CoffeeVendingMachine machine);
    void Cancel(CoffeeVendingMachine machine);
}




class OutOfIngredientState : IVendingMachineState
{
    public void SelectCoffee(CoffeeVendingMachine machine, Coffee coffee)
    {
        Console.WriteLine("Sorry, we are sold out.");
    }

    public void InsertMoney(CoffeeVendingMachine machine, int amount)
    {
        Console.WriteLine("Sorry, we are sold out. Money refunded.");
    }

    public void DispenseCoffee(CoffeeVendingMachine machine)
    {
        Console.WriteLine("Sorry, we are sold out.");
    }

    public void Cancel(CoffeeVendingMachine machine)
    {
        Console.WriteLine($"Refunding {machine.GetMoneyInserted()}");
        machine.Reset();
        machine.SetState(new ReadyState());
    }
}






class PaidState : IVendingMachineState
{
    public void SelectCoffee(CoffeeVendingMachine machine, Coffee coffee)
    {
        Console.WriteLine("Already paid. Please dispense or cancel.");
    }

    public void InsertMoney(CoffeeVendingMachine machine, int amount)
    {
        machine.SetMoneyInserted(machine.GetMoneyInserted() + amount);
        Console.WriteLine($"Additional {amount} inserted. Total: {machine.GetMoneyInserted()}");
    }

    public void DispenseCoffee(CoffeeVendingMachine machine)
    {
        Inventory inventory = Inventory.GetInstance();
        Coffee coffee = machine.GetSelectedCoffee();

        if (!inventory.HasIngredients(coffee.GetRecipe()))
        {
            Console.WriteLine("Sorry, we are out of ingredients. Refunding your money.");
            Console.WriteLine($"Refunding {machine.GetMoneyInserted()}");
            machine.Reset();
            machine.SetState(new OutOfIngredientState());
            return;
        }

        // Deduct ingredients and prepare coffee
        inventory.DeductIngredients(coffee.GetRecipe());
        coffee.Prepare();

        // Calculate change
        int change = machine.GetMoneyInserted() - coffee.GetPrice();
        if (change > 0)
        {
            Console.WriteLine($"Here's your change: {change}");
        }

        machine.Reset();
        machine.SetState(new ReadyState());
    }

    public void Cancel(CoffeeVendingMachine machine)
    {
        Console.WriteLine($"Transaction cancelled. Refunding {machine.GetMoneyInserted()}");
        machine.Reset();
        machine.SetState(new ReadyState());
    }
}





class ReadyState : IVendingMachineState
{
    public void SelectCoffee(CoffeeVendingMachine machine, Coffee coffee)
    {
        machine.SetSelectedCoffee(coffee);
        machine.SetState(new SelectingState());
        Console.WriteLine($"{coffee.GetCoffeeType()} selected. Price: {coffee.GetPrice()}");
    }

    public void InsertMoney(CoffeeVendingMachine machine, int amount)
    {
        Console.WriteLine("Please select a coffee first.");
    }

    public void DispenseCoffee(CoffeeVendingMachine machine)
    {
        Console.WriteLine("Please select and pay first.");
    }

    public void Cancel(CoffeeVendingMachine machine)
    {
        Console.WriteLine("Nothing to cancel.");
    }
}






class SelectingState : IVendingMachineState
{
    public void SelectCoffee(CoffeeVendingMachine machine, Coffee coffee)
    {
        Console.WriteLine("Already selected. Please pay or cancel.");
    }

    public void InsertMoney(CoffeeVendingMachine machine, int amount)
    {
        machine.SetMoneyInserted(machine.GetMoneyInserted() + amount);
        Console.WriteLine($"Inserted {amount}. Total: {machine.GetMoneyInserted()}");
        if (machine.GetMoneyInserted() >= machine.GetSelectedCoffee().GetPrice())
        {
            machine.SetState(new PaidState());
        }
    }

    public void DispenseCoffee(CoffeeVendingMachine machine)
    {
        Console.WriteLine("Please insert enough money first.");
    }

    public void Cancel(CoffeeVendingMachine machine)
    {
        Console.WriteLine($"Transaction cancelled. Refunding {machine.GetMoneyInserted()}");
        machine.Reset();
        machine.SetState(new ReadyState());
    }
}











class Cappuccino : Coffee
{
    public Cappuccino()
    {
        coffeeType = "Cappuccino";
    }

    public override void AddCondiments()
    {
        Console.WriteLine("- Adding steamed milk and foam.");
    }

    public override int GetPrice()
    {
        return 250;
    }

    public override Dictionary<Ingredient, int> GetRecipe()
    {
        return new Dictionary<Ingredient, int>
        {
            { Ingredient.COFFEE_BEANS, 7 },
            { Ingredient.WATER, 30 },
            { Ingredient.MILK, 100 }
        };
    }
}






abstract class Coffee
{
    protected string coffeeType = "Unknown Coffee";

    public virtual string GetCoffeeType()
    {
        return coffeeType;
    }

    // The Template Method
    public virtual void Prepare()
    {
        Console.WriteLine($"\nPreparing your {GetCoffeeType()}...");
        GrindBeans();
        Brew();
        AddCondiments(); // The "hook" for base coffee types
        PourIntoCup();
        Console.WriteLine($"{GetCoffeeType()} is ready!");
    }

    // Common steps
    private void GrindBeans() { Console.WriteLine("- Grinding fresh coffee beans."); }
    private void Brew() { Console.WriteLine("- Brewing coffee with hot water."); }
    private void PourIntoCup() { Console.WriteLine("- Pouring into a cup."); }

    // Abstract step to be implemented by subclasses
    public abstract void AddCondiments();

    public abstract int GetPrice();
    public abstract Dictionary<Ingredient, int> GetRecipe();
}








class Espresso : Coffee
{
    public Espresso()
    {
        coffeeType = "Espresso";
    }

    public override void AddCondiments()
    {
        // No extra condiments for espresso
    }

    public override int GetPrice()
    {
        return 150;
    }

    public override Dictionary<Ingredient, int> GetRecipe()
    {
        return new Dictionary<Ingredient, int>
        {
            { Ingredient.COFFEE_BEANS, 7 },
            { Ingredient.WATER, 30 }
        };
    }
}






class Latte : Coffee
{
    public Latte()
    {
        coffeeType = "Latte";
    }

    public override void AddCondiments()
    {
        Console.WriteLine("- Adding steamed milk.");
    }

    public override int GetPrice()
    {
        return 220;
    }

    public override Dictionary<Ingredient, int> GetRecipe()
    {
        return new Dictionary<Ingredient, int>
        {
            { Ingredient.COFFEE_BEANS, 7 },
            { Ingredient.WATER, 30 },
            { Ingredient.MILK, 150 }
        };
    }
}










class CoffeeVendingMachine
{
    private static CoffeeVendingMachine instance;
    private static readonly object lockObject = new object();
    private IVendingMachineState state;
    private Coffee selectedCoffee;
    private int moneyInserted;

    private CoffeeVendingMachine()
    {
        state = new ReadyState();
        moneyInserted = 0;
    }

    public static CoffeeVendingMachine GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new CoffeeVendingMachine();
                }
            }
        }
        return instance;
    }

    public void SelectCoffee(CoffeeType type, List<ToppingType> toppings)
    {
        // 1. Create the base coffee using the factory
        Coffee coffee = CoffeeFactory.CreateCoffee(type);

        // 2. Wrap it with decorators
        foreach (ToppingType topping in toppings)
        {
            switch (topping)
            {
                case ToppingType.EXTRA_SUGAR:
                    coffee = new ExtraSugarDecorator(coffee);
                    break;
                case ToppingType.CARAMEL_SYRUP:
                    coffee = new CaramelSyrupDecorator(coffee);
                    break;
            }
        }
        // Let the state handle the rest
        state.SelectCoffee(this, coffee);
    }

    public void InsertMoney(int amount) { state.InsertMoney(this, amount); }
    public void DispenseCoffee() { state.DispenseCoffee(this); }
    public void Cancel() { state.Cancel(this); }

    // Getters and Setters used by State objects
    public void SetState(IVendingMachineState state) { this.state = state; }
    public IVendingMachineState GetState() { return state; }
    public void SetSelectedCoffee(Coffee selectedCoffee) { this.selectedCoffee = selectedCoffee; }
    public Coffee GetSelectedCoffee() { return selectedCoffee; }
    public void SetMoneyInserted(int moneyInserted) { this.moneyInserted = moneyInserted; }
    public int GetMoneyInserted() { return moneyInserted; }

    public void Reset()
    {
        selectedCoffee = null;
        moneyInserted = 0;
    }
}











using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

public class CoffeeVendingMachineDemo
{
    public static void Main(string[] args)
    {
        CoffeeVendingMachine machine = CoffeeVendingMachine.GetInstance();
        Inventory inventory = Inventory.GetInstance();

        // Initial setup: Refill inventory
        Console.WriteLine("=== Initializing Vending Machine ===");
        inventory.AddStock(Ingredient.COFFEE_BEANS, 50);
        inventory.AddStock(Ingredient.WATER, 500);
        inventory.AddStock(Ingredient.MILK, 200);
        inventory.AddStock(Ingredient.SUGAR, 100);
        inventory.AddStock(Ingredient.CARAMEL_SYRUP, 50);
        inventory.PrintInventory();

        // Scenario 1: Successful Purchase of a Latte
        Console.WriteLine("\n--- SCENARIO 1: Buy a Latte (Success) ---");
        machine.SelectCoffee(CoffeeType.LATTE, new List<ToppingType>());
        machine.InsertMoney(200);
        machine.InsertMoney(50); // Total 250, price is 220
        machine.DispenseCoffee();
        inventory.PrintInventory();

        // Scenario 2: Purchase with Insufficient Funds & Cancellation
        Console.WriteLine("\n--- SCENARIO 2: Buy Espresso (Insufficient Funds & Cancel) ---");
        machine.SelectCoffee(CoffeeType.ESPRESSO, new List<ToppingType>());
        machine.InsertMoney(100); // Price is 150
        machine.DispenseCoffee(); // Should fail
        machine.Cancel(); // Should refund 100
        inventory.PrintInventory(); // Should be unchanged

        // Scenario 3: Attempt to Buy with Insufficient Ingredients
        Console.WriteLine("\n--- SCENARIO 3: Buy Cappuccino (Out of Milk) ---");
        inventory.PrintInventory();
        machine.SelectCoffee(CoffeeType.CAPPUCCINO, new List<ToppingType> { ToppingType.CARAMEL_SYRUP, ToppingType.EXTRA_SUGAR });
        machine.InsertMoney(300);
        machine.DispenseCoffee(); // Should fail and refund
        inventory.PrintInventory();

        // Refill and final test
        Console.WriteLine("\n--- REFILLING AND FINAL TEST ---");
        inventory.AddStock(Ingredient.MILK, 200);
        inventory.PrintInventory();
        machine.SelectCoffee(CoffeeType.LATTE, new List<ToppingType> { ToppingType.CARAMEL_SYRUP });
        machine.InsertMoney(250);
        machine.DispenseCoffee();
        inventory.PrintInventory();
    }
}









class Inventory
{
    private static Inventory instance;
    private static readonly object lockObject = new object();
    private readonly Dictionary<Ingredient, int> stock = new Dictionary<Ingredient, int>();
    private readonly object inventoryLock = new object();

    private Inventory() { }

    public static Inventory GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                {
                    instance = new Inventory();
                }
            }
        }
        return instance;
    }

    public void AddStock(Ingredient ingredient, int quantity)
    {
        if (stock.ContainsKey(ingredient))
            stock[ingredient] += quantity;
        else
            stock[ingredient] = quantity;
    }

    public bool HasIngredients(Dictionary<Ingredient, int> recipe)
    {
        return recipe.All(pair => stock.GetValueOrDefault(pair.Key, 0) >= pair.Value);
    }

    public void DeductIngredients(Dictionary<Ingredient, int> recipe)
    {
        lock (inventoryLock)
        {
            if (!HasIngredients(recipe))
            {
                Console.WriteLine("Not enough ingredients to make coffee.");
                return;
            }
            foreach (var pair in recipe)
            {
                stock[pair.Key] -= pair.Value;
            }
        }
    }

    public void PrintInventory()
    {
        Console.WriteLine("--- Current Inventory ---");
        foreach (var pair in stock)
        {
            Console.WriteLine($"{pair.Key}: {pair.Value}");
        }
        Console.WriteLine("-------------------------");
    }
}


























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































