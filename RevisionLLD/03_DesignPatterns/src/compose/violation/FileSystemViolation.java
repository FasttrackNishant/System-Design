package compose.violation;

import java.util.*;

class File {
    private String name;
    private int size;

    public File(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public void printStructure(String indent) {
        System.out.println(indent + "File: " + this.name);
    }

    public void delete() {
        System.out.println("Deleting file: " + name);
    }
}

class Directory {
    private String name;
    private List<Object> contents = new ArrayList<>();

    public Directory(String name) {
        this.name = name;
    }

    public void add(Object item) {
        contents.add(item);
    }

    public int getSize() {
        int totalSize = 0;
        for (Object obj : contents) {
            if (obj instanceof File) {
                totalSize += ((File) obj).getSize();
            } else if (obj instanceof Directory) {
                totalSize += ((Directory) obj).getSize();
            }
        }
        return totalSize;
    }

    public void printStructure(String indent) {
        System.out.println(indent + name + "/");
        String childIndent = indent + "  ";

        for (Object obj : contents) {
            if (obj instanceof File) {
                ((File) obj).printStructure(childIndent);
            } else if (obj instanceof Directory) {
                ((Directory) obj).printStructure(childIndent);
            }
        }
    }

    public void delete() {
        for (Object item : contents) {
            if (item instanceof File) {
                ((File) item).delete();
            } else if (item instanceof Directory) {
                ((Directory) item).delete();
            }
        }
        System.out.println("Deleting folder: " + name);
    }
}
 class Main {
    public static void main(String[] args) {
        // Create files
        File file1 = new File("document.txt", 100);
        File file2 = new File("image.jpg", 500);
        File file3 = new File("data.csv", 50);

        // Create directories
        Directory root = new Directory("root");
        Directory docs = new Directory("documents");
        Directory images = new Directory("images");

        // Build structure
        root.add(file1);
        docs.add(file2);
        images.add(file3);
        root.add(docs);
        root.add(images);

        // Print structure
        System.out.println("=== File Structure ===");
        root.printStructure("");

        // Show total size
        System.out.println("\n=== Total Size ===");
        System.out.println("Total: " + root.getSize() + " bytes");

        // Delete
        System.out.println("\n=== Delete Operation ===");
        root.delete();
    }
}