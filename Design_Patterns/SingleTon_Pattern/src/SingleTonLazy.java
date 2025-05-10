public class SingleTonLazy {

    private SingleTonLazy() {
        System.out.println("Instance Created");
    }

    private static class Holder {
        private static final SingleTonLazy INSTANCE = new SingleTonLazy();

    }

    public static SingleTonLazy getInstance() {
        return Holder.INSTANCE;
    }

    public static void main(String[] args) {
        Thread th1 = new Thread(SingleTonLazy::getInstance);
        th1.start();
    }
}
