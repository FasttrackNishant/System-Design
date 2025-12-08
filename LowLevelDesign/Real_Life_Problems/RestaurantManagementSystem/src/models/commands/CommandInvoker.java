package models.commands;

public class CommandInvoker {

    public Command command;
    public CommandInvoker(Command command){
        this.command = command;
    }

    public  void executeCommand() {
        if (command != null) {
            command.execute();
        }
    }
}