package decorater;

public class Main {
    public static void main(String[] args) {
        TextView text = new PlainTextView("Hello, World!");

        System.out.print("Plain: ");
        text.render();
        System.out.println();

        System.out.print("Bold: ");
        TextView boldText = new BoldDecorater(text);
        boldText.render();
        System.out.println();

        System.out.print("Italic + Bold: ");
        TextView italicUnderline = new ItalicDecorater(new BoldDecorater(text));
        italicUnderline.render();
        System.out.println();

    }
}