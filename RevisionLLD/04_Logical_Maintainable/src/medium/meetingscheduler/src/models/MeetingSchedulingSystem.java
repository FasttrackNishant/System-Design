package medium.meetingscheduler.src.models;

import java.util.List;

public class MeetingSchedulingSystem {
    private MeetingScheduler scheduler;
    private List<User> users;
    private List<MeetingRoom> rooms;

    private static MeetingSchedulingSystem schedulingSystemInstance = null;
    private MeetingSchedulingSystem() {}

    public static MeetingSchedulingSystem getInstance() {
        if(schedulingSystemInstance == null) {
            schedulingSystemInstance = new MeetingSchedulingSystem();
        }
        return schedulingSystemInstance;
    }



    public void addRoom() {


    }

    public void removeRoom() {

    }

    public void scheduleMeeting(List<User> user, Interval interval) {
        //request ko delegate karre ho
        scheduler.scheduleMeeting(user, interval);
    }
    public void cancelMeeting() {

    }


}
