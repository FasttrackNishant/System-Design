package easy.snakeandladder.java;


class Course implements CourseComponent {
    private final String id;
    private final String title;
    private final Instructor instructor;
    private final List<CourseComponent> content = new ArrayList<>();

    public Course(String id, String title, Instructor instructor) {
        this.id = id;
        this.title = title;
        this.instructor = instructor;
    }

    public void addContent(CourseComponent component) {
        content.add(component);
    }

    @Override 
    public String getId() { return id; }

    @Override 
    public String getTitle() { return title; }

    public Instructor getInstructor() { return instructor; }

    public List<CourseComponent> getContent() { return content; }

    @Override 
    public void display() {
        System.out.println("Course: " + title + " by " + instructor.getName());
        content.forEach(CourseComponent::display);
    }
}




interface CourseComponent {
    String getId();
    String getTitle();
    void display();
}



class Lecture implements CourseComponent {
    private final String id;
    private final String title;
    private final int durationMinutes;

    public Lecture(String id, String title, int duration) {
        this.id = id;
        this.title = title;
        this.durationMinutes = duration;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getTitle() { return title; }

    @Override
    public void display() {
        System.out.println("  - Lecture: " + title + " (" + durationMinutes + " mins)");
    }
}



class Quiz implements CourseComponent {
    private final String id;
    private final String title;
    private final int questionCount;

    public Quiz(String id, String title, int questionCount) {
        this.id = id;
        this.title = title;
        this.questionCount = questionCount;
    }

    @Override 
    public String getId() { return id; }

    @Override 
    public String getTitle() { return title; }

    @Override
    public void display() {
        System.out.println("  - Quiz: " + title + " (" + questionCount + " questions)");
    }
}

















class Enrollment {
    public enum Status { IN_PROGRESS, COMPLETED }
    private final String id;
    private final Student student;
    private final Course course;
    private final Map<String, Boolean> progress = new HashMap<>(); // contentId -> isCompleted
    private Status status;

    public Enrollment(String id, Student student, Course course) {
        this.id = id;
        this.student = student;
        this.course = course;
        this.status = Status.IN_PROGRESS;
    }

    public void markComponentComplete(String componentId) {
        progress.put(componentId, true);
    }

    public boolean isCourseCompleted() {
        return progress.size() == course.getContent().size();
    }

    public double getProgressPercentage() {
        long completedCount = progress.size();
        return (double) completedCount / course.getContent().size() * 100;
    }

    public String getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}




class Instructor extends User {
    public Instructor(String name, String email) {
        super(name, email);
    }
}



class Student extends User {
    public Student(String name, String email) {
        super(name, email);
    }
}


abstract class User {
    private final String id;
    private final String name;
    private final String email;

    public User(String name, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}







class ContentFactory {
    public static CourseComponent createLecture(String title, int duration) {
        return new Lecture(UUID.randomUUID().toString(), title, duration);
    }

    public static CourseComponent createQuiz(String title, int questionCount) {
        return new Quiz(UUID.randomUUID().toString(), title, questionCount);
    }
}








class CertificateIssuer implements ProgressObserver {
    @Override
    public void onCourseCompleted(Enrollment enrollment) {
        System.out.println("--- OBSERVER (CertificateIssuer) ---");
        System.out.println("Issuing certificate to " + enrollment.getStudent().getName() +
                " for completing '" + enrollment.getCourse().getTitle() + "'.");
        System.out.println("------------------------------------");
    }
}


class InstructorNotifier implements ProgressObserver {
    @Override
    public void onCourseCompleted(Enrollment enrollment) {
        System.out.println("--- OBSERVER (InstructorNotifier) ---");
        System.out.println("Notifying instructor " + enrollment.getCourse().getInstructor().getName() +
                " that " + enrollment.getStudent().getName() + " has completed the course '" +
                enrollment.getCourse().getTitle() + "'.");
        System.out.println("-------------------------------------");
    }
}




interface ProgressObserver {
    void onCourseCompleted(Enrollment enrollment);
}









class CourseRepository {
    private static final CourseRepository INSTANCE = new CourseRepository();
    private final Map<String, Course> courses = new ConcurrentHashMap<>();

    public static CourseRepository getInstance() { return INSTANCE; }

    public void save(Course course) { 
      courses.put(course.getId(), course); 
    }

    public Course findById(String id) { 
      return courses.get(id); 
    }
}



class EnrollmentRepository {
    private static final EnrollmentRepository INSTANCE = new EnrollmentRepository();
    private final Map<String, Enrollment> enrollments = new ConcurrentHashMap<>();

    public static EnrollmentRepository getInstance() { return INSTANCE; }

    public void save(Enrollment enrollment) {
        enrollments.put(enrollment.getId(), enrollment);
    }

    public Enrollment findById(String id) {
        return enrollments.get(id);
    }
}



class UserRepository {
    private static final UserRepository INSTANCE = new UserRepository();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public static UserRepository getInstance() { return INSTANCE; }

    public void save(User user) { users.put(user.getId(), user); }

    public User findById(String id) {
        return users.get(id);
    }
}







class EnrollmentService {
    private final EnrollmentRepository enrollRepo = EnrollmentRepository.getInstance();
    private final List<ProgressObserver> observers = new ArrayList<>();

    public Enrollment enrollStudent(Student student, Course course) {
        String enrollmentId = getEnrollmentId(student.getId(), course.getId());
        Enrollment enrollment = new Enrollment(enrollmentId, student, course);
        enrollRepo.save(enrollment);
        return enrollment;
    }

    private String getEnrollmentId (String studentId, String courseId) {
        return  studentId + "|" + courseId;
    }

    public void markComponentAsComplete(String studentId, String courseId, String componentId) {
        Enrollment enrollment = enrollRepo.findById(getEnrollmentId(studentId, courseId));
        enrollment.markComponentComplete(componentId);
        System.out.println("Progress for " + enrollment.getStudent().getName() + " in '" + enrollment.getCourse().getTitle() + "': "
                + String.format("%.2f", enrollment.getProgressPercentage()) + "%");

        if (enrollment.isCourseCompleted()) {
            enrollment.setStatus(Enrollment.Status.COMPLETED);
            notifyCourseCompletion(enrollment);
        }

        enrollRepo.save(enrollment);
    }

    public void addObserver(ProgressObserver observer) { observers.add(observer); }

    private void notifyCourseCompletion(Enrollment enrollment) {
        observers.forEach(o -> o.onCourseCompleted(enrollment));
    }
}



import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LearningPlatformDemo {
    public static void main(String[] args) {
        // 1. Setup the system facade and observers
        LearningPlatformFacade platform = new LearningPlatformFacade();
        platform.addProgressObserver(new CertificateIssuer());
        platform.addProgressObserver(new InstructorNotifier());

        // 2. Create users and a course
        Instructor instructor = platform.createInstructor("Dr. Smith", "smith@algomaster.io");
        Student alice = platform.createStudent("Alice", "smith@algomaster.io");
        Course javaCourse = platform.createCourse("JAVA-101", "Advanced Java", instructor);

        // 3. Add content to the course using the factory
        platform.addLectureToCourse(javaCourse.getId(), "Introduction to Design Patterns", 60);
        platform.addQuizToCourse(javaCourse.getId(), "SOLID Principles Quiz", 10);
        platform.addLectureToCourse(javaCourse.getId(), "Advanced Concurrency", 90);

        System.out.println("----------- Course Structure -----------");
        javaCourse.display();

        System.out.println("\n----------- Alice Enrolls and Makes Progress -----------");
        Enrollment alicesEnrollment = platform.enrollStudent(alice.getId(), javaCourse.getId());
        if (alicesEnrollment == null) { System.out.println("Enrollment failed."); return; }

        System.out.println(alice.getName() + " enrolled in '" + javaCourse.getTitle() + "'.");

        // Alice completes the first lecture
        String firstLectureId = javaCourse.getContent().get(0).getId();
        platform.completeComponent(alice.getId(), javaCourse.getId(), firstLectureId);

        // Alice completes the quiz
        String quizId = javaCourse.getContent().get(1).getId();
        platform.completeComponent(alice.getId(), javaCourse.getId(), quizId);

        System.out.println("\n----------- Alice Completes the Course (Triggers Observers) -----------");
        // Alice completes the final lecture
        String secondLectureId = javaCourse.getContent().get(2).getId();
        platform.completeComponent(alice.getId(), javaCourse.getId(), secondLectureId);
    }
}




class LearningPlatformFacade {
    private final UserRepository userRepo = UserRepository.getInstance();
    private final CourseRepository courseRepo = CourseRepository.getInstance();
    private final EnrollmentService enrollmentService = new EnrollmentService();

    public void addProgressObserver(ProgressObserver observer) {
        enrollmentService.addObserver(observer);
    }

    public Student createStudent(String name, String email) {
        Student student = new Student(name, email);
        userRepo.save(student);
        return student;
    }

    public Instructor createInstructor(String name, String email) {
        Instructor instructor = new Instructor(name, email);
        userRepo.save(instructor);
        return instructor;
    }

    public Course createCourse(String courseId, String title, Instructor instructor) {
        Course course = new Course(courseId, title, instructor);
        courseRepo.save(course);
        return course;
    }

    public void addLectureToCourse(String courseId, String title, int duration) {
        Course course = courseRepo.findById(courseId);
        CourseComponent lecture = ContentFactory.createLecture(title, duration);
        course.addContent(lecture);
    }

    public void addQuizToCourse(String courseId, String title, int questions) {
        Course course = courseRepo.findById(courseId);
        CourseComponent quiz = ContentFactory.createQuiz(title, questions);
        course.addContent(quiz);
    }

    public Enrollment enrollStudent(String studentId, String courseId) {
        Student student = (Student) userRepo.findById(studentId);
        Course course = courseRepo.findById(courseId);
        return enrollmentService.enrollStudent(student, course);
    }

    public void completeComponent(String studentId, String courseId, String componentId) {
        enrollmentService.markComponentAsComplete(studentId, courseId, componentId);
    }
}





























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































