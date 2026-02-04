package hard.inmemoryfilesystem.java;


interface Command {
    void execute();
}


class CatCommand implements Command {
    private final FileSystem fs;
    private final String path;

    public CatCommand(FileSystem fs, String path) {
        this.fs = fs;
        this.path = path;
    }

    @Override
    public void execute() {
        String content = fs.readFile(path);
        if (content != null && !content.isEmpty()) {
            System.out.println(content);
        }
    }
}



class CdCommand implements Command {
    private final FileSystem fs;
    private final String path;

    public CdCommand(FileSystem fs, String path) {
        this.fs = fs; this.path = path;
    }

    @Override public void execute() {
        fs.changeDirectory(path);
    }
}




class EchoCommand implements Command {
    private final FileSystem fs;
    private final String content;
    private final String filePath;

    public EchoCommand(FileSystem fs, String content, String filePath) {
        this.fs = fs;
        this.content = content;
        this.filePath = filePath;
    }

    @Override
    public void execute() {
        // The '>' redirection character is handled implicitly by the command's nature.
        // In a more complex shell, this would be more sophisticated.
        fs.writeToFile(filePath, content);
    }
}



class LsCommand implements Command {
    private final FileSystem fs;
    private final String path; // Path can be null, meaning "current directory"
    private final ListingStrategy strategy;

    public LsCommand(FileSystem fs, String path, ListingStrategy strategy) {
        this.fs = fs;
        this.path = path;
        this.strategy = strategy;
    }

    @Override
    public void execute() {
        if (path == null) {
            fs.listContents(strategy);
        } else {
            fs.listContents(path, strategy);
        }
    }
}



class MkdirCommand implements Command {
    private final FileSystem fs;
    private final String path;

    public MkdirCommand(FileSystem fs, String path) {
        this.fs = fs;
        this.path = path;
    }

    @Override
    public void execute() { fs.createDirectory(path); }
}




class PwdCommand implements Command {
    private final FileSystem fs;

    public PwdCommand(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void execute() {
        System.out.println(fs.getWorkingDirectory());
    }
}



class TouchCommand implements Command {
    private final FileSystem fs;
    private final String path;

    public TouchCommand(FileSystem fs, String path) {
        this.fs = fs;
        this.path = path;
    }

    @Override
    public void execute() {
        fs.createFile(path);
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
        return Collections.unmodifiableMap(children);
    }

    public FileSystemNode getChild(String name) {
        return children.get(name);
    }
}






class File extends FileSystemNode {
    private String content;

    public File(String name, Directory parent) {
        super(name, parent);
        this.content = "";
    }

    public String getContent() { return content; }

    public void setContent(String content) {
        this.content = content;
    }
}






abstract class FileSystemNode {
    protected String name;
    protected Directory parent;
    protected Instant createdTime;

    public FileSystemNode(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.createdTime = Instant.now();
    }

    public String getPath() {
        if (parent == null) { // This is the root directory
            return name;
        }
        // Avoid double slash for root's children
        if (parent.getParent() == null) {
            return parent.getPath() + name;
        }
        return parent.getPath() + "/" + name;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Directory getParent() { return parent; }
    public Instant getCreatedTime() { return createdTime; }
}








class DetailedListingStrategy implements ListingStrategy {
    @Override
    public void list(Directory directory) {
        for (FileSystemNode node : directory.getChildren().values()) {
            char type = (node instanceof Directory) ? 'd' : 'f';
            System.out.println(type + "\t" + node.getName() + "\t" + node.getCreatedTime());
        }
    }
}


interface ListingStrategy {
    void list(Directory directory);
}



class SimpleListingStrategy implements ListingStrategy {
    @Override
    public void list(Directory directory) {
        directory.getChildren().keySet().forEach(name -> System.out.print(name + "  "));
        System.out.println();
    }
}
















class FileSystem {
    private static volatile FileSystem instance;
    private final Directory root;
    private Directory currentDirectory;

    private FileSystem() {
        this.root = new Directory("/", null);
        this.currentDirectory = root;
    }

    public static FileSystem getInstance() {
        if (instance == null) {
            synchronized (FileSystem.class) {
                if (instance == null) {
                    instance = new FileSystem();
                }
            }
        }
        return instance;
    }

    public void createDirectory(String path) {
        createNode(path, true);
    }
    public void createFile(String path) {
        createNode(path, false);
    }

    public void changeDirectory(String path) {
        FileSystemNode node = getNode(path);
        if (node instanceof Directory) {
            currentDirectory = (Directory) node;
        } else {
            System.out.println("Error: '" + path + "' is not a directory.");
        }
    }

    public void listContents(ListingStrategy strategy) {
        strategy.list(currentDirectory);
    }

    public void listContents(String path, ListingStrategy strategy) {
        FileSystemNode node = getNode(path);
        if (node == null) {
            System.err.println("ls: cannot access '" + path + "': No such file or directory");
            return;
        }

        if (node instanceof Directory) {
            strategy.list((Directory) node);
        } else {
            // Mimic Unix behavior: if ls is pointed at a file, it just prints the file name.
            System.out.println(node.getName());
        }
    }

    public String getWorkingDirectory() {
        return currentDirectory.getPath();
    }

    public void writeToFile(String path, String content) {
        FileSystemNode node = getNode(path);
        if (node instanceof File) {
            ((File) node).setContent(content);
        } else {
            System.out.println("Error: Cannot write to '" + path + "'. It is not a file or does not exist.");
        }
    }

    public String readFile(String path) {
        FileSystemNode node = getNode(path);
        if (node instanceof File) {
            return ((File) node).getContent();
        }
        System.out.println("Error: Cannot read from '" + path + "'. It is not a file or does not exist.");
        return "";
    }

    // --- Private Helper Methods ---
    private void createNode(String path, boolean isDirectory) {
        String name;
        Directory parent;

        System.out.println(currentDirectory.getName());

        if (path.contains("/")) {
            // Path has directory components (e.g., "/a/b/c" or "b/c")
            int lastSlashIndex = path.lastIndexOf('/');
            name = path.substring(lastSlashIndex + 1);
            String parentPath = path.substring(0, lastSlashIndex);

            // Handle creating in root, e.g., "/testfile"
            if (parentPath.isEmpty()) {
                parentPath = "/";
            }

            FileSystemNode parentNode = getNode(parentPath);
            if (!(parentNode instanceof Directory)) {
                System.out.println("Error: Invalid path. Parent '" + parentPath + "' is not a directory or does not exist.");
                return;
            }
            parent = (Directory) parentNode;
        } else {
            // Path is a simple name in the current directory (e.g., "c")
            name = path;
            parent = currentDirectory;
        }

        if (name.isEmpty()) {
            System.err.println("Error: File or directory name cannot be empty.");
            return;
        }

        // --- Common logic from here ---
        if (parent.getChild(name) != null) {
            System.out.println("Error: Node '" + name + "' already exists in '" + parent.getPath() + "'.");
            return;
        }

        FileSystemNode newNode = isDirectory ? new Directory(name, parent) : new File(name, parent);
        parent.addChild(newNode);
    }

    private FileSystemNode getNode(String path) {
        if (path.equals("/")) return root;

        Directory startDir = path.startsWith("/") ? root : currentDirectory;
        // Use a non-empty string split to handle leading/trailing slashes gracefully
        String[] parts = path.split("/");

        FileSystemNode current = startDir;
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (!(current instanceof Directory)) {
                return null; // Part of the path is a file, so it's invalid
            }

            if (part.equals("..")) {
                current = current.getParent();
                if (current == null) current = root; // Can't go above root
            } else {
                current = current.getChild(part);
            }

            if (current == null) return null; // Path component does not exist
        }
        return current;
    }
}







import java.util.*;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class FileSystemDemo {
    public static void main(String[] args) {
        Shell shell = new Shell();
        String[] commands = {
                "pwd",                          // /
                "mkdir /home",
                "mkdir /home/user",
                "touch /home/user/file1.txt",
                "ls -l /home",                  // d user
                "cd /home/user",
                "pwd",                          // /home/user
                "ls",                           // file1.txt
                "echo 'Hello World!' > file1.txt",
                "cat file1.txt",                // Hello World!
                "echo 'Overwriting content' > file1.txt",
                "cat file1.txt",                // Overwriting content
                "mkdir documents",
                "cd documents",
                "pwd",                          // /home/user/documents
                "touch report.docx",
                "ls",                           // report.docx
                "cd ..",
                "pwd",                          // /home/user
                "ls -l",                        // d documents, f file1.txt
                "cd /",
                "pwd",                          // /
                "ls -l",                        // d home
                "cd /nonexistent/path"          // Error: not a directory
        };

        for (String command : commands) {
            System.out.println("\n$ " + command);
            shell.executeCommand(command);
        }
    }
}











class Shell {
    private final FileSystem fs;

    public Shell() {
        this.fs = FileSystem.getInstance();
    }

    public void executeCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        String commandName = parts[0];

        Command command;

        try {
            switch (commandName) {
                case "mkdir":
                    command = new MkdirCommand(fs, parts[1]);
                    break;
                case "touch":
                    command = new TouchCommand(fs, parts[1]);
                    break;
                case "cd":
                    command = new CdCommand(fs, parts[1]);
                    break;
                case "ls":
                    command = new LsCommand(fs, getPathArgumentForLs(parts), getListingStrategy(parts));
                    break;
                case "pwd":
                    command = new PwdCommand(fs);
                    break;
                case "cat":
                    command = new CatCommand(fs, parts[1]);
                    break;
                case "echo":
                    command = new EchoCommand(fs, getEchoContent(input), getEchoFilePath(parts));
                    break;
                default:
                    command = () -> System.err.println("Error: Unknown command '" + commandName + "'.");
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error: Missing argument for command '" + commandName + "'.");
            command = () -> {}; // No-op command
        }

        command.execute();
    }

    private ListingStrategy getListingStrategy(String[] args) {
        if (Arrays.asList(args).contains("-l")) {
            return new DetailedListingStrategy();
        }
        return new SimpleListingStrategy();
    }

    private String getPathArgumentForLs(String[] parts) {
        // Find the first argument that is not an option flag.
        return Arrays.stream(parts)
                .skip(1) // Skip the command name itself
                .filter(part -> !part.startsWith("-"))
                .findFirst()
                .orElse(null); // Return null if no path argument is found
    }

    private String getEchoContent(String input) {
        // Simple parsing for "echo 'content' > file"
        try {
            return input.substring(input.indexOf("'") + 1, input.lastIndexOf("'"));
        } catch (Exception e) {
            return "";
        }
    }

    private String getEchoFilePath(String[] parts) {
        // The file path is the last argument after the redirection symbol '>'
        for (int i = 0; i < parts.length; i++) {
            if (">".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i+1];
            }
        }
        return ""; // Should be handled by argument check
    }
}





























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































