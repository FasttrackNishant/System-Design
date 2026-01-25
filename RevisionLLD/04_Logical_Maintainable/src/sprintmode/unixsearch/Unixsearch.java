package sprintmode.unixsearch;

import java.util.ArrayList;
import java.util.List;

abstract class FileSystemNode {

    private String name;

    public FileSystemNode(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}

class File extends FileSystemNode {

    private long size;
    private String extension;

    public File(String name, long size, String extension) {
        super(name);
        this.size = size;
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String getName() {
        return super.getName();
    }
}


class Directory extends FileSystemNode {

    private List<FileSystemNode> children;

    public Directory(String name) {
        super(name);
        children = new ArrayList<>();
    }

    public void addChild(FileSystemNode node) {
        children.add(node);
    }

    public List<FileSystemNode> getChild() {
        return children;
    }

}

interface SearchStrategy {
    boolean matches(FileSystemNode node);
}

class SearchByName implements SearchStrategy {

    private String targetName;

    public SearchByName(String targetName) {
        this.targetName = targetName;
    }

    @Override
    public boolean matches(FileSystemNode node) {
        return node.getName().equals(targetName);
    }
}

class SearchByExtension implements SearchStrategy {

    private String targetExtension;

    public SearchByExtension(String targetExtension) {
        this.targetExtension = targetExtension;
    }

    @Override
    public boolean matches(FileSystemNode node) {

        File fileNode = ((File) node);
        return fileNode.getExtension().equals(targetExtension);
    }

}

class SearchBySize implements SearchStrategy {

    public long targetSize;

    public SearchBySize(long target){
        this.targetSize = target;
    }

    @Override
    public boolean matches(FileSystemNode node) {

        return node instanceof File && ((File) node).getSize() >= targetSize;
    }
}

class SearchService{

    public List<FileSystemNode> search(SearchStrategy strategy, Directory root){

        List<FileSystemNode> result = new ArrayList<>();
        dfs(root,strategy,result);
        return result;

    }

    public void dfs(FileSystemNode root , SearchStrategy strategy , List<FileSystemNode> result){

        if(strategy.matches(root)){
            result.add(root);
        }

        if(root instanceof Directory){

            for(FileSystemNode node : ((Directory)root).getChild())
            {
                dfs(node,strategy,result);
            }
        }
    }
}

class Main{

    public static void main(String[] args) {
        Directory root = new Directory("root");
        Directory docs = new Directory("docs");
        Directory images = new Directory("images");
        Directory music = new Directory("music");
        FileSystemNode file = new File("log",23,"txt");

        images.addChild(file);

        root.addChild(docs);
        docs.addChild(images);
        docs.addChild(music);

        SearchService service = new SearchService();

        List<FileSystemNode> result =  service.search(new SearchByName("log"),root);

        for(FileSystemNode node : result){
            System.out.println(node.getName());
        }




    }
}