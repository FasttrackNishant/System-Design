package models.commands;

public class AddEntryCommand implements  Command {

    // Singleton instance of DB

    private Database database;

    private  Entry entry;

    public AddEntryCommand(Database database, Entry entry) {
        this.database = database;
        this.entry = entry;
    }

    @Override
    public void execute() {
        database.addEntry(entry);
    }

}
