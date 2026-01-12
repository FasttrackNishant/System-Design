package behavioural.command.solution;

// Command Design Pattern


import java.util.Stack;

// Commands
interface Command {

    void execute();

    void undo();

}


// Receiver

class Light {
    public void on() {
        System.out.println("Light turned ON");
    }

    public void off() {
        System.out.println("Light turned OFF");
    }
}

class Thermostat {
    private int currentTemperature = 20; // default

    public void setTemperature(int temp) {
        System.out.println("Thermostat set to " + temp + "°C");
        currentTemperature = temp;
    }

    public int getCurrentTemperature() {
        return currentTemperature;
    }
}

// Concrete Command

class LightOnCommand implements Command {

    private final Light light;

    public LightOnCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.on();
    }

    @Override
    public void undo() {
        light.off();
    }
}

class LightOffCommand implements Command {
    private final Light light;

    public LightOffCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.off();
    }

    @Override
    public void undo() {
        light.on();
    }
}

class SetTemperatureCommand implements Command {
    private final Thermostat thermostat;
    private final int newTemperature;
    private int previousTemperature;

    public SetTemperatureCommand(Thermostat thermostat, int temperature) {
        this.thermostat = thermostat;
        this.newTemperature = temperature;
    }

    @Override
    public void execute() {
        previousTemperature = thermostat.getCurrentTemperature();
        thermostat.setTemperature(newTemperature);
    }

    @Override
    public void undo() {
        thermostat.setTemperature(previousTemperature);
    }
}


// Invoker

class SmartButton {

    private Command currentCommand;
    private Stack<Command> history = new Stack<>();

    public void setCommand(Command currentCommand) {
        this.currentCommand = currentCommand;
    }

    public void press() {
        if (currentCommand != null) {
            currentCommand.execute();
            history.push(currentCommand);
        } else {
            System.out.println("No Command Present");
        }
    }

    public void undo() {
        if (history.isEmpty()) {
            System.out.println("No Command to Execute");
        } else {
            Command prevCommand = history.pop();
            prevCommand.undo();
        }
    }
}

class HomeController {

    public static void main(String[] args) {

        Light light = new Light();
        Thermostat thermostat = new Thermostat();

        Command lightOn = new LightOnCommand(light);
        Command lightOff = new LightOffCommand(light);

        Command thermostatCommand = new SetTemperatureCommand(thermostat, 78);


        SmartButton button = new SmartButton();

        button.setCommand(lightOn);
        button.press(); // light on
        button.setCommand(thermostatCommand);
        button.press(); // set 78
        button.undo(); // temp 20
        button.undo(); // light off
        button.setCommand(lightOff);
        button.press();
        button.undo();

    }
}


class SmartHomeApp {
    public static void main(String[] args) {
        // Receivers
        Light light = new Light();
        Thermostat thermostat = new Thermostat();

        // Commands
        Command lightOn  = new LightOnCommand(light);
        Command lightOff = new LightOffCommand(light);
        Command setTemp22 = new SetTemperatureCommand(thermostat, 22);

        // Invoker
        SmartButton button = new SmartButton();

        // Simulate usage
        System.out.println("→ Pressing Light ON");
        button.setCommand(lightOn);
        button.press();

        System.out.println("→ Pressing Set Temp to 22°C");
        button.setCommand(setTemp22);
        button.press();

        System.out.println("→ Pressing Light OFF");
        button.setCommand(lightOff);
        button.press();

        // Undo sequence
        System.out.println("\n↶ Undo Last Action");
        button.undo();  // undo Light OFF

        System.out.println("↶ Undo Previous Action");
        button.undo();  // undo Set Temp

        System.out.println("↶ Undo Again");
        button.undo();  // undo Light ON
    }
}









