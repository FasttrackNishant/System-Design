import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AirForce {

    List<IAirCraft> jets = new ArrayList<>();

    IAirCraft[] helis = new IAirCraft[1];

    LinkedList<Boeing> cargo = new LinkedList<>();

    public AirForce() {
        jets.add(new Tejas());
        helis[0] = new Chinook();
        cargo.add(new Boeing());
    }

    public List<IAirCraft> getJets() {
        return jets;
    }

    public IAirCraft[] getHelis() {
        return helis;
    }

    public LinkedList<Boeing> getCargo() {
        return cargo;
    }


    // crete airforce iterator

    public CustomIterator createIterator() {
        return new AirForceIterator(this);
    }


}