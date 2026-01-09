package violatelsp;

public class Document {

    private String data;

    public Document(String data){
        this.data = data;
    }

    public void openDocument(){
        System.out.println("Document Opened"+ this.data);
    }

    public void closeDocument(){
        System.out.println("Document Closed" + this.data);
    }

    public void saveDocument(String data){

        this.data += data;
        System.out.println("Doc is Saved");
    }
}
