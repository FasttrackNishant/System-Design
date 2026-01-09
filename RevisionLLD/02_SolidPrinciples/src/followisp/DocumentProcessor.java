package followisp;

public class DocumentProcessor {

    // first way

    public void process(Document doc) {
        doc.open();
        System.out.println("Document processed.");
    }

    public void processAndSave(Document doc, String additionalInfo) {
        if (!(doc instanceof Editable editableDoc)) {
            throw new IllegalArgumentException("Document is not editable.");
        }

        doc.open();
        String currentData = doc.getData();
        String newData = currentData + " | Processed: " + additionalInfo;
        editableDoc.save(newData);
        System.out.println("Editable document processed and saved.");
    }

    void processEditableDocument(Editable editableDoc, Document doc, String additionalInfo) {
        doc.open();
        String currentData = doc.getData();
        String newData = currentData + " | Processed: " + additionalInfo;
        editableDoc.save(newData);
        System.out.println("Editable document processed and saved.");
    }
}
