package medium.meetingscheduler.src.models;

import java.util.List;

public class MeetingRoom {

    private int id;
    private int capacity;
    private boolean isAvailable;
    private List<Interval> bookedIntervals;
}
