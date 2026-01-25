package medium.librarymanagementsystem;

import java.time.LocalDateTime;
import java.util.*;

/* ===================== ENUM ===================== */

enum BookStatus {
    AVAILABLE,
    ISSUED
}

/* ===================== BOOK ===================== */

class Book {
    private String bookId;
    private String name;
    private String description;
    private BookStatus status;

    public Book(String name, String description) {
        this.bookId = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.status = BookStatus.AVAILABLE;
    }

    public String getBookId() {
        return bookId;
    }

    public String getName() {
        return name;
    }

    public BookStatus getStatus() {
        return status;
    }

    public void setStatus(BookStatus status) {
        this.status = status;
    }
}

/* ===================== LOAN ===================== */

class Loan {
    private String bookId;
    private String memberId;
    private LocalDateTime issueDate;

    public Loan(String bookId, String memberId) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = LocalDateTime.now();
    }

    public String getBookId() {
        return bookId;
    }

    public String getMemberId() {
        return memberId;
    }
}

/* ===================== MEMBER ===================== */

class Member {
    private String id;
    private String name;
    private List<Loan> loans;

    public Member(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.loans = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addLoan(Loan loan) {
        loans.add(loan);
    }

    public void removeLoan(String bookId) {
        loans.removeIf(loan -> loan.getBookId().equals(bookId));
    }

    public List<Loan> getLoans() {
        return loans;
    }
}

/* ===================== LIBRARY MANAGEMENT SYSTEM ===================== */

class LibraryManagementSystem {

    private Map<String, Book> books = new HashMap<>();
    private Map<String, Member> members = new HashMap<>();
    private Map<String, Loan> activeLoans = new HashMap<>();

    /* Add Book */
    public void addBook(Book book) {
        books.put(book.getBookId(), book);
    }

    /* Add Member */
    public void addMember(Member member) {
        members.put(member.getId(), member);
    }

    /* Issue Book */
    public boolean issueBook(String bookId, String memberId) {

        if (!books.containsKey(bookId) || !members.containsKey(memberId)) {
            return false;
        }

        Book book = books.get(bookId);
        Member member = members.get(memberId);

        if (book.getStatus() == BookStatus.ISSUED) {
            return false;
        }

        book.setStatus(BookStatus.ISSUED);

        Loan loan = new Loan(bookId, memberId);
        activeLoans.put(bookId, loan);
        member.addLoan(loan);

        System.out.println(
                "Book issued: " + book.getName() +
                        " to member: " + member.getName()
        );
        return true;
    }

    /* Return Book */
    public boolean returnBook(String bookId) {

        if (!activeLoans.containsKey(bookId)) {
            System.out.println("Invalid book return");
            return false;
        }

        Loan loan = activeLoans.get(bookId);
        Member member = members.get(loan.getMemberId());
        Book book = books.get(bookId);

        activeLoans.remove(bookId);
        member.removeLoan(bookId);
        book.setStatus(BookStatus.AVAILABLE);

        System.out.println("Book returned successfully: " + book.getName());
        return true;
    }
}

/* ===================== MAIN (TEST) ===================== */

class Main {
    public static void main(String[] args) {

        LibraryManagementSystem system = new LibraryManagementSystem();

        Book book1 = new Book("Clean Code", "Programming best practices");
        Book book2 = new Book("Wings of Fire", "Autobiography");

        Member member = new Member("Ram");

        system.addBook(book1);
        system.addBook(book2);
        system.addMember(member);

        system.issueBook(book1.getBookId(), member.getId());
        system.returnBook(book1.getBookId());
    }
}
