import java.util.LinkedList;
import java.util.List;

public class AirForceIterator implements CustomIterator {

    // main logic yaha rehega ki run karne hain

    List<IAirCraft> jets;

    IAirCraft[] helis;

    LinkedList<Boeing> cargo;

    // ek position for tracking

    int jetsPos = 0;

    int helisPos = 0;

    int cargoPos = 0;

    public AirForceIterator(AirForce airForce) {
        jets = airForce.getJets();
        helis = airForce.getHelis();
        cargo = airForce.getCargo();
    }

    @Override
    public IAirCraft next() {
        // ye current return karega and aage jayega

        if (helisPos < helis.length) {
            return helis[helisPos++];
        }

        if (jetsPos < jets.size()) {
            return jets.get(jetsPos++);
        }

        if (cargoPos < cargo.size()) {
            return cargo.get(cargoPos++);
        }

        throw new RuntimeException("No More Elements");

    }

    @Override
    public boolean hasNext() {
        return helis.length > helisPos || jets.size() > jetsPos || cargo.size() > cargoPos;
    }
}
