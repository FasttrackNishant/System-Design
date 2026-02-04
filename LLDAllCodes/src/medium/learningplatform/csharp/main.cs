class Course : ICourseComponent
{
    private readonly string id;
    private readonly string title;
    private readonly Instructor instructor;
    private readonly List<ICourseComponent> content = new List<ICourseComponent>();

    public Course(string id, string title, Instructor instructor)
    {
        this.id = id;
        this.title = title;
        this.instructor = instructor;
    }

    public void AddContent(ICourseComponent component)
    {
        content.Add(component);
    }

    public string GetId() { return id; }
    public string GetTitle() { return title; }
    public Instructor GetInstructor() { return instructor; }
    public List<ICourseComponent> GetContent() { return content; }

    public void Display()
    {
        Console.WriteLine($"Course: {title} by {instructor.GetName()}");
        foreach (var component in content)
        {
            component.Display();
        }
    }
}






interface ICourseComponent
{
    string GetId();
    string GetTitle();
    void Display();
}







class Lecture : ICourseComponent
{
    private readonly string id;
    private readonly string title;
    private readonly int durationMinutes;

    public Lecture(string id, string title, int duration)
    {
        this.id = id;
        this.title = title;
        this.durationMinutes = duration;
    }

    public string GetId() { return id; }
    public string GetTitle() { return title; }

    public void Display()
    {
        Console.WriteLine($"  - Lecture: {title} ({durationMinutes} mins)");
    }
}






class Quiz : ICourseComponent
{
    private readonly string id;
    private readonly string title;
    private readonly int questionCount;

    public Quiz(string id, string title, int questionCount)
    {
        this.id = id;
        this.title = title;
        this.questionCount = questionCount;
    }

    public string GetId() { return id; }
    public string GetTitle() { return title; }

    public void Display()
    {
        Console.WriteLine($"  - Quiz: {title} ({questionCount} questions)");
    }
}








class Enrollment
{
    public enum Status { IN_PROGRESS, COMPLETED }

    private readonly string id;
    private readonly Student student;
    private readonly Course course;
    private readonly Dictionary<string, bool> progress = new Dictionary<string, bool>(); // contentId -> isCompleted
    private Status status;

    public Enrollment(string id, Student student, Course course)
    {
        this.id = id;
        this.student = student;
        this.course = course;
        this.status = Status.IN_PROGRESS;
    }

    public void MarkComponentComplete(string componentId)
    {
        progress[componentId] = true;
    }

    public bool IsCourseCompleted()
    {
        return progress.Count == course.GetContent().Count;
    }

    public double GetProgressPercentage()
    {
        int completedCount = progress.Count;
        return (double)completedCount / course.GetContent().Count * 100;
    }

    public string GetId() { return id; }
    public Student GetStudent() { return student; }
    public Course GetCourse() { return course; }
    public Status GetStatus() { return status; }
    public void SetStatus(Status status) { this.status = status; }
}



class Instructor : User
{
    public Instructor(string name, string email) : base(name, email) { }
}





class Student : User
{
    public Student(string name, string email) : base(name, email) { }
}



abstract class User
{
    private readonly string id;
    private readonly string name;
    private readonly string email;

    public User(string name, string email)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.email = email;
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
}







class ContentFactory
{
    public static ICourseComponent CreateLecture(string title, int duration)
    {
        return new Lecture(Guid.NewGuid().ToString(), title, duration);
    }

    public static ICourseComponent CreateQuiz(string title, int questionCount)
    {
        return new Quiz(Guid.NewGuid().ToString(), title, questionCount);
    }
}




class CertificateIssuer : IProgressObserver
{
    public void OnCourseCompleted(Enrollment enrollment)
    {
        Console.WriteLine("--- OBSERVER (CertificateIssuer) ---");
        Console.WriteLine($"Issuing certificate to {enrollment.GetStudent().GetName()} " +
                         $"for completing '{enrollment.GetCourse().GetTitle()}'.");
        Console.WriteLine("------------------------------------");
    }
}





class InstructorNotifier : IProgressObserver
{
    public void OnCourseCompleted(Enrollment enrollment)
    {
        Console.WriteLine("--- OBSERVER (InstructorNotifier) ---");
        Console.WriteLine($"Notifying instructor {enrollment.GetCourse().GetInstructor().GetName()} " +
                         $"that {enrollment.GetStudent().GetName()} has completed the course '" +
                         $"{enrollment.GetCourse().GetTitle()}'.");
        Console.WriteLine("-------------------------------------");
    }
}





interface IProgressObserver
{
    void OnCourseCompleted(Enrollment enrollment);
}








class CourseRepository
{
    private static readonly CourseRepository INSTANCE = new CourseRepository();
    private readonly ConcurrentDictionary<string, Course> courses = new ConcurrentDictionary<string, Course>();

    private CourseRepository() { }

    public static CourseRepository GetInstance() { return INSTANCE; }

    public void Save(Course course)
    {
        courses[course.GetId()] = course;
    }

    public Course FindById(string id)
    {
        courses.TryGetValue(id, out Course course);
        return course;
    }
}




class EnrollmentRepository
{
    private static readonly EnrollmentRepository INSTANCE = new EnrollmentRepository();
    private readonly ConcurrentDictionary<string, Enrollment> enrollments = new ConcurrentDictionary<string, Enrollment>();

    private EnrollmentRepository() { }

    public static EnrollmentRepository GetInstance() { return INSTANCE; }

    public void Save(Enrollment enrollment)
    {
        enrollments[enrollment.GetId()] = enrollment;
    }

    public Enrollment FindById(string id)
    {
        enrollments.TryGetValue(id, out Enrollment enrollment);
        return enrollment;
    }
}





class UserRepository
{
    private static readonly UserRepository INSTANCE = new UserRepository();
    private readonly ConcurrentDictionary<string, User> users = new ConcurrentDictionary<string, User>();

    private UserRepository() { }

    public static UserRepository GetInstance() { return INSTANCE; }

    public void Save(User user)
    {
        users[user.GetId()] = user;
    }

    public User FindById(string id)
    {
        users.TryGetValue(id, out User user);
        return user;
    }
}









class EnrollmentService
{
    private readonly EnrollmentRepository enrollRepo = EnrollmentRepository.GetInstance();
    private readonly List<IProgressObserver> observers = new List<IProgressObserver>();

    public Enrollment EnrollStudent(Student student, Course course)
    {
        string enrollmentId = GetEnrollmentId(student.GetId(), course.GetId());
        Enrollment enrollment = new Enrollment(enrollmentId, student, course);
        enrollRepo.Save(enrollment);
        return enrollment;
    }

    private string GetEnrollmentId(string studentId, string courseId)
    {
        return studentId + "|" + courseId;
    }

    public void MarkComponentAsComplete(string studentId, string courseId, string componentId)
    {
        Enrollment enrollment = enrollRepo.FindById(GetEnrollmentId(studentId, courseId));
        enrollment.MarkComponentComplete(componentId);
        Console.WriteLine($"Progress for {enrollment.GetStudent().GetName()} in '{enrollment.GetCourse().GetTitle()}': " +
                         $"{enrollment.GetProgressPercentage():F2}%");

        if (enrollment.IsCourseCompleted())
        {
            enrollment.SetStatus(Enrollment.Status.COMPLETED);
            NotifyCourseCompletion(enrollment);
        }

        enrollRepo.Save(enrollment);
    }

    public void AddObserver(IProgressObserver observer)
    {
        observers.Add(observer);
    }

    private void NotifyCourseCompletion(Enrollment enrollment)
    {
        foreach (var observer in observers)
        {
            observer.OnCourseCompleted(enrollment);
        }
    }
}







using System;
using System.Collections.Generic;
using System.Collections.Concurrent;

public class LearningPlatformDemo
{
    public static void Main(string[] args)
    {
        // 1. Setup the system facade and observers
        LearningPlatformFacade platform = new LearningPlatformFacade();
        platform.AddProgressObserver(new CertificateIssuer());
        platform.AddProgressObserver(new InstructorNotifier());

        // 2. Create users and a course
        Instructor instructor = platform.CreateInstructor("Dr. Smith", "smith@algomaster.io");
        Student alice = platform.CreateStudent("Alice", "smith@algomaster.io");
        Course javaCourse = platform.CreateCourse("JAVA-101", "Advanced Java", instructor);

        // 3. Add content to the course using the factory
        platform.AddLectureToCourse(javaCourse.GetId(), "Introduction to Design Patterns", 60);
        platform.AddQuizToCourse(javaCourse.GetId(), "SOLID Principles Quiz", 10);
        platform.AddLectureToCourse(javaCourse.GetId(), "Advanced Concurrency", 90);

        Console.WriteLine("----------- Course Structure -----------");
        javaCourse.Display();

        Console.WriteLine("\n----------- Alice Enrolls and Makes Progress -----------");
        Enrollment alicesEnrollment = platform.EnrollStudent(alice.GetId(), javaCourse.GetId());
        if (alicesEnrollment == null)
        {
            Console.WriteLine("Enrollment failed.");
            return;
        }

        Console.WriteLine($"{alice.GetName()} enrolled in '{javaCourse.GetTitle()}'.");

        // Alice completes the first lecture
        string firstLectureId = javaCourse.GetContent()[0].GetId();
        platform.CompleteComponent(alice.GetId(), javaCourse.GetId(), firstLectureId);

        // Alice completes the quiz
        string quizId = javaCourse.GetContent()[1].GetId();
        platform.CompleteComponent(alice.GetId(), javaCourse.GetId(), quizId);

        Console.WriteLine("\n----------- Alice Completes the Course (Triggers Observers) -----------");
        // Alice completes the final lecture
        string secondLectureId = javaCourse.GetContent()[2].GetId();
        platform.CompleteComponent(alice.GetId(), javaCourse.GetId(), secondLectureId);
    }
}







class LearningPlatformFacade
{
    private readonly UserRepository userRepo = UserRepository.GetInstance();
    private readonly CourseRepository courseRepo = CourseRepository.GetInstance();
    private readonly EnrollmentService enrollmentService = new EnrollmentService();

    public void AddProgressObserver(IProgressObserver observer)
    {
        enrollmentService.AddObserver(observer);
    }

    public Student CreateStudent(string name, string email)
    {
        Student student = new Student(name, email);
        userRepo.Save(student);
        return student;
    }

    public Instructor CreateInstructor(string name, string email)
    {
        Instructor instructor = new Instructor(name, email);
        userRepo.Save(instructor);
        return instructor;
    }

    public Course CreateCourse(string courseId, string title, Instructor instructor)
    {
        Course course = new Course(courseId, title, instructor);
        courseRepo.Save(course);
        return course;
    }

    public void AddLectureToCourse(string courseId, string title, int duration)
    {
        Course course = courseRepo.FindById(courseId);
        ICourseComponent lecture = ContentFactory.CreateLecture(title, duration);
        course.AddContent(lecture);
    }

    public void AddQuizToCourse(string courseId, string title, int questions)
    {
        Course course = courseRepo.FindById(courseId);
        ICourseComponent quiz = ContentFactory.CreateQuiz(title, questions);
        course.AddContent(quiz);
    }

    public Enrollment EnrollStudent(string studentId, string courseId)
    {
        Student student = (Student)userRepo.FindById(studentId);
        Course course = courseRepo.FindById(courseId);
        return enrollmentService.EnrollStudent(student, course);
    }

    public void CompleteComponent(string studentId, string courseId, string componentId)
    {
        enrollmentService.MarkComponentAsComplete(studentId, courseId, componentId);
    }
}















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































