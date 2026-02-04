package easy.snakeandladder.java;



class Branch {
    private String name;
    private Commit head;

    public Branch(String name, Commit head) {
        this.name = name;
        this.head = head;
    }

    public String getName() {
        return name;
    }

    public Commit getHead() {
        return head;
    }

    public void setHead(Commit head) {
        this.head = head;
    }
}




class Commit {
    private final String id;
    private final String message;
    private final String author;
    private final LocalDateTime timestamp;
    private final Commit parent;
    private final Directory rootSnapshot;

    public Commit(String author, String message, Commit parent, Directory rootSnapshot) {
        this.id = UUID.randomUUID().toString().substring(0, 8); // Simple unique ID
        this.author = author;
        this.message = message;
        this.parent = parent;
        this.rootSnapshot = rootSnapshot;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getId() { return id; }
    public String getMessage() { return message; }
    public String getAuthor() { return author; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Commit getParent() { return parent; }
    public Directory getRootSnapshot() { return rootSnapshot; }
}




class Directory extends FileSystemNode {
    private Map<String, FileSystemNode> children = new HashMap<>();

    public Directory(String name) {
        super(name);
    }

    public void addChild(FileSystemNode node) {
        children.put(node.getName(), node);
    }

    public FileSystemNode getChild(String name) {
        return children.get(name);
    }

    public Map<String, FileSystemNode> getChildren() {
        return children;
    }

    @Override
    public FileSystemNode clone() {
        Directory newDir = new Directory(this.name);
        for (FileSystemNode child : this.children.values()) {
            newDir.addChild(child.clone()); // Recursively clone children
        }
        return newDir;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "+ " + name + " (Directory)");
        for (FileSystemNode child : children.values()) {
            child.print(indent + "  ");
        }
    }
}





class File extends FileSystemNode {
    private String content;

    public File(String name, String content) {
        super(name);
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public FileSystemNode clone() {
        return new File(this.name, this.content);
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "- " + name + " (File)");
    }
}




abstract class FileSystemNode {
    protected String name;

    public FileSystemNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Abstract clone method for deep copying (Prototype Pattern)
    public abstract FileSystemNode clone();

    public abstract void print(String indent);
}





class BranchManager {
    private final Map<String, Branch> branches = new HashMap<>();
    private Branch currentBranch;

    public BranchManager(Commit initialCommit) {
        // Create the main branch pointing to the initial commit
        Branch mainBranch = new Branch("main", initialCommit);
        this.branches.put("main", mainBranch);
        this.currentBranch = mainBranch;
    }

    public void createBranch(String name, Commit head) {
        if (branches.containsKey(name)) {
            System.out.println("Error: Branch '" + name + "' already exists.");
            return;
        }
        Branch newBranch = new Branch(name, head);
        branches.put(name, newBranch);
        System.out.println("Created branch '" + name + "'.");
    }

    public boolean switchBranch(String name) {
        if (!branches.containsKey(name)) {
            System.out.println("Error: Branch '" + name + "' not found.");
            return false;
        }
        this.currentBranch = branches.get(name);
        System.out.println("Switched to branch '" + name + "'.");
        return true;
    }

    public void updateHead(Commit newHead) {
        this.currentBranch.setHead(newHead);
    }

    public Branch getCurrentBranch() {
        return currentBranch;
    }
}






class CommitManager {
    private final Map<String, Commit> commits = new HashMap<>();

    public Commit createCommit(String author, String message, Commit parent, Directory rootSnapshot) {
        Commit newCommit = new Commit(author, message, parent, rootSnapshot);
        commits.put(newCommit.getId(), newCommit);
        return newCommit;
    }

    public Commit getCommit(String commitId) {
        return commits.get(commitId);
    }

    public void printHistory(Commit headCommit) {
        if (headCommit == null) {
            System.out.println("No commits in history.");
            return;
        }

        Commit current = headCommit;
        while (current != null) {
            System.out.println("Commit: " + current.getId());
            System.out.println("Author: " + current.getAuthor());
            System.out.println("Date: " + current.getTimestamp());
            System.out.println("Message: " + current.getMessage());
            System.out.println("--------------------");
            current = current.getParent();
        }
    }
}




class VersionControlSystem {
    private static VersionControlSystem instance;
    private final CommitManager commitManager;
    private final BranchManager branchManager;
    private Directory workingDirectory;

    private VersionControlSystem() {
        this.commitManager = new CommitManager();
        // Initialize with a root directory
        this.workingDirectory = new Directory("root");
        // Create the first commit (initial state)
        Commit initialCommit = commitManager.createCommit("system", "Initial commit", null, (Directory) workingDirectory.clone());
        // Initialize branch manager with the first commit
        this.branchManager = new BranchManager(initialCommit);
    }

    public static synchronized VersionControlSystem getInstance() {
        if (instance == null) {
            instance = new VersionControlSystem();
        }
        return instance;
    }

    public Directory getWorkingDirectory() {
        return workingDirectory;
    }

    public String commit(String author, String message) {
        Commit parentCommit = branchManager.getCurrentBranch().getHead();
        Directory snapshot = (Directory) workingDirectory.clone();

        Commit newCommit = commitManager.createCommit(author, message, parentCommit, snapshot);
        branchManager.updateHead(newCommit);

        System.out.println("Committed " + newCommit.getId() + " to branch " + branchManager.getCurrentBranch().getName());
        return newCommit.getId();
    }

    public void createBranch(String name) {
        Commit head = branchManager.getCurrentBranch().getHead();
        branchManager.createBranch(name, head);
    }

    public void checkoutBranch(String name) {
        boolean success = branchManager.switchBranch(name);
        if (success) {
            // On successful switch, revert working directory to the new branch's head
            Commit newHead = branchManager.getCurrentBranch().getHead();
            this.workingDirectory = (Directory) newHead.getRootSnapshot().clone();
        }
    }

    public void revert(String commitId) {
        Commit targetCommit = commitManager.getCommit(commitId);
        if (targetCommit == null) {
            System.out.println("Error: Commit '" + commitId + "' not found.");
            return;
        }
        // Note: This is a deep clone to prevent the working dir from modifying a historical snapshot
        this.workingDirectory = (Directory) targetCommit.getRootSnapshot().clone();
        branchManager.updateHead(targetCommit);

        System.out.println("Repository state reverted to commit " + commitId);
    }

    public void log() {
        System.out.println("\n--- Commit History for branch '" + branchManager.getCurrentBranch().getName() + "' ---");
        Commit headCommit = branchManager.getCurrentBranch().getHead();
        commitManager.printHistory(headCommit);
    }

    public void printCurrentState() {
        System.out.println("\n--- Current Working Directory State ---");
        workingDirectory.print("");
    }
}







import java.time.LocalDateTime;
import java.util.*;

public class VersionControlSystemDemo {
    public static void main(String[] args) {
        System.out.println("Initializing Version Control System...");
        VersionControlSystem vcs = VersionControlSystem.getInstance();

        // --- Initial State on 'main' branch ---
        vcs.printCurrentState();

        // --- First Commit ---
        System.out.println("\n1. Making initial changes and committing...");
        Directory root = vcs.getWorkingDirectory();
        root.addChild(new File("README.md", "This is a simple VCS."));
        Directory srcDir = new Directory("src");
        root.addChild(srcDir);
        srcDir.addChild(new File("Main.java", "public class Main {}"));
        String firstCommitId = vcs.commit("Alice", "Add README and initial source structure");
        vcs.printCurrentState();

        // --- Second Commit ---
        System.out.println("\n2. Modifying a file and committing again...");
        File readme = (File) root.getChild("README.md");
        readme.setContent("This is an in-memory version control system.");
        String secondCommitId = vcs.commit("Alice", "Update README documentation");
        vcs.printCurrentState();

        // --- View History ---
        vcs.log();

        // --- Branching ---
        System.out.println("\n3. Creating a new branch 'feature/add-tests'...");
        vcs.createBranch("feature/add-tests");
        vcs.checkoutBranch("feature/add-tests");

        System.out.println("\n4. Working on the new branch...");
        Directory testDir = new Directory("tests");
        root.addChild(testDir);
        testDir.addChild(new File("VCS_Test.java", "import org.junit.Test;"));
        String featureCommitId = vcs.commit("Bob", "Add test directory and initial test file");
        vcs.printCurrentState();

        // --- View history on feature branch ---
        vcs.log();

        // --- Switch back to main ---
        System.out.println("\n5. Switching back to 'main' branch...");
        vcs.checkoutBranch("main");
        // Notice the 'tests' directory is gone, as it only exists on the feature branch.
        vcs.printCurrentState();
        vcs.log(); // Log shows only main branch history

        // --- Reverting ---
        System.out.println("\n6. Reverting 'main' branch to the first commit...");
        vcs.revert(firstCommitId);
        vcs.printCurrentState(); // The README content is back to its original state.

        // --- View history after revert ---
        System.out.println("\nHistory of 'main' after reverting:");
        vcs.log(); // The head is now the first commit
    }
}



















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































