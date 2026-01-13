package medium.meetingscheduler.src.models;

import java.util.List;

public class MeetingScheduler {
    private Calendar calendar;
    private List<MeetingRoom> rooms;
    private User organiser;

    private static MeetingScheduler schedulerInstance = null;

    private MeetingScheduler() {}

    public static MeetingScheduler getInstance() {
        if(schedulerInstance == null ) {
            schedulerInstance = new MeetingScheduler();
        }
        return schedulerInstance;
    }


    public boolean scheduleMeeting(List<User> user, Interval interval) {
        //dummy
        return true;
    }
    public boolean cancelMeeting(List<User> user, Interval interval) {
        //dummy
        return true;
    }
    public boolean bookRoom(MeetingRoom room, int numberOfPerson, Interval interval) {
        //dummy
        return true;
    }
    public boolean releaseRoom(MeetingRoom room, Interval interval) {
        //dummy
        return true;
    }



}
