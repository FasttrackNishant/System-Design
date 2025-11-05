public class BulletProof extends  ScorpioDecorater{

    ICar scorpio;

    public BulletProof(ICar meriScorpio)
    {
        // yaha ref copy ho raha hain
        this.scorpio = meriScorpio;
    }

    @Override
    public void start() {
        scorpio.start();
    }

    @Override
    public void stop() {
        scorpio.stop();
    }

    @Override
    public float getWeight() {
        return 300f + scorpio.getWeight();
    }
}
