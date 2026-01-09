package violatelsp;


public class Main {

    public static void main(String[] args) {

        Document landPaper = new Document("This is Land Paper");
        Document readonlyDocument = new ReadonlyDocument("This is read only document");

        DocumentProcessor docProcessor = new DocumentProcessor();
        docProcessor.processAndSave(landPaper,"Signed by Nishant");

        try {
            docProcessor.processAndSave(readonlyDocument, "This is New Readonly");
        }catch (UnsupportedOperationException ex){
            System.out.println("Action is Unsupported");
        }
    }
}