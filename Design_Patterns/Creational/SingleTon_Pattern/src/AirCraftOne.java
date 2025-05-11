public class AirCraftOne {

    // ye singleton pattern hain
    public static AirCraftOne airCraftInstance;

    private AirCraftOne() {

    }

    // ye code ensure kar lega ki ek hi object bane
    public static AirCraftOne getInstance() {

        if(airCraftInstance==null)
        {
            airCraftInstance = new AirCraftOne();
        }
        return  airCraftInstance;

    }
}
