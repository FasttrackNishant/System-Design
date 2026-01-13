package trash.ztrash.createandlistfiles;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class FileSystemNode{
    protected String name;
    protected Directory parent;
    protected Instant createdTime;

    public FileSystemNode(String name, Directory parent){
        this.name = name;
        this.parent = parent;
    }

    public String getName(){
        return this.name;
    }

}


class File extends FileSystemNode{

    private String content;

    public File(String name , Directory parent){
        super(name,parent);
        this.content = "";
    }

}

class Directory extends FileSystemNode{

    private final Map<String,FileSystemNode> children = new ConcurrentHashMap<>();

    public Directory(String name , Directory parent){
        super(name,parent);
    }

    public void addChild(FileSystemNode node){
        children.put(node.getName(),node);
    }

}

interface Command{
    void execute();
}

class CatCommand implements Command{

    private FileSystem fs;
    private String path;

    public CatCommand(FileSystem fs , String path){
        this.fs = fs;
        this.path = path;
    }

    @Override
    public void execute() {

    }
}


class FileSystem{

    private static FileSystem instance;
    private Directory root;
    private Directory currentDirectory;

    private FileSystem(){
        this.root = new Directory("/",null);
        currentDirectory = root;
    }

    public static FileSystem getInstance(){
        if(instance == null){
            instance = new FileSystem();
        }
        return  instance;
    }

    public void readFile(String path){

    }
}

class Shell{
    private FileSystem fs;
}
