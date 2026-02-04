class CourseComponent(ABC):
    @abstractmethod
    def get_id(self) -> str:
        pass

    @abstractmethod
    def get_title(self) -> str:
        pass

    @abstractmethod
    def display(self) -> None:
        pass





class Course(CourseComponent):
    def __init__(self, course_id: str, title: str, instructor: Instructor):
        self.id = course_id
        self.title = title
        self.instructor = instructor
        self.content: List[CourseComponent] = []

    def add_content(self, component: CourseComponent) -> None:
        self.content.append(component)

    def get_id(self) -> str:
        return self.id

    def get_title(self) -> str:
        return self.title

    def get_instructor(self) -> Instructor:
        return self.instructor

    def get_content(self) -> List[CourseComponent]:
        return self.content

    def display(self) -> None:
        print(f"Course: {self.title} by {self.instructor.get_name()}")
        for component in self.content:
            component.display()





class Lecture(CourseComponent):
    def __init__(self, lecture_id: str, title: str, duration: int):
        self.id = lecture_id
        self.title = title
        self.duration_minutes = duration

    def get_id(self) -> str:
        return self.id

    def get_title(self) -> str:
        return self.title

    def display(self) -> None:
        print(f"  - Lecture: {self.title} ({self.duration_minutes} mins)")




class Quiz(CourseComponent):
    def __init__(self, quiz_id: str, title: str, question_count: int):
        self.id = quiz_id
        self.title = title
        self.question_count = question_count

    def get_id(self) -> str:
        return self.id

    def get_title(self) -> str:
        return self.title

    def display(self) -> None:
        print(f"  - Quiz: {self.title} ({self.question_count} questions)")












class Enrollment:
    class Status(Enum):
        IN_PROGRESS = "IN_PROGRESS"
        COMPLETED = "COMPLETED"

    def __init__(self, enrollment_id: str, student: Student, course: Course):
        self.id = enrollment_id
        self.student = student
        self.course = course
        self.progress: Dict[str, bool] = {}  # contentId -> isCompleted
        self.status = self.Status.IN_PROGRESS

    def mark_component_complete(self, component_id: str) -> None:
        self.progress[component_id] = True

    def is_course_completed(self) -> bool:
        return len(self.progress) == len(self.course.get_content())

    def get_progress_percentage(self) -> float:
        completed_count = len(self.progress)
        return (completed_count / len(self.course.get_content())) * 100

    def get_id(self) -> str:
        return self.id

    def get_student(self) -> Student:
        return self.student

    def get_course(self) -> Course:
        return self.course

    def get_status(self) -> 'Status':
        return self.status

    def set_status(self, status: 'Status') -> None:
        self.status = status





class User(ABC):
    def __init__(self, name: str, email: str):
        self.id = str(uuid.uuid4())
        self.name = name
        self.email = email

    def get_id(self) -> str:
        return self.id

    def get_name(self) -> str:
        return self.name

class Student(User):
    def __init__(self, name: str, email: str):
        super().__init__(name, email)

class Instructor(User):
    def __init__(self, name: str, email: str):
        super().__init__(name, email)








class ContentFactory:
    @staticmethod
    def create_lecture(title: str, duration: int) -> CourseComponent:
        return Lecture(str(uuid.uuid4()), title, duration)

    @staticmethod
    def create_quiz(title: str, question_count: int) -> CourseComponent:
        return Quiz(str(uuid.uuid4()), title, question_count)




class CertificateIssuer(ProgressObserver):
    def on_course_completed(self, enrollment: Enrollment) -> None:
        print("--- OBSERVER (CertificateIssuer) ---")
        print(f"Issuing certificate to {enrollment.get_student().get_name()} "
              f"for completing '{enrollment.get_course().get_title()}'.")
        print("------------------------------------")




class InstructorNotifier(ProgressObserver):
    def on_course_completed(self, enrollment: Enrollment) -> None:
        print("--- OBSERVER (InstructorNotifier) ---")
        print(f"Notifying instructor {enrollment.get_course().get_instructor().get_name()} "
              f"that {enrollment.get_student().get_name()} has completed the course '"
              f"{enrollment.get_course().get_title()}'.")
        print("-------------------------------------")



class ProgressObserver(ABC):
    @abstractmethod
    def on_course_completed(self, enrollment: Enrollment) -> None:
        pass










class CourseRepository:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance.courses = {}
        return cls._instance

    @classmethod
    def get_instance(cls):
        return cls()

    def save(self, course: Course) -> None:
        self.courses[course.get_id()] = course

    def find_by_id(self, course_id: str) -> Optional[Course]:
        return self.courses.get(course_id)






class EnrollmentRepository:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance.enrollments = {}
        return cls._instance

    @classmethod
    def get_instance(cls):
        return cls()

    def save(self, enrollment: Enrollment) -> None:
        self.enrollments[enrollment.get_id()] = enrollment

    def find_by_id(self, enrollment_id: str) -> Optional[Enrollment]:
        return self.enrollments.get(enrollment_id)





class UserRepository:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance.users = {}
        return cls._instance

    @classmethod
    def get_instance(cls):
        return cls()

    def save(self, user: User) -> None:
        self.users[user.get_id()] = user

    def find_by_id(self, user_id: str) -> Optional[User]:
        return self.users.get(user_id)









class EnrollmentService:
    def __init__(self):
        self.enroll_repo = EnrollmentRepository.get_instance()
        self.observers: List[ProgressObserver] = []

    def enroll_student(self, student: Student, course: Course) -> Enrollment:
        enrollment_id = self._get_enrollment_id(student.get_id(), course.get_id())
        enrollment = Enrollment(enrollment_id, student, course)
        self.enroll_repo.save(enrollment)
        return enrollment

    def _get_enrollment_id(self, student_id: str, course_id: str) -> str:
        return f"{student_id}|{course_id}"

    def mark_component_as_complete(self, student_id: str, course_id: str, component_id: str) -> None:
        enrollment = self.enroll_repo.find_by_id(self._get_enrollment_id(student_id, course_id))
        enrollment.mark_component_complete(component_id)
        print(f"Progress for {enrollment.get_student().get_name()} in '{enrollment.get_course().get_title()}': "
              f"{enrollment.get_progress_percentage():.2f}%")

        if enrollment.is_course_completed():
            enrollment.set_status(Enrollment.Status.COMPLETED)
            self._notify_course_completion(enrollment)

        self.enroll_repo.save(enrollment)

    def add_observer(self, observer: ProgressObserver) -> None:
        self.observers.append(observer)

    def _notify_course_completion(self, enrollment: Enrollment) -> None:
        for observer in self.observers:
            observer.on_course_completed(enrollment)












def main():
    # 1. Setup the system facade and observers
    platform = LearningPlatformFacade()
    platform.add_progress_observer(CertificateIssuer())
    platform.add_progress_observer(InstructorNotifier())

    # 2. Create users and a course
    instructor = platform.create_instructor("Dr. Smith", "smith@algomaster.io")
    alice = platform.create_student("Alice", "smith@algomaster.io")
    java_course = platform.create_course("JAVA-101", "Advanced Java", instructor)

    # 3. Add content to the course using the factory
    platform.add_lecture_to_course(java_course.get_id(), "Introduction to Design Patterns", 60)
    platform.add_quiz_to_course(java_course.get_id(), "SOLID Principles Quiz", 10)
    platform.add_lecture_to_course(java_course.get_id(), "Advanced Concurrency", 90)

    print("----------- Course Structure -----------")
    java_course.display()

    print("\n----------- Alice Enrolls and Makes Progress -----------")
    alices_enrollment = platform.enroll_student(alice.get_id(), java_course.get_id())
    if alices_enrollment is None:
        print("Enrollment failed.")
        return

    print(f"{alice.get_name()} enrolled in '{java_course.get_title()}'.")

    # Alice completes the first lecture
    first_lecture_id = java_course.get_content()[0].get_id()
    platform.complete_component(alice.get_id(), java_course.get_id(), first_lecture_id)

    # Alice completes the quiz
    quiz_id = java_course.get_content()[1].get_id()
    platform.complete_component(alice.get_id(), java_course.get_id(), quiz_id)

    print("\n----------- Alice Completes the Course (Triggers Observers) -----------")
    # Alice completes the final lecture
    second_lecture_id = java_course.get_content()[2].get_id()
    platform.complete_component(alice.get_id(), java_course.get_id(), second_lecture_id)

if __name__ == "__main__":
    main()












class LearningPlatformFacade:
    def __init__(self):
        self.user_repo = UserRepository.get_instance()
        self.course_repo = CourseRepository.get_instance()
        self.enrollment_service = EnrollmentService()

    def add_progress_observer(self, observer: ProgressObserver) -> None:
        self.enrollment_service.add_observer(observer)

    def create_student(self, name: str, email: str) -> Student:
        student = Student(name, email)
        self.user_repo.save(student)
        return student

    def create_instructor(self, name: str, email: str) -> Instructor:
        instructor = Instructor(name, email)
        self.user_repo.save(instructor)
        return instructor

    def create_course(self, course_id: str, title: str, instructor: Instructor) -> Course:
        course = Course(course_id, title, instructor)
        self.course_repo.save(course)
        return course

    def add_lecture_to_course(self, course_id: str, title: str, duration: int) -> None:
        course = self.course_repo.find_by_id(course_id)
        lecture = ContentFactory.create_lecture(title, duration)
        course.add_content(lecture)

    def add_quiz_to_course(self, course_id: str, title: str, questions: int) -> None:
        course = self.course_repo.find_by_id(course_id)
        quiz = ContentFactory.create_quiz(title, questions)
        course.add_content(quiz)

    def enroll_student(self, student_id: str, course_id: str) -> Enrollment:
        student = self.user_repo.find_by_id(student_id)
        course = self.course_repo.find_by_id(course_id)
        return self.enrollment_service.enroll_student(student, course)

    def complete_component(self, student_id: str, course_id: str, component_id: str) -> None:
        self.enrollment_service.mark_component_as_complete(student_id, course_id, component_id)













































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































