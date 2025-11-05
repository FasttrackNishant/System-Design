import java.util.ArrayList;
import java.util.List;

public class ATCTower {

    // data chahiye sab aircrafk ka

    List<IAirCraft> queueForLanding = new ArrayList<>();


    synchronized public void requestToLand(IAirCraft airCraft) {
        if (queueForLanding.size() == 0) {
            airCraft.land();
        } else {
            queueForLanding.add(airCraft);
        }
    }

}
