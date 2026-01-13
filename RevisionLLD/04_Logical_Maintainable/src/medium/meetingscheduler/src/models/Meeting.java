package medium.meetingscheduler.src.models;

import java.util.List;

public class Meeting {
    private int id;
    private MeetingRoom room;
    private String subject;
    private Interval interval;
    private List<User> attendees;
    private User sender;

    public void addInMeeting(List<User> attendees) {

    }
    public void removeFromMeeting(User user) {}

}
