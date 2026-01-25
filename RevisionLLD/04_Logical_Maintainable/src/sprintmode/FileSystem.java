package sprintmode;


// node
// file -> node
// directory -> node

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class FileSystemNode {

    protected String name;
    protected Directory parent;

    FileSystemNode(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return this.name;
    }

    public Directory getParent() {
        return parent;
    }
}

class File extends FileSystemNode {

    private String content = "";

    public File(String name, Directory parent) {
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

    Map<String, FileSystemNode> children;

    public Directory(String name, Directory parent) {
        super(name, parent);
        this.children = new ConcurrentHashMap<>();
    }

    public void addChild(FileSystemNode node) {
        children.put(node.getName(), node);
    }

    public FileSystemNode getChild(String name) {
        return children.get(name);
    }

}

class FileSystem {

    private final Directory root;

    public FileSystem() {
        root = new Directory("/", null);
    }

    private FileSystemNode resolve(String filePath) {

        if (filePath == null) {
            throw new RuntimeException("Invalid Path");
        }

        if (filePath.equals("/")) return root;

        String[] parts = filePath.split("/");
        FileSystemNode current = root;

        for (String path : parts) {
            if (path.isEmpty())
                continue;

            if (!(current instanceof Directory)) {
                throw new RuntimeException("Can not traverse file " + current.getName());
            }

            current = ((Directory) current).getChild(path);

            if (current == null) {
                throw new RuntimeException("Path not found");
            }
        }

        return current;
    }

    public void mkdir(String name , String path) {

        Directory directory = ((Directory) resolve(path));

        FileSystemNode newDirectory = new Directory(name,directory);

        directory.addChild(newDirectory);

    }

    public void readFile(String filePath,String fileName) {
        Directory node = ((Directory) resolve(filePath));

        FileSystemNode child = node.getChild(fileName);
        System.out.println(((File)child).getContent());
    }

    public void createFile(String name, String filepath, String content) {

        Directory node = (Directory) resolve(filepath);
        File newFile = new File(name, node);

        newFile.setContent(content);
        node.addChild(newFile);

    }

    public void list(String filePath){

        Directory directory = ((Directory) resolve(filePath));

        for(String name : directory.children.keySet())
        {
            System.out.println(name);
        }

    }

}

class Main {

    public static void main(String[] args) {

        FileSystem system = new FileSystem();
        system.mkdir("Pictures","/");

        system.createFile("Log.txt","/","Prod logs");

        system.mkdir("Personal","/Pictures");
        system.list("/");

        system.readFile("/","Log.txt");


    }

}