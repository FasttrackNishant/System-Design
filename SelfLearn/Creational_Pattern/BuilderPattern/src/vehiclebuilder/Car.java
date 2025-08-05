package vehiclebuilder;

public class Car {
    private  String chassis;
    private String engine;
    private String bodyShell;
    private String tyre;

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    public void setBodyShell(String bodyShell) {
        this.bodyShell = bodyShell;
    }

    public void setTyre(String tyre) {
        this.tyre = tyre;
    }

    @Override
    public String toString() {
        return "Car{" +
                "chassis='" + chassis + '\'' +
                ", engine='" + engine + '\'' +
                ", bodyShell='" + bodyShell + '\'' +
                ", tyre='" + tyre + '\'' +
                '}';
    }
}
