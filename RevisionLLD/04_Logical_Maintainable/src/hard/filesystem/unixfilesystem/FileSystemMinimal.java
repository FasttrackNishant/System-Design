package hard.filesystem.unixfilesystem;


import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* ===================== NODES ===================== */

abstract class FileSystemNode {
    protected String name;
    protected Directory parent;
    protected Instant createdTime;

    FileSystemNode(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.createdTime = Instant.now();
    }

    public String getName() {
        return name;
    }

    public Directory getParent() {
        return parent;
    }
}

class File extends FileSystemNode {
    private String content = "";

    File(String name, Directory parent) {
        super(name, parent);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

class Directory extends FileSystemNode {

    Map<String, FileSystemNode> children = new ConcurrentHashMap<>();

    Directory(String name, Directory parent) {
        super(name, parent);
    }

    void addChild(FileSystemNode node) {
        children.put(node.getName(), node);
    }

    FileSystemNode getChild(String name) {
        return children.get(name);
    }
}


/* ===================== FILE SYSTEM ===================== */

class FileSystem {

    private final Directory root;

    FileSystem() {
        root = new Directory("/", null);
    }

    /* ---------- RESOLVE ---------- */
    private FileSystemNode resolve(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new RuntimeException("Invalid path");
        }

        if (path.equals("/")) return root;

        String[] parts = path.split("/");
        FileSystemNode current = root;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (!(current instanceof Directory)) {
                throw new RuntimeException("Cannot traverse file: " + current.getName());
            }

            current = ((Directory) current).getChild(part);
            if (current == null) {
                throw new RuntimeException("Path not found");
            }
        }
        return current;
    }

    /* ---------- MKDIR ---------- */
    public void mkdir(String path) {
        int idx = path.lastIndexOf('/');
        String parentPath = (idx == 0) ? "/" : path.substring(0, idx);
        String dirName = path.substring(idx + 1);

        Directory parent = (Directory) resolve(parentPath);
        parent.addChild(new Directory(dirName, parent));
    }

    /* ---------- CREATE FILE ---------- */
    public void createFile(String path, String content) {
        int idx = path.lastIndexOf('/');
        String parentPath = (idx == 0) ? "/" : path.substring(0, idx);
        String fileName = path.substring(idx + 1);

        Directory parent = (Directory) resolve(parentPath);
        File file = new File(fileName, parent);
        file.setContent(content);
        parent.addChild(file);
    }

    /* ---------- CAT ---------- */
    public void readFile(String path) {
        FileSystemNode node = resolve(path);

        if (!(node instanceof File)) {
            throw new RuntimeException("Not a file");
        }

        System.out.println(((File) node).getContent());
    }

    /* ---------- LS ---------- */
    public void list(String path) {
        FileSystemNode node = resolve(path);

        if (node instanceof File) {
            System.out.println(node.getName());
            return;
        }

        Directory dir = (Directory) node;
        for (String name : dir.children.keySet()) {
            System.out.println(name);
        }
    }

    /* ---------- RM ---------- */
    public void removeFile(String path) {
        int idx = path.lastIndexOf('/');
        String parentPath = (idx == 0) ? "/" : path.substring(0, idx);
        String fileName = path.substring(idx + 1);

        Directory parent = (Directory) resolve(parentPath);
        FileSystemNode node = parent.getChild(fileName);

        if (!(node instanceof File)) {
            throw new RuntimeException("Not a file");
        }

        parent.children.remove(fileName);
    }
}


class Demo {
    public static void main(String[] args) {
        FileSystem fs = new FileSystem();

        fs.mkdir("/home");
        fs.mkdir("/home/user");
        fs.mkdir("/home/dev");
        fs.createFile("/home/user/file.txt", "Hello Interview 123");

        fs.list("/home");           // user
        fs.list("/home/user");      // file.txt
        fs.readFile("/home/user/file.txt");
        fs.removeFile("/home/user/file.txt");
        fs.list("/home/user");      // empty
    }
}
