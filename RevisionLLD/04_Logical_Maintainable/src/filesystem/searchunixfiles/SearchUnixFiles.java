package filesystem.searchunixfiles;

import java.util.*;

/* ===================== NODE MODEL ===================== */

abstract class Node {
    String name;

    Node(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }
}

class FileNode extends Node {
    long size;
    String extension;

    FileNode(String name, long size) {
        super(name);
        this.size = size;
        int idx = name.lastIndexOf('.');
        this.extension = (idx == -1) ? "" : name.substring(idx + 1);
    }

    long getSize() {
        return size;
    }

    String getExtension() {
        return extension;
    }
}

class DirectoryNode extends Node {
    List<Node> children = new ArrayList<>();

    DirectoryNode(String name) {
        super(name);
    }

    void add(Node node) {
        children.add(node);
    }

    List<Node> getChildren() {
        return children;
    }
}

/* ===================== SEARCH STRATEGY ===================== */

interface SearchStrategy {
    boolean matches(Node node);
}

/* -------- Strategy: Search by Name -------- */
class NameSearchStrategy implements SearchStrategy {
    private final String targetName;

    NameSearchStrategy(String targetName) {
        this.targetName = targetName;
    }

    @Override
    public boolean matches(Node node) {
        return node.getName().equals(targetName);
    }
}

/* -------- Strategy: Search by Extension -------- */
class ExtensionSearchStrategy implements SearchStrategy {
    private final String extension;

    ExtensionSearchStrategy(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean matches(Node node) {
        return node instanceof FileNode &&
                ((FileNode) node).getExtension().equals(extension);
    }
}

/* -------- Strategy: Search by File Size -------- */
class FileSizeSearchStrategy implements SearchStrategy {
    private final long minSize;

    FileSizeSearchStrategy(long minSize) {
        this.minSize = minSize;
    }

    @Override
    public boolean matches(Node node) {
        return node instanceof FileNode &&
                ((FileNode) node).getSize() >= minSize;
    }
}

/* ===================== SEARCH SERVICE ===================== */

class FileSearchService {

    public List<Node> search(DirectoryNode root, SearchStrategy strategy) {
        List<Node> result = new ArrayList<>();
        dfs(root, strategy, result);
        return result;
    }

    private void dfs(Node node, SearchStrategy strategy, List<Node> result) {

        if (strategy.matches(node)) {
            result.add(node);
        }

        if (node instanceof DirectoryNode) {
            for (Node child : ((DirectoryNode) node).getChildren()) {
                dfs(child, strategy, result);
            }
        }
    }
}

/* ===================== DEMO / TEST ===================== */

class FileSearchDemo {

    public static void main(String[] args) {

        // Build directory tree
        DirectoryNode root = new DirectoryNode("root");
        DirectoryNode docs = new DirectoryNode("docs");
        DirectoryNode images = new DirectoryNode("images");

        root.add(docs);
        root.add(images);

        docs.add(new FileNode("resume.txt", 1200));
        docs.add(new FileNode("resume.txt", 300));
        images.add(new FileNode("resume.png", 2500));

        FileSearchService searchService = new FileSearchService();

        // 1️⃣ Search by name
        List<Node> byName =
                searchService.search(root, new NameSearchStrategy("resume.txt"));

        // 2️⃣ Search by extension
        List<Node> byExtension =
                searchService.search(root, new ExtensionSearchStrategy("pdf"));

        // 3️⃣ Search by file size
        List<Node> bySize =
                searchService.search(root, new FileSizeSearchStrategy(1000));

        for(Node node : byName){
            System.out.println(node.getName());
        }
        System.out.println("By Name: " + byName.size());
        System.out.println("By Extension: " + byExtension.size());
        System.out.println("By Size: " + bySize.size());
    }
}
