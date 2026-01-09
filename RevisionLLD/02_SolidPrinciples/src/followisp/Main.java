package followisp;

class Main {
    public static void main(String[] args) {
        Document editable = new EditableDocument("Draft proposal for Q3.");
        Document readOnly = new ReadOnlyDocument("Top secret strategy.");

        DocumentProcessor processor = new DocumentProcessor();

        System.out.println("--- Processing Editable Document ---");
        processor.processAndSave(editable, "Reviewed by Alice");

        System.out.println("\n--- Processing Read-Only Document ---");
        processor.process(readOnly); // This works fine
    }
}