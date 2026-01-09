package shoppingcartsystem;

public class Item {

    private String itemId;
    private String name;
    private double price;

    public Item(String itemId,String name , double price){

        this.itemId = itemId;
        this.name = name;
        this.price = price;

    }

    //getters

    public String getItemId(){
        return this.itemId;
    }

    public String getName(){
        return this.name;
    }

    public double getPrice(){
        return this.price;
    }


    @Override
    public String toString(){
        return name + " " + price + " - " ;
    }
}
