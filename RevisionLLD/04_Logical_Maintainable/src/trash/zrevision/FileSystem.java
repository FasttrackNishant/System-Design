package trash.zrevision;

import java.util.HashMap;
import java.util.Map;


abstract class FileSystemNode {
    private String name;
    private Directory parent;

    public FileSystemNode(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return this.name;
    }
}

class File extends FileSystemNode {

    private String fileContent;

    public File(String name, Directory parent) {
        super(name, parent);
    }

    public String getContent() {
        return this.getContent();
    }

    public void setFileContent(String content) {
        this.fileContent = content;
    }

}

class Directory extends FileSystemNode {

    Map<String, FileSystemNode> children = new HashMap<>();

    public Directory(String name, Directory parent) {
        super(name, parent);
    }

    public void addChild(FileSystemNode node) {
        children.put(node.getName(), node);
    }

    FileSystemNode getChild(String name){

        FileSystemNode child  = children.get(name);

        if(child == null){
            throw new RuntimeException("Path not found");
        }
        return child;
    }

}

class FileSystem{

    private final Directory root;

    public FileSystem(){

        root = new Directory("/",null);





    }

}