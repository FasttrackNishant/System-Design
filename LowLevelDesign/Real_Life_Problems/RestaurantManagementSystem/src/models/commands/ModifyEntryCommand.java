package models.commands;

public class ModifyEntryCommand implements Command{

    private Database database;
    private  Entry entry;

    public ModifyEntryCommand(Database database, Entry entry) {
        this.database = database;
        this.entry = entry;
    }

    @Override
    public void execute() {
        database.modifyEntry(entry);
    }

}
