class Branch {
private:
    string name;
    Commit* head;

public:
    Branch(const string& name, Commit* head) : name(name), head(head) {}

    string getName() const {
        return name;
    }

    Commit* getHead() const {
        return head;
    }

    void setHead(Commit* head) {
        this->head = head;
    }
};






class Commit {
private:
    string id;
    string message;
    string author;
    time_t timestamp;
    Commit* parent;
    Directory* rootSnapshot;

    string generateId() {
        static int counter = 0;
        stringstream ss;
        ss << "commit" << counter++;
        return ss.str();
    }

public:
    Commit(const string& author, const string& message, Commit* parent, Directory* rootSnapshot) 
        : author(author), message(message), parent(parent), rootSnapshot(rootSnapshot) {
        id = generateId();
        timestamp = time(nullptr);
    }

    ~Commit() {
        delete rootSnapshot;
    }

    string getId() const { return id; }
    string getMessage() const { return message; }
    string getAuthor() const { return author; }
    time_t getTimestamp() const { return timestamp; }
    Commit* getParent() const { return parent; }
    Directory* getRootSnapshot() const { return rootSnapshot; }
};







class Directory : public FileSystemNode {
private:
    map<string, FileSystemNode*> children;

public:
    Directory(const string& name) : FileSystemNode(name) {}

    ~Directory() {
        for (auto& pair : children) {
            delete pair.second;
        }
    }

    void addChild(FileSystemNode* node) {
        children[node->getName()] = node;
    }

    FileSystemNode* getChild(const string& name) {
        auto it = children.find(name);
        return (it != children.end()) ? it->second : nullptr;
    }

    const map<string, FileSystemNode*>& getChildren() const {
        return children;
    }

    FileSystemNode* clone() const override {
        Directory* newDir = new Directory(name);
        for (const auto& child : children) {
            newDir->addChild(child.second->clone());
        }
        return newDir;
    }

    void print(const string& indent) const override {
        cout << indent << "+ " << name << " (Directory)" << endl;
        for (const auto& child : children) {
            child.second->print(indent + "  ");
        }
    }
};





class File : public FileSystemNode {
private:
    string content;

public:
    File(const string& name, const string& content) 
        : FileSystemNode(name), content(content) {}

    string getContent() const {
        return content;
    }

    void setContent(const string& content) {
        this->content = content;
    }

    FileSystemNode* clone() const override {
        return new File(name, content);
    }

    void print(const string& indent) const override {
        cout << indent << "- " << name << " (File)" << endl;
    }
};







class FileSystemNode {
protected:
    string name;

public:
    FileSystemNode(const string& name) : name(name) {}
    virtual ~FileSystemNode() = default;

    string getName() const {
        return name;
    }

    virtual FileSystemNode* clone() const = 0;
    virtual void print(const string& indent) const = 0;
};






class BranchManager {
private:
    map<string, Branch*> branches;
    Branch* currentBranch;

public:
    BranchManager(Commit* initialCommit) {
        Branch* mainBranch = new Branch("main", initialCommit);
        branches["main"] = mainBranch;
        currentBranch = mainBranch;
    }

    ~BranchManager() {
        for (auto& pair : branches) {
            delete pair.second;
        }
    }

    void createBranch(const string& name, Commit* head) {
        if (branches.find(name) != branches.end()) {
            cout << "Error: Branch '" << name << "' already exists." << endl;
            return;
        }
        Branch* newBranch = new Branch(name, head);
        branches[name] = newBranch;
        cout << "Created branch '" << name << "'." << endl;
    }

    bool switchBranch(const string& name) {
        auto it = branches.find(name);
        if (it == branches.end()) {
            cout << "Error: Branch '" << name << "' not found." << endl;
            return false;
        }
        currentBranch = it->second;
        cout << "Switched to branch '" << name << "'." << endl;
        return true;
    }

    void updateHead(Commit* newHead) {
        currentBranch->setHead(newHead);
    }

    Branch* getCurrentBranch() {
        return currentBranch;
    }
};





class CommitManager {
private:
    map<string, Commit*> commits;

public:
    ~CommitManager() {
        for (auto& pair : commits) {
            delete pair.second;
        }
    }

    Commit* createCommit(const string& author, const string& message, Commit* parent, Directory* rootSnapshot) {
        Commit* newCommit = new Commit(author, message, parent, rootSnapshot);
        commits[newCommit->getId()] = newCommit;
        return newCommit;
    }

    Commit* getCommit(const string& commitId) {
        auto it = commits.find(commitId);
        return (it != commits.end()) ? it->second : nullptr;
    }

    void printHistory(Commit* headCommit) {
        if (headCommit == nullptr) {
            cout << "No commits in history." << endl;
            return;
        }

        Commit* current = headCommit;
        while (current != nullptr) {
            cout << "Commit: " << current->getId() << endl;
            cout << "Author: " << current->getAuthor() << endl;
            time_t timestamp = current->getTimestamp();
            cout << "Date: " << ctime(&timestamp);
            cout << "Message: " << current->getMessage() << endl;
            cout << "--------------------" << endl;
            current = current->getParent();
        }
    }
};





class VersionControlSystem {
private:
    static VersionControlSystem* instance;
    CommitManager* commitManager;
    BranchManager* branchManager;
    Directory* workingDirectory;

    VersionControlSystem() {
        commitManager = new CommitManager();
        workingDirectory = new Directory("root");
        Commit* initialCommit = commitManager->createCommit("system", "Initial commit", nullptr, 
                                                            static_cast<Directory*>(workingDirectory->clone()));
        branchManager = new BranchManager(initialCommit);
    }

public:
    ~VersionControlSystem() {
        delete commitManager;
        delete branchManager;
        delete workingDirectory;
    }

    static VersionControlSystem* getInstance() {
        if (instance == nullptr) {
            instance = new VersionControlSystem();
        }
        return instance;
    }

    Directory* getWorkingDirectory() {
        return workingDirectory;
    }

    string commit(const string& author, const string& message) {
        Commit* parentCommit = branchManager->getCurrentBranch()->getHead();
        Directory* snapshot = static_cast<Directory*>(workingDirectory->clone());

        Commit* newCommit = commitManager->createCommit(author, message, parentCommit, snapshot);
        branchManager->updateHead(newCommit);

        cout << "Committed " << newCommit->getId() << " to branch " << branchManager->getCurrentBranch()->getName() << endl;
        return newCommit->getId();
    }

    void createBranch(const string& name) {
        Commit* head = branchManager->getCurrentBranch()->getHead();
        branchManager->createBranch(name, head);
    }

    void checkoutBranch(const string& name) {
        bool success = branchManager->switchBranch(name);
        if (success) {
            Commit* newHead = branchManager->getCurrentBranch()->getHead();
            delete workingDirectory;
            workingDirectory = static_cast<Directory*>(newHead->getRootSnapshot()->clone());
        }
    }

    void revert(const string& commitId) {
        Commit* targetCommit = commitManager->getCommit(commitId);
        if (targetCommit == nullptr) {
            cout << "Error: Commit '" << commitId << "' not found." << endl;
            return;
        }
        delete workingDirectory;
        workingDirectory = static_cast<Directory*>(targetCommit->getRootSnapshot()->clone());
        branchManager->updateHead(targetCommit);

        cout << "Repository state reverted to commit " << commitId << endl;
    }

    void log() {
        cout << "\n--- Commit History for branch '" << branchManager->getCurrentBranch()->getName() << "' ---" << endl;
        Commit* headCommit = branchManager->getCurrentBranch()->getHead();
        commitManager->printHistory(headCommit);
    }

    void printCurrentState() {
        cout << "\n--- Current Working Directory State ---" << endl;
        workingDirectory->print("");
    }
};

// Static member definition
VersionControlSystem* VersionControlSystem::instance = nullptr;







class VersionControlSystemDemo {
public:
    static void main() {
        cout << "Initializing Version Control System..." << endl;
        VersionControlSystem* vcs = VersionControlSystem::getInstance();

        // --- Initial State on 'main' branch ---
        vcs->printCurrentState();

        // --- First Commit ---
        cout << "\n1. Making initial changes and committing..." << endl;
        Directory* root = vcs->getWorkingDirectory();
        root->addChild(new File("README.md", "This is a simple VCS."));
        Directory* srcDir = new Directory("src");
        root->addChild(srcDir);
        srcDir->addChild(new File("Main.java", "public class Main {}"));
        string firstCommitId = vcs->commit("Alice", "Add README and initial source structure");
        vcs->printCurrentState();

        // --- Second Commit ---
        cout << "\n2. Modifying a file and committing again..." << endl;
        File* readme = static_cast<File*>(root->getChild("README.md"));
        readme->setContent("This is an in-memory version control system.");
        string secondCommitId = vcs->commit("Alice", "Update README documentation");
        vcs->printCurrentState();

        // --- View History ---
        vcs->log();

        // --- Branching ---
        cout << "\n3. Creating a new branch 'feature/add-tests'..." << endl;
        vcs->createBranch("feature/add-tests");
        vcs->checkoutBranch("feature/add-tests");

        cout << "\n4. Working on the new branch..." << endl;
        Directory* testDir = new Directory("tests");
        root->addChild(testDir);
        testDir->addChild(new File("VCS_Test.java", "import org.junit.Test;"));
        string featureCommitId = vcs->commit("Bob", "Add test directory and initial test file");
        vcs->printCurrentState();

        // --- View history on feature branch ---
        vcs->log();

        // --- Switch back to main ---
        cout << "\n5. Switching back to 'main' branch..." << endl;
        vcs->checkoutBranch("main");
        vcs->printCurrentState();
        vcs->log();

        // --- Reverting ---
        cout << "\n6. Reverting 'main' branch to the first commit..." << endl;
        vcs->revert(firstCommitId);
        vcs->printCurrentState();

        // --- View history after revert ---
        cout << "\nHistory of 'main' after reverting:" << endl;
        vcs->log();
    }
};

int main() {
    VersionControlSystemDemo::main();
    return 0;
}












































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































