package templates;

public class ThreadSafeSingleton {

    private  static  ThreadSafeSingleton instance;

    private  ThreadSafeSingleton(){}

    // by sync keword
    public  static  synchronized ThreadSafeSingleton getInstance()
    {
        if(instance== null)
        {
            instance = new ThreadSafeSingleton();
        }

        return instance;
    }

}

