class Course : public CourseComponent {
private:
    string id;
    string title;
    shared_ptr<Instructor> instructor;
    vector<shared_ptr<CourseComponent>> content;

public:
    Course(const string& id, const string& title, shared_ptr<Instructor> instructor)
        : id(id), title(title), instructor(instructor) {}

    void addContent(shared_ptr<CourseComponent> component) {
        content.push_back(component);
    }

    string getId() const override { return id; }
    string getTitle() const override { return title; }
    shared_ptr<Instructor> getInstructor() const { return instructor; }
    vector<shared_ptr<CourseComponent>> getContent() const { return content; }

    void display() const override {
        cout << "Course: " << title << " by " << instructor->getName() << endl;
        for (const auto& component : content) {
            component->display();
        }
    }
};





class CourseComponent {
public:
    virtual ~CourseComponent() = default;
    virtual string getId() const = 0;
    virtual string getTitle() const = 0;
    virtual void display() const = 0;
};






class Lecture : public CourseComponent {
private:
    string id;
    string title;
    int durationMinutes;

public:
    Lecture(const string& id, const string& title, int duration)
        : id(id), title(title), durationMinutes(duration) {}

    string getId() const override { return id; }
    string getTitle() const override { return title; }

    void display() const override {
        cout << "  - Lecture: " << title << " (" << durationMinutes << " mins)" << endl;
    }
};





class Quiz : public CourseComponent {
private:
    string id;
    string title;
    int questionCount;

public:
    Quiz(const string& id, const string& title, int questionCount)
        : id(id), title(title), questionCount(questionCount) {}

    string getId() const override { return id; }
    string getTitle() const override { return title; }

    void display() const override {
        cout << "  - Quiz: " << title << " (" << questionCount << " questions)" << endl;
    }
};







class Enrollment {
public:
    enum Status { IN_PROGRESS, COMPLETED };

private:
    string id;
    shared_ptr<Student> student;
    shared_ptr<Course> course;
    map<string, bool> progress; // contentId -> isCompleted
    Status status;

public:
    Enrollment(const string& id, shared_ptr<Student> student, shared_ptr<Course> course)
        : id(id), student(student), course(course), status(IN_PROGRESS) {}

    void markComponentComplete(const string& componentId) {
        progress[componentId] = true;
    }

    bool isCourseCompleted() const {
        return progress.size() == course->getContent().size();
    }

    double getProgressPercentage() const {
        int completedCount = progress.size();
        return (double)completedCount / course->getContent().size() * 100;
    }

    string getId() const { return id; }
    shared_ptr<Student> getStudent() const { return student; }
    shared_ptr<Course> getCourse() const { return course; }
    Status getStatus() const { return status; }
    void setStatus(Status status) { this->status = status; }
};





class Instructor : public User {
public:
    Instructor(const string& name, const string& email) : User(name, email) {}
};



class Student : public User {
public:
    Student(const string& name, const string& email) : User(name, email) {}
};




class User {
protected:
    string id;
    string name;
    string email;

public:
    User(const string& name, const string& email) 
        : id(generateId()), name(name), email(email) {}

    virtual ~User() = default;

    string getId() const { return id; }
    string getName() const { return name; }
};








class ContentFactory {
public:
    static shared_ptr<CourseComponent> createLecture(const string& title, int duration) {
        return make_shared<Lecture>(generateId(), title, duration);
    }

    static shared_ptr<CourseComponent> createQuiz(const string& title, int questionCount) {
        return make_shared<Quiz>(generateId(), title, questionCount);
    }
};




class CertificateIssuer : public ProgressObserver {
public:
    void onCourseCompleted(shared_ptr<Enrollment> enrollment) override {
        cout << "--- OBSERVER (CertificateIssuer) ---" << endl;
        cout << "Issuing certificate to " << enrollment->getStudent()->getName()
             << " for completing '" << enrollment->getCourse()->getTitle() << "'." << endl;
        cout << "------------------------------------" << endl;
    }
};




class InstructorNotifier : public ProgressObserver {
public:
    void onCourseCompleted(shared_ptr<Enrollment> enrollment) override {
        cout << "--- OBSERVER (InstructorNotifier) ---" << endl;
        cout << "Notifying instructor " << enrollment->getCourse()->getInstructor()->getName()
             << " that " << enrollment->getStudent()->getName() << " has completed the course '"
             << enrollment->getCourse()->getTitle() << "'." << endl;
        cout << "-------------------------------------" << endl;
    }
};



class ProgressObserver {
public:
    virtual ~ProgressObserver() = default;
    virtual void onCourseCompleted(shared_ptr<Enrollment> enrollment) = 0;
};





class CourseRepository {
private:
    static CourseRepository* instance;
    static mutex instanceMutex;
    map<string, shared_ptr<Course>> courses;

    CourseRepository() {}

public:
    static CourseRepository* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new CourseRepository();
        }
        return instance;
    }

    void save(shared_ptr<Course> course) {
        courses[course->getId()] = course;
    }

    shared_ptr<Course> findById(const string& id) {
        auto it = courses.find(id);
        return (it != courses.end()) ? it->second : nullptr;
    }
};




class EnrollmentRepository {
private:
    static EnrollmentRepository* instance;
    static mutex instanceMutex;
    map<string, shared_ptr<Enrollment>> enrollments;

    EnrollmentRepository() {}

public:
    static EnrollmentRepository* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new EnrollmentRepository();
        }
        return instance;
    }

    void save(shared_ptr<Enrollment> enrollment) {
        enrollments[enrollment->getId()] = enrollment;
    }

    shared_ptr<Enrollment> findById(const string& id) {
        auto it = enrollments.find(id);
        return (it != enrollments.end()) ? it->second : nullptr;
    }
};

// Static member definitions (required for linking)
UserRepository* UserRepository::instance = nullptr;
mutex UserRepository::instanceMutex;

CourseRepository* CourseRepository::instance = nullptr;
mutex CourseRepository::instanceMutex;

EnrollmentRepository* EnrollmentRepository::instance = nullptr;
mutex EnrollmentRepository::instanceMutex;







class UserRepository {
private:
    static UserRepository* instance;
    static mutex instanceMutex;
    map<string, shared_ptr<User>> users;

    UserRepository() {}

public:
    static UserRepository* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new UserRepository();
        }
        return instance;
    }

    void save(shared_ptr<User> user) {
        users[user->getId()] = user;
    }

    shared_ptr<User> findById(const string& id) {
        auto it = users.find(id);
        return (it != users.end()) ? it->second : nullptr;
    }
};









class EnrollmentService {
private:
    EnrollmentRepository* enrollRepo;
    vector<shared_ptr<ProgressObserver>> observers;

    string getEnrollmentId(const string& studentId, const string& courseId) {
        return studentId + "|" + courseId;
    }

    void notifyCourseCompletion(shared_ptr<Enrollment> enrollment) {
        for (const auto& observer : observers) {
            observer->onCourseCompleted(enrollment);
        }
    }

public:
    EnrollmentService() : enrollRepo(EnrollmentRepository::getInstance()) {}

    shared_ptr<Enrollment> enrollStudent(shared_ptr<Student> student, shared_ptr<Course> course) {
        string enrollmentId = getEnrollmentId(student->getId(), course->getId());
        shared_ptr<Enrollment> enrollment = make_shared<Enrollment>(enrollmentId, student, course);
        enrollRepo->save(enrollment);
        return enrollment;
    }

    void markComponentAsComplete(const string& studentId, const string& courseId, const string& componentId) {
        shared_ptr<Enrollment> enrollment = enrollRepo->findById(getEnrollmentId(studentId, courseId));
        enrollment->markComponentComplete(componentId);
        cout << "Progress for " << enrollment->getStudent()->getName() << " in '"
             << enrollment->getCourse()->getTitle() << "': "
             << enrollment->getProgressPercentage() << "%" << endl;

        if (enrollment->isCourseCompleted()) {
            enrollment->setStatus(Enrollment::COMPLETED);
            notifyCourseCompletion(enrollment);
        }

        enrollRepo->save(enrollment);
    }

    void addObserver(shared_ptr<ProgressObserver> observer) {
        observers.push_back(observer);
    }
};





int main() {
    // 1. Setup the system facade and observers
    LearningPlatformFacade platform;
    platform.addProgressObserver(make_shared<CertificateIssuer>());
    platform.addProgressObserver(make_shared<InstructorNotifier>());

    // 2. Create users and a course
    shared_ptr<Instructor> instructor = platform.createInstructor("Dr. Smith", "smith@algomaster.io");
    shared_ptr<Student> alice = platform.createStudent("Alice", "alice@algomaster.io");
    shared_ptr<Course> javaCourse = platform.createCourse("JAVA-101", "Advanced Java", instructor);

    // 3. Add content to the course using the factory
    platform.addLectureToCourse(javaCourse->getId(), "Introduction to Design Patterns", 60);
    platform.addQuizToCourse(javaCourse->getId(), "SOLID Principles Quiz", 10);
    platform.addLectureToCourse(javaCourse->getId(), "Advanced Concurrency", 90);

    cout << "----------- Course Structure -----------" << endl;
    javaCourse->display();

    cout << endl << "----------- Alice Enrolls and Makes Progress -----------" << endl;
    shared_ptr<Enrollment> alicesEnrollment = platform.enrollStudent(alice->getId(), javaCourse->getId());
    if (alicesEnrollment == nullptr) {
        cout << "Enrollment failed." << endl;
        return 1;
    }

    cout << alice->getName() << " enrolled in '" << javaCourse->getTitle() << "'." << endl;

    // Alice completes the first lecture
    string firstLectureId = javaCourse->getContent()[0]->getId();
    platform.completeComponent(alice->getId(), javaCourse->getId(), firstLectureId);

    // Alice completes the quiz
    string quizId = javaCourse->getContent()[1]->getId();
    platform.completeComponent(alice->getId(), javaCourse->getId(), quizId);

    cout << endl << "----------- Alice Completes the Course (Triggers Observers) -----------" << endl;
    // Alice completes the final lecture
    string secondLectureId = javaCourse->getContent()[2]->getId();
    platform.completeComponent(alice->getId(), javaCourse->getId(), secondLectureId);

    return 0;
}












class LearningPlatformFacade {
private:
    UserRepository* userRepo;
    CourseRepository* courseRepo;
    unique_ptr<EnrollmentService> enrollmentService;

public:
    LearningPlatformFacade() 
        : userRepo(UserRepository::getInstance()),
          courseRepo(CourseRepository::getInstance()),
          enrollmentService(make_unique<EnrollmentService>()) {}

    void addProgressObserver(shared_ptr<ProgressObserver> observer) {
        enrollmentService->addObserver(observer);
    }

    shared_ptr<Student> createStudent(const string& name, const string& email) {
        shared_ptr<Student> student = make_shared<Student>(name, email);
        userRepo->save(student);
        return student;
    }

    shared_ptr<Instructor> createInstructor(const string& name, const string& email) {
        shared_ptr<Instructor> instructor = make_shared<Instructor>(name, email);
        userRepo->save(instructor);
        return instructor;
    }

    shared_ptr<Course> createCourse(const string& courseId, const string& title, shared_ptr<Instructor> instructor) {
        shared_ptr<Course> course = make_shared<Course>(courseId, title, instructor);
        courseRepo->save(course);
        return course;
    }

    void addLectureToCourse(const string& courseId, const string& title, int duration) {
        shared_ptr<Course> course = courseRepo->findById(courseId);
        shared_ptr<CourseComponent> lecture = ContentFactory::createLecture(title, duration);
        course->addContent(lecture);
    }

    void addQuizToCourse(const string& courseId, const string& title, int questions) {
        shared_ptr<Course> course = courseRepo->findById(courseId);
        shared_ptr<CourseComponent> quiz = ContentFactory::createQuiz(title, questions);
        course->addContent(quiz);
    }

    shared_ptr<Enrollment> enrollStudent(const string& studentId, const string& courseId) {
        shared_ptr<User> user = userRepo->findById(studentId);
        shared_ptr<Student> student = dynamic_pointer_cast<Student>(user);
        shared_ptr<Course> course = courseRepo->findById(courseId);
        return enrollmentService->enrollStudent(student, course);
    }

    void completeComponent(const string& studentId, const string& courseId, const string& componentId) {
        enrollmentService->markComponentAsComplete(studentId, courseId, componentId);
    }
};


















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































