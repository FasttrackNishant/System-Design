package violatelsp;



public class DocumentProcessor{

    public void processAndSave(Document document , String data){
        // open
        document.openDocument();

        // put data
        document.saveDocument(data);

        // close file
        document.closeDocument();
    }

}
