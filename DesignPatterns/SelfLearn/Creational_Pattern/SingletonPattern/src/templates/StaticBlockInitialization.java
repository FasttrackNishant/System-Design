package templates;

public class StaticBlockInitialization {
    private  static StaticBlockInitialization instance;

    private  StaticBlockInitialization(){}

    static {
        try
        {
            instance = new StaticBlockInitialization();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception Occured in creating singoleton instance");
        }
    }

    public  static StaticBlockInitialization getInstance()
    {
        return  instance;
    }
}
