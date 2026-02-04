class Branch
{
    private string name;
    private Commit head;

    public Branch(string name, Commit head)
    {
        this.name = name;
        this.head = head;
    }

    public string GetName()
    {
        return name;
    }

    public Commit GetHead()
    {
        return head;
    }

    public void SetHead(Commit head)
    {
        this.head = head;
    }
}






class Commit
{
    private readonly string id;
    private readonly string message;
    private readonly string author;
    private readonly DateTime timestamp;
    private readonly Commit parent;
    private readonly Directory rootSnapshot;

    public Commit(string author, string message, Commit parent, Directory rootSnapshot)
    {
        this.id = Guid.NewGuid().ToString().Substring(0, 8);
        this.author = author;
        this.message = message;
        this.parent = parent;
        this.rootSnapshot = rootSnapshot;
        this.timestamp = DateTime.Now;
    }

    public string GetId() { return id; }
    public string GetMessage() { return message; }
    public string GetAuthor() { return author; }
    public DateTime GetTimestamp() { return timestamp; }
    public Commit GetParent() { return parent; }
    public Directory GetRootSnapshot() { return rootSnapshot; }
}






class Directory : FileSystemNode
{
    private Dictionary<string, FileSystemNode> children = new Dictionary<string, FileSystemNode>();

    public Directory(string name) : base(name)
    {
    }

    public void AddChild(FileSystemNode node)
    {
        children[node.GetName()] = node;
    }

    public FileSystemNode GetChild(string name)
    {
        children.TryGetValue(name, out FileSystemNode node);
        return node;
    }

    public Dictionary<string, FileSystemNode> GetChildren()
    {
        return children;
    }

    public override FileSystemNode Clone()
    {
        Directory newDir = new Directory(this.name);
        foreach (FileSystemNode child in this.children.Values)
        {
            newDir.AddChild(child.Clone());
        }
        return newDir;
    }

    public override void Print(string indent)
    {
        Console.WriteLine(indent + "+ " + name + " (Directory)");
        foreach (FileSystemNode child in children.Values)
        {
            child.Print(indent + "  ");
        }
    }
}






class File : FileSystemNode
{
    private string content;

    public File(string name, string content) : base(name)
    {
        this.content = content;
    }

    public string GetContent()
    {
        return content;
    }

    public void SetContent(string content)
    {
        this.content = content;
    }

    public override FileSystemNode Clone()
    {
        return new File(this.name, this.content);
    }

    public override void Print(string indent)
    {
        Console.WriteLine(indent + "- " + name + " (File)");
    }
}






abstract class FileSystemNode
{
    protected string name;

    public FileSystemNode(string name)
    {
        this.name = name;
    }

    public string GetName()
    {
        return name;
    }

    public abstract FileSystemNode Clone();

    public abstract void Print(string indent);
}





class BranchManager
{
    private readonly Dictionary<string, Branch> branches = new Dictionary<string, Branch>();
    private Branch currentBranch;

    public BranchManager(Commit initialCommit)
    {
        Branch mainBranch = new Branch("main", initialCommit);
        this.branches["main"] = mainBranch;
        this.currentBranch = mainBranch;
    }

    public void CreateBranch(string name, Commit head)
    {
        if (branches.ContainsKey(name))
        {
            Console.WriteLine("Error: Branch '" + name + "' already exists.");
            return;
        }
        Branch newBranch = new Branch(name, head);
        branches[name] = newBranch;
        Console.WriteLine("Created branch '" + name + "'.");
    }

    public bool SwitchBranch(string name)
    {
        if (!branches.ContainsKey(name))
        {
            Console.WriteLine("Error: Branch '" + name + "' not found.");
            return false;
        }
        this.currentBranch = branches[name];
        Console.WriteLine("Switched to branch '" + name + "'.");
        return true;
    }

    public void UpdateHead(Commit newHead)
    {
        this.currentBranch.SetHead(newHead);
    }

    public Branch GetCurrentBranch()
    {
        return currentBranch;
    }
}




class CommitManager
{
    private readonly Dictionary<string, Commit> commits = new Dictionary<string, Commit>();

    public Commit CreateCommit(string author, string message, Commit parent, Directory rootSnapshot)
    {
        Commit newCommit = new Commit(author, message, parent, rootSnapshot);
        commits[newCommit.GetId()] = newCommit;
        return newCommit;
    }

    public Commit GetCommit(string commitId)
    {
        commits.TryGetValue(commitId, out Commit commit);
        return commit;
    }

    public void PrintHistory(Commit headCommit)
    {
        if (headCommit == null)
        {
            Console.WriteLine("No commits in history.");
            return;
        }

        Commit current = headCommit;
        while (current != null)
        {
            Console.WriteLine("Commit: " + current.GetId());
            Console.WriteLine("Author: " + current.GetAuthor());
            Console.WriteLine("Date: " + current.GetTimestamp());
            Console.WriteLine("Message: " + current.GetMessage());
            Console.WriteLine("--------------------");
            current = current.GetParent();
        }
    }
}





class VersionControlSystem
{
    private static VersionControlSystem instance;
    private readonly CommitManager commitManager;
    private readonly BranchManager branchManager;
    private Directory workingDirectory;

    private VersionControlSystem()
    {
        this.commitManager = new CommitManager();
        this.workingDirectory = new Directory("root");
        Commit initialCommit = commitManager.CreateCommit("system", "Initial commit", null, (Directory)workingDirectory.Clone());
        this.branchManager = new BranchManager(initialCommit);
    }

    public static VersionControlSystem GetInstance()
    {
        if (instance == null)
        {
            instance = new VersionControlSystem();
        }
        return instance;
    }

    public Directory GetWorkingDirectory()
    {
        return workingDirectory;
    }

    public string Commit(string author, string message)
    {
        Commit parentCommit = branchManager.GetCurrentBranch().GetHead();
        Directory snapshot = (Directory)workingDirectory.Clone();

        Commit newCommit = commitManager.CreateCommit(author, message, parentCommit, snapshot);
        branchManager.UpdateHead(newCommit);

        Console.WriteLine("Committed " + newCommit.GetId() + " to branch " + branchManager.GetCurrentBranch().GetName());
        return newCommit.GetId();
    }

    public void CreateBranch(string name)
    {
        Commit head = branchManager.GetCurrentBranch().GetHead();
        branchManager.CreateBranch(name, head);
    }

    public void CheckoutBranch(string name)
    {
        bool success = branchManager.SwitchBranch(name);
        if (success)
        {
            Commit newHead = branchManager.GetCurrentBranch().GetHead();
            this.workingDirectory = (Directory)newHead.GetRootSnapshot().Clone();
        }
    }

    public void Revert(string commitId)
    {
        Commit targetCommit = commitManager.GetCommit(commitId);
        if (targetCommit == null)
        {
            Console.WriteLine("Error: Commit '" + commitId + "' not found.");
            return;
        }
        this.workingDirectory = (Directory)targetCommit.GetRootSnapshot().Clone();
        branchManager.UpdateHead(targetCommit);

        Console.WriteLine("Repository state reverted to commit " + commitId);
    }

    public void Log()
    {
        Console.WriteLine("\n--- Commit History for branch '" + branchManager.GetCurrentBranch().GetName() + "' ---");
        Commit headCommit = branchManager.GetCurrentBranch().GetHead();
        commitManager.PrintHistory(headCommit);
    }

    public void PrintCurrentState()
    {
        Console.WriteLine("\n--- Current Working Directory State ---");
        workingDirectory.Print("");
    }
}






using System;
using System.Collections.Generic;

class VersionControlSystemDemo
{
    public static void Main()
    {
        Console.WriteLine("Initializing Version Control System...");
        VersionControlSystem vcs = VersionControlSystem.GetInstance();

        // --- Initial State on 'main' branch ---
        vcs.PrintCurrentState();

        // --- First Commit ---
        Console.WriteLine("\n1. Making initial changes and committing...");
        Directory root = vcs.GetWorkingDirectory();
        root.AddChild(new File("README.md", "This is a simple VCS."));
        Directory srcDir = new Directory("src");
        root.AddChild(srcDir);
        srcDir.AddChild(new File("Main.java", "public class Main {}"));
        string firstCommitId = vcs.Commit("Alice", "Add README and initial source structure");
        vcs.PrintCurrentState();

        // --- Second Commit ---
        Console.WriteLine("\n2. Modifying a file and committing again...");
        File readme = (File)root.GetChild("README.md");
        readme.SetContent("This is an in-memory version control system.");
        string secondCommitId = vcs.Commit("Alice", "Update README documentation");
        vcs.PrintCurrentState();

        // --- View History ---
        vcs.Log();

        // --- Branching ---
        Console.WriteLine("\n3. Creating a new branch 'feature/add-tests'...");
        vcs.CreateBranch("feature/add-tests");
        vcs.CheckoutBranch("feature/add-tests");

        Console.WriteLine("\n4. Working on the new branch...");
        Directory testDir = new Directory("tests");
        root.AddChild(testDir);
        testDir.AddChild(new File("VCS_Test.java", "import org.junit.Test;"));
        string featureCommitId = vcs.Commit("Bob", "Add test directory and initial test file");
        vcs.PrintCurrentState();

        // --- View history on feature branch ---
        vcs.Log();

        // --- Switch back to main ---
        Console.WriteLine("\n5. Switching back to 'main' branch...");
        vcs.CheckoutBranch("main");
        vcs.PrintCurrentState();
        vcs.Log();

        // --- Reverting ---
        Console.WriteLine("\n6. Reverting 'main' branch to the first commit...");
        vcs.Revert(firstCommitId);
        vcs.PrintCurrentState();

        // --- View history after revert ---
        Console.WriteLine("\nHistory of 'main' after reverting:");
        vcs.Log();
    }
}
















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































