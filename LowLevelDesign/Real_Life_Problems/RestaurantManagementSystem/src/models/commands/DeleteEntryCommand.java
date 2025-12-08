package models.commands;

public class DeleteEntryCommand implements Command{

    private Database database;
    private  Entry entry;

    public DeleteEntryCommand(Database database, Entry entry) {
        this.database = database;
        this.entry = entry;
    }

    @Override
    public void execute() {
        database.deleteEntry(entry);
    }
}
