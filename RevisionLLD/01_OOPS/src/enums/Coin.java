package enums;

public enum Coin{

    PENNY(1),
    HUND(100),
    THOUSAND(400);

    private final int value;

    Coin(int value)
    {
        this.value = value;
    }

    public int getValue(){
        return value;
    }
}