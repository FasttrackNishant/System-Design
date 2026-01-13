package trash.ztrash.findfilesinsystem;

import java.util.*;

// ==================== CORE ENTITIES ====================

abstract class FileSystemEntity {
    protected String name;
    protected Directory parent;

    public FileSystemEntity(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() { return name; }
    public Directory getParent() { return parent; }

    public abstract boolean isDirectory();

    // Template method for search
    public List<FileSystemEntity> search(SearchCriteria criteria) {
        List<FileSystemEntity> results = new ArrayList<>();
        if (criteria.matches(this)) {
            results.add(this);
        }
        return results;
    }
}

class File extends FileSystemEntity {
    private String extension;
    private long size;

    public File(String name, Directory parent, long size) {
        super(name, parent);
        this.size = size;
        this.extension = extractExtension(name);
    }

    private String extractExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex > 0) ? name.substring(dotIndex + 1) : "";
    }

    public String getExtension() { return extension; }
    public long getSize() { return size; }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String toString() {
        return "File: " + name + " (" + size + " bytes)";
    }
}

class Directory extends FileSystemEntity {
    private Map<String, FileSystemEntity> children;

    public Directory(String name, Directory parent) {
        super(name, parent);
        this.children = new HashMap<>();
    }

    public void addChild(FileSystemEntity entity) {
        children.put(entity.getName(), entity);
    }

    public FileSystemEntity getChild(String name) {
        return children.get(name);
    }

    public Collection<FileSystemEntity> getChildren() {
        return children.values();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    // Override search to include all children
    @Override
    public List<FileSystemEntity> search(SearchCriteria criteria) {
        List<FileSystemEntity> results = new ArrayList<>();

        // Check current directory
        if (criteria.matches(this)) {
            results.add(this);
        }

        // Recursively search all children
        for (FileSystemEntity child : children.values()) {
            results.addAll(child.search(criteria));
        }

        return results;
    }

    @Override
    public String toString() {
        return "Directory: " + name + " [" + children.size() + " items]";
    }
}

// ==================== SEARCH CRITERIA ====================

interface SearchCriteria {
    boolean matches(FileSystemEntity entity);
}

// Concrete criteria implementations
class NameCriteria implements SearchCriteria {
    private String searchName;
    private boolean exactMatch;

    public NameCriteria(String searchName) {
        this(searchName, false);
    }

    public NameCriteria(String searchName, boolean exactMatch) {
        this.searchName = searchName.toLowerCase();
        this.exactMatch = exactMatch;
    }

    @Override
    public boolean matches(FileSystemEntity entity) {
        String entityName = entity.getName().toLowerCase();
        if (exactMatch) {
            return entityName.equals(searchName);
        }
        return entityName.contains(searchName);
    }
}

class ExtensionCriteria implements SearchCriteria {
    private String extension;

    public ExtensionCriteria(String extension) {
        this.extension = extension.toLowerCase();
    }

    @Override
    public boolean matches(FileSystemEntity entity) {
        if (entity.isDirectory()) return false;
        File file = (File) entity;
        return file.getExtension().toLowerCase().equals(extension);
    }
}

class SizeCriteria implements SearchCriteria {
    private long minSize;
    private long maxSize;

    public SizeCriteria(long exactSize) {
        this(exactSize, exactSize);
    }

    public SizeCriteria(long minSize, long maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    @Override
    public boolean matches(FileSystemEntity entity) {
        if (entity.isDirectory()) return false;
        File file = (File) entity;
        long size = file.getSize();
        return size >= minSize && size <= maxSize;
    }
}

class CompositeCriteria implements SearchCriteria {
    private List<SearchCriteria> criteriaList;
    private Operator operator;

    public enum Operator {
        AND, OR
    }

    public CompositeCriteria(Operator operator) {
        this.criteriaList = new ArrayList<>();
        this.operator = operator;
    }

    public void addCriteria(SearchCriteria criteria) {
        criteriaList.add(criteria);
    }

    @Override
    public boolean matches(FileSystemEntity entity) {
        if (criteriaList.isEmpty()) return true;

        if (operator == Operator.AND) {
            for (SearchCriteria criteria : criteriaList) {
                if (!criteria.matches(entity)) return false;
            }
            return true;
        } else { // OR
            for (SearchCriteria criteria : criteriaList) {
                if (criteria.matches(entity)) return true;
            }
            return false;
        }
    }
}

// ==================== SEARCH SERVICE ====================

class FileSearchService {

    public List<FileSystemEntity> search(Directory root, SearchCriteria criteria) {
        if (root == null || criteria == null) {
            throw new IllegalArgumentException("Root and criteria cannot be null");
        }
        return root.search(criteria);
    }

    // Helper methods for common searches
    public List<FileSystemEntity> searchByName(Directory root, String name) {
        return search(root, new NameCriteria(name));
    }

    public List<FileSystemEntity> searchByExtension(Directory root, String extension) {
        return search(root, new ExtensionCriteria(extension));
    }

    public List<FileSystemEntity> searchBySize(Directory root, long minSize, long maxSize) {
        return search(root, new SizeCriteria(minSize, maxSize));
    }
}

// ==================== DEMO / TEST ====================

class FileSearchSystemDemo {

    // Build a sample file system
    private static Directory buildSampleFileSystem() {
        Directory root = new Directory("root", null);

        // Create directories
        Directory docs = new Directory("documents", root);
        Directory images = new Directory("images", root);
        Directory videos = new Directory("videos", root);

        root.addChild(docs);
        root.addChild(images);
        root.addChild(videos);

        // Create subdirectories
        Directory workDocs = new Directory("work", docs);
        Directory personalDocs = new Directory("personal", docs);
        docs.addChild(workDocs);
        docs.addChild(personalDocs);

        // Create files
        File file1 = new File("resume.pdf", docs, 2048);
        File file2 = new File("notes.txt", docs, 512);
        File file3 = new File("report.doc", workDocs, 1024);
        File file4 = new File("report.pdf", workDocs, 4096); // Another report with same name
        File file5 = new File("photo.jpg", images, 8192);
        File file6 = new File("video.mp4", videos, 16384);
        File file7 = new File("backup.zip", personalDocs, 3072);
        File file8 = new File("report.txt", personalDocs, 256); // Another report

        docs.addChild(file1);
        docs.addChild(file2);
        workDocs.addChild(file3);
        workDocs.addChild(file4);
        images.addChild(file5);
        videos.addChild(file6);
        personalDocs.addChild(file7);
        personalDocs.addChild(file8);

        return root;
    }

    public static void main(String[] args) {
        Directory root = buildSampleFileSystem();
        FileSearchService searchService = new FileSearchService();

        System.out.println("=== File Search System Demo ===\n");

        // Search 1: Find all entities with "report" in name
        System.out.println("1. Searching for 'report' (partial match):");
        List<FileSystemEntity> results = searchService.searchByName(root, "report");
        results.forEach(System.out::println);

        System.out.println("\n2. Searching for 'report' (exact match):");
        SearchCriteria exactName = new NameCriteria("report", true);
        results = searchService.search(root, exactName);
        results.forEach(System.out::println);

        System.out.println("\n3. Searching for PDF files:");
        results = searchService.searchByExtension(root, "pdf");
        results.forEach(System.out::println);

        System.out.println("\n4. Searching for files between 1KB and 4KB:");
        results = searchService.searchBySize(root, 1024, 4096);
        results.forEach(System.out::println);

        System.out.println("\n5. Composite search: PDF files larger than 2KB:");
        CompositeCriteria composite = new CompositeCriteria(CompositeCriteria.Operator.AND);
        composite.addCriteria(new ExtensionCriteria("pdf"));
        composite.addCriteria(new SizeCriteria(2000, Long.MAX_VALUE));
        results = searchService.search(root, composite);
        results.forEach(System.out::println);

        System.out.println("\n6. Search in specific directory (documents folder):");
        Directory docs = (Directory) root.getChild("documents");
        results = searchService.searchByName(docs, "report");
        results.forEach(System.out::println);
    }
}