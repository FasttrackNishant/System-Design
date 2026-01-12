package filesystem.commandslinux;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class FileSystemNode {
    protected String name;
    protected Directory parent;
    protected Instant createdTime;

    public FileSystemNode(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.createdTime = Instant.now();
    }

    public String getName() {
        return this.name;
    }
}

class File extends FileSystemNode {
    private String content;

    public File(String name, Directory parent) {
        super(name, parent);
        this.content = "";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

class Directory extends FileSystemNode {
    private final Map<String, FileSystemNode> children = new ConcurrentHashMap<>();

    public Directory(String name, Directory parent) {
        super(name, parent);
    }

    public void addChild(FileSystemNode node) {
        children.put(node.getName(), node);
    }

    public Map<String, FileSystemNode> getChildren() {
        return children;
    }
}

interface Command {
    void execute();
}

class MkdirCommand implements Command {
    private FileSystem fs;
    private String dirName;

    public MkdirCommand(FileSystem fs, String dirName) {
        this.fs = fs;
        this.dirName = dirName;
    }

    @Override
    public void execute() {
        fs.createDirectory(dirName);
    }
}

class TouchCommand implements Command {
    private FileSystem fs;
    private String fileName;

    public TouchCommand(FileSystem fs, String fileName) {
        this.fs = fs;
        this.fileName = fileName;
    }

    @Override
    public void execute() {
        fs.createFile(fileName);
    }
}

class LsCommand implements Command {
    private FileSystem fs;

    public LsCommand(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void execute() {
        fs.listDirectory();
    }
}

class PwdCommand implements Command {
    private FileSystem fs;

    public PwdCommand(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void execute() {
        fs.printWorkingDirectory();
    }
}

class FileSystem {
    private static FileSystem instance;
    private Directory root;
    private Directory currentDirectory;

    private FileSystem() {
        this.root = new Directory("/", null);
        this.currentDirectory = root;
    }

    public static FileSystem getInstance() {
        if (instance == null) {
            instance = new FileSystem();
        }
        return instance;
    }

    public void createDirectory(String dirName) {
        Directory newDir = new Directory(dirName, currentDirectory);
        currentDirectory.addChild(newDir);
        System.out.println("Directory created: " + dirName);
    }

    public void createFile(String fileName) {
        File newFile = new File(fileName, currentDirectory);
        currentDirectory.addChild(newFile);
        System.out.println("File created: " + fileName);
    }

    public void listDirectory() {
        Map<String, FileSystemNode> children = currentDirectory.getChildren();

        if (children.isEmpty()) {
            System.out.println("(empty)");
            return;
        }

        for (String name : children.keySet()) {
            FileSystemNode node = children.get(name);
            if (node instanceof Directory) {
                System.out.println(name + "/");
            } else {
                System.out.println(name);
            }
        }
    }

    public void printWorkingDirectory() {
        if (currentDirectory == root) {
            System.out.println("/");
        } else {
            System.out.println("/" + currentDirectory.getName());
        }
    }
}

class Shell {
    private FileSystem fs;

    public Shell() {
        this.fs = FileSystem.getInstance();
    }

    // Execute only ONE command at a time
    public void executeCommand(String commandName, String argument) {
        switch (commandName) {
            case "mkdir":
                Command mkdirCmd = new MkdirCommand(fs, argument);
                mkdirCmd.execute();
                break;

            case "touch":
                Command touchCmd = new TouchCommand(fs, argument);
                touchCmd.execute();
                break;

            case "ls":
                Command lsCmd = new LsCommand(fs);
                lsCmd.execute();
                break;

            case "pwd":
                Command pwdCmd = new PwdCommand(fs);
                pwdCmd.execute();
                break;

            default:
                System.out.println("Unknown command: " + commandName);
        }
    }
}

class Main {
    public static void main(String[] args) {
        Shell shell = new Shell();

        System.out.println("=== Testing One Command at a Time ===");

        // Execute ONE command at a time
        System.out.println("\nCommand 1: pwd");
        shell.executeCommand("pwd", "");

        System.out.println("\nCommand 2: mkdir docs");
        shell.executeCommand("mkdir", "docs");

        System.out.println("\nCommand 3: mkdir images");
        shell.executeCommand("mkdir", "images");

        System.out.println("\nCommand 4: touch file1.txt");
        shell.executeCommand("touch", "file1.txt");

        System.out.println("\nCommand 5: touch file2.txt");
        shell.executeCommand("touch", "file2.txt");

        System.out.println("\nCommand 6: ls");
        shell.executeCommand("ls", "");

        System.out.println("\nCommand 7: pwd");
        shell.executeCommand("pwd", "");
    }
}