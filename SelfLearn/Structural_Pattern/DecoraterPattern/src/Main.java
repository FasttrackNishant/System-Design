import vehicle.BulletProofDecorator;
import vehicle.Car;
import vehicle.Scorpio;
import wordrender.*;

public class Main {
    public static void main1(String[] args) {

        TextView text = new PlainTextView("Hello World");

        System.out.print("Plain: ");
        text.render();
        System.out.println();

        System.out.print("Bold: ");
        TextView boldText = new BoldDecorater(text);
        boldText.render();
        System.out.println();

        System.out.print("Italic + Underline: ");
        TextView italicUnderline = new UnderlineDecorator(new ItalicDecorater(text));
        italicUnderline.render();
        System.out.println();

        System.out.print("Bold + Italic + Underline: ");
        TextView allStyles = new UnderlineDecorator(new ItalicDecorater(new BoldDecorater(text)));
        allStyles.render();
        System.out.println();
    }

    public static void main(String[] args) {

        Car scorpio = new Scorpio();
        System.out.println(scorpio.getWeight());

        Car bulletProofScorpio = new BulletProofDecorator(scorpio);
        bulletProofScorpio.start();



    }
}