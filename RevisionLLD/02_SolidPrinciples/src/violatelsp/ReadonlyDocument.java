package violatelsp;

public class ReadonlyDocument extends Document{

    public ReadonlyDocument(String data){
        super(data);
    }

    @Override
    public void saveDocument(String data){
        throw new UnsupportedOperationException("File not Supported");
    }
}
