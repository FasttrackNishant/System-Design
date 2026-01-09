package singleton;

public class DoubleLockingSingletong {

    private static volatile DoubleLockingSingletong instance;

    private DoubleLockingSingletong() {}

    public static DoubleLockingSingletong getInstance(){

        if(instance == null){
            synchronized (DoubleLockingSingletong.class){
                if(instance == null){
                    instance = new DoubleLockingSingletong();
                }
            }
        }

        return instance;
    }
}
