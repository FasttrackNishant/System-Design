public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        Scorpio scorpio = new Scorpio();
        Scorpio advanceScorpio = new AdvanceScorpio();
        StandardBreak standardBreak = new StandardBreak();
        StandardBreak advanceBreak = new AdvanceBreak();

//        scorpio.applyBreak(standardBreak);
//        advanceScorpio.applyBreak(standardBreak);

        scorpio.applyBreak(advanceBreak);
        advanceScorpio.applyBreak(advanceBreak);

    }
}

class StandardBreak {
    public String playSound() {
        return "Normal Break ki Sound";
    }
}

class AdvanceBreak extends StandardBreak {
    @Override
    public String playSound() {
        return "Adv Break ki Sound";
    }
}

class AdvanceScorpio extends Scorpio {

    @Override
    public String whatMyName() {
        return "Advanced Scorpio";
    }
}


class Scorpio {
    public String whatMyName() {
        return "Scorpio";
    }

    public void applyBreak(StandardBreak standardBreak) {
        System.out.println(whatMyName() + " Applying Normal Break" + standardBreak.playSound());
    }

    public void applyAdvanceBreak(AdvanceBreak advanceBreak) {
        System.out.println(whatMyName() + "Applying Advanced Break" + advanceBreak.playSound());
    }
}