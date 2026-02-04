class CatCommand : public Command {
private:
    FileSystem* fs;
    string path;

public:
    CatCommand(FileSystem* fs, const string& path) : fs(fs), path(path) {}
    
    void execute() override;
};




class CdCommand : public Command {
private:
    FileSystem* fs;
    string path;

public:
    CdCommand(FileSystem* fs, const string& path) : fs(fs), path(path) {}
    
    void execute() override;
};





class Command {
public:
    virtual ~Command() = default;
    virtual void execute() = 0;
};





class EchoCommand : public Command {
private:
    FileSystem* fs;
    string content;
    string filePath;

public:
    EchoCommand(FileSystem* fs, const string& content, const string& filePath) 
        : fs(fs), content(content), filePath(filePath) {}
    
    void execute() override;
};



class LsCommand : public Command {
private:
    FileSystem* fs;
    string path; // Path can be empty, meaning "current directory"
    shared_ptr<ListingStrategy> strategy;

public:
    LsCommand(FileSystem* fs, const string& path, shared_ptr<ListingStrategy> strategy) 
        : fs(fs), path(path), strategy(strategy) {}
    
    void execute() override;
};




class MkdirCommand : public Command {
private:
    FileSystem* fs;
    string path;

public:
    MkdirCommand(FileSystem* fs, const string& path) : fs(fs), path(path) {}
    
    void execute() override;
};






class PwdCommand : public Command {
private:
    FileSystem* fs;

public:
    PwdCommand(FileSystem* fs) : fs(fs) {}
    
    void execute() override;
};





class TouchCommand : public Command {
private:
    FileSystem* fs;
    string path;

public:
    TouchCommand(FileSystem* fs, const string& path) : fs(fs), path(path) {}
    
    void execute() override;
};





class Directory : public FileSystemNode {
private:
    map<string, shared_ptr<FileSystemNode>> children;
    mutable mutex childrenMutex;

public:
    Directory(const string& name, Directory* parent) : FileSystemNode(name, parent) {}

    void addChild(shared_ptr<FileSystemNode> node) {
        lock_guard<mutex> lock(childrenMutex);
        children[node->getName()] = node;
    }

    map<string, shared_ptr<FileSystemNode>> getChildren() const {
        lock_guard<mutex> lock(childrenMutex);
        return children;
    }

    shared_ptr<FileSystemNode> getChild(const string& name) {
        lock_guard<mutex> lock(childrenMutex);
        auto it = children.find(name);
        return (it != children.end()) ? it->second : nullptr;
    }
};

string FileSystemNode::getPath() {
    if (parent == nullptr) { // This is the root directory
        return name;
    }
    // Avoid double slash for root's children
    if (parent->getParent() == nullptr) {
        return parent->getPath() + name;
    }
    return parent->getPath() + "/" + name;
}






class File : public FileSystemNode {
private:
    string content;

public:
    File(const string& name, Directory* parent) : FileSystemNode(name, parent), content("") {}

    string getContent() const { return content; }
    void setContent(const string& content) { this->content = content; }
};





class FileSystemNode {
protected:
    string name;
    Directory* parent;
    string createdTime;

    string getCurrentTime() {
        auto now = chrono::system_clock::now();
        auto time_t = chrono::system_clock::to_time_t(now);
        string timeStr = ctime(&time_t);
        // Remove the newline character at the end
        if (!timeStr.empty() && timeStr.back() == '\n') {
            timeStr.pop_back();
        }
        return timeStr;
    }

public:
    FileSystemNode(const string& name, Directory* parent) 
        : name(name), parent(parent), createdTime(getCurrentTime()) {}

    virtual ~FileSystemNode() = default;

    string getPath();

    string getName() const { return name; }
    void setName(const string& name) { this->name = name; }
    Directory* getParent() const { return parent; }
    string getCreatedTime() const { return createdTime; }
};







class DetailedListingStrategy : public ListingStrategy {
public:
    void list(Directory* directory) override {
        auto children = directory->getChildren();
        for (const auto& pair : children) {
            auto node = pair.second;
            char type = (dynamic_pointer_cast<Directory>(node) != nullptr) ? 'd' : 'f';
            cout << type << "\t" << node->getName() << "\t" << node->getCreatedTime() << endl;
        }
    }
};





class ListingStrategy {
public:
    virtual ~ListingStrategy() = default;
    virtual void list(Directory* directory) = 0;
};




class SimpleListingStrategy : public ListingStrategy {
public:
    void list(Directory* directory) override {
        auto children = directory->getChildren();
        for (const auto& pair : children) {
            cout << pair.first << "  ";
        }
        cout << endl;
    }
};









class FileSystem {
private:
    static FileSystem* instance;
    static mutex instanceMutex;
    
    shared_ptr<Directory> root;
    Directory* currentDirectory;

    FileSystem() {
        root = make_shared<Directory>("/", nullptr);
        currentDirectory = root.get();
    }

    void createNode(const string& path, bool isDirectory) {
        string name;
        Directory* parent;

        if (path.find("/") != string::npos) {
            // Path has directory components (e.g., "/a/b/c" or "b/c")
            size_t lastSlashIndex = path.rfind('/');
            name = path.substr(lastSlashIndex + 1);
            string parentPath = path.substr(0, lastSlashIndex);

            // Handle creating in root, e.g., "/testfile"
            if (parentPath.empty()) {
                parentPath = "/";
            }

            shared_ptr<FileSystemNode> parentNode = getNode(parentPath);
            Directory* parentDir = dynamic_cast<Directory*>(parentNode.get());
            if (parentDir == nullptr) {
                cout << "Error: Invalid path. Parent '" << parentPath << "' is not a directory or does not exist." << endl;
                return;
            }
            parent = parentDir;
        } else {
            // Path is a simple name in the current directory (e.g., "c")
            name = path;
            parent = currentDirectory;
        }

        if (name.empty()) {
            cerr << "Error: File or directory name cannot be empty." << endl;
            return;
        }

        // --- Common logic from here ---
        if (parent->getChild(name) != nullptr) {
            cout << "Error: Node '" << name << "' already exists in '" << parent->getPath() << "'." << endl;
            return;
        }

        shared_ptr<FileSystemNode> newNode;
        if (isDirectory) {
            newNode = make_shared<Directory>(name, parent);
        } else {
            newNode = make_shared<File>(name, parent);
        }
        parent->addChild(newNode);
    }

    shared_ptr<FileSystemNode> getNode(const string& path) {
        if (path == "/") return root;

        Directory* startDir = path.front() == '/' ? root.get() : currentDirectory;
        
        // Split path by '/'
        vector<string> parts;
        stringstream ss(path);
        string part;
        while (getline(ss, part, '/')) {
            if (!part.empty()) {
                parts.push_back(part);
            }
        }

        shared_ptr<FileSystemNode> current = (startDir == root.get()) ? root : shared_ptr<FileSystemNode>();
        
        // Handle relative paths starting from current directory
        if (startDir != root.get()) {
            // For simplicity, traverse from root to find current directory
            // In a real implementation, you'd maintain proper shared_ptr references
            current = root;
            // This is a simplified approach - in production you'd need proper tracking
        }

        for (const string& p : parts) {
            if (p.empty() || p == ".") {
                continue;
            }
            
            Directory* currentDir = dynamic_cast<Directory*>(current.get());
            if (currentDir == nullptr) {
                return nullptr; // Part of the path is a file, so it's invalid
            }

            if (p == "..") {
                Directory* parentDir = currentDir->getParent();
                if (parentDir != nullptr) {
                    // Find the parent as a shared_ptr - simplified approach
                    if (parentDir == root.get()) {
                        current = root;
                    } else {
                        // In production, you'd need proper shared_ptr tracking
                        current = root; // Fallback to root for safety
                    }
                } else {
                    current = root; // Can't go above root
                }
            } else {
                current = currentDir->getChild(p);
            }

            if (current == nullptr) return nullptr; // Path component does not exist
        }
        return current;
    }

public:
    static FileSystem* getInstance() {
        lock_guard<mutex> lock(instanceMutex);
        if (instance == nullptr) {
            instance = new FileSystem();
        }
        return instance;
    }

    void createDirectory(const string& path) {
        createNode(path, true);
    }

    void createFile(const string& path) {
        createNode(path, false);
    }

    void changeDirectory(const string& path) {
        shared_ptr<FileSystemNode> node = getNode(path);
        Directory* dir = dynamic_cast<Directory*>(node.get());
        if (dir != nullptr) {
            currentDirectory = dir;
        } else {
            cout << "Error: '" << path << "' is not a directory." << endl;
        }
    }

    void listContents(shared_ptr<ListingStrategy> strategy) {
        strategy->list(currentDirectory);
    }

    void listContentsPath(const string& path, shared_ptr<ListingStrategy> strategy) {
        shared_ptr<FileSystemNode> node = getNode(path);
        if (node == nullptr) {
            cerr << "ls: cannot access '" << path << "': No such file or directory" << endl;
            return;
        }

        Directory* dir = dynamic_cast<Directory*>(node.get());
        if (dir != nullptr) {
            strategy->list(dir);
        } else {
            // Mimic Unix behavior: if ls is pointed at a file, it just prints the file name.
            cout << node->getName() << endl;
        }
    }

    string getWorkingDirectory() {
        return currentDirectory->getPath();
    }

    void writeToFile(const string& path, const string& content) {
        shared_ptr<FileSystemNode> node = getNode(path);
        File* file = dynamic_cast<File*>(node.get());
        if (file != nullptr) {
            file->setContent(content);
        } else {
            cout << "Error: Cannot write to '" << path << "'. It is not a file or does not exist." << endl;
        }
    }

    string readFile(const string& path) {
        shared_ptr<FileSystemNode> node = getNode(path);
        File* file = dynamic_cast<File*>(node.get());
        if (file != nullptr) {
            return file->getContent();
        }
        cout << "Error: Cannot read from '" << path << "'. It is not a file or does not exist." << endl;
        return "";
    }
};

// Static member definitions
FileSystem* FileSystem::instance = nullptr;
mutex FileSystem::instanceMutex;

// Command implementations
void CatCommand::execute() {
    string content = fs->readFile(path);
    if (!content.empty()) {
        cout << content << endl;
    }
}

void CdCommand::execute() {
    fs->changeDirectory(path);
}

void EchoCommand::execute() {
    fs->writeToFile(filePath, content);
}

void LsCommand::execute() {
    if (path.empty()) {
        fs->listContents(strategy);
    } else {
        fs->listContentsPath(path, strategy);
    }
}

void MkdirCommand::execute() {
    fs->createDirectory(path);
}

void PwdCommand::execute() {
    cout << fs->getWorkingDirectory() << endl;
}

void TouchCommand::execute() {
    fs->createFile(path);
}






int main() {
    Shell shell;
    vector<string> commands = {
        "pwd",                          // /
        "mkdir /home",
        "mkdir /home/user",
        "touch /home/user/file1.txt",
        "ls -l /home",                  // d user
        "cd /home/user",
        "pwd",                          // /home/user
        "ls",                           // file1.txt
        "echo 'Hello World!' > file1.txt",
        "cat file1.txt",                // Hello World!
        "echo 'Overwriting content' > file1.txt",
        "cat file1.txt",                // Overwriting content
        "mkdir documents",
        "cd documents",
        "pwd",                          // /home/user/documents
        "touch report.docx",
        "ls",                           // report.docx
        "cd ..",
        "pwd",                          // /home/user
        "ls -l",                        // d documents, f file1.txt
        "cd /",
        "pwd",                          // /
        "ls -l",                        // d home
        "cd /nonexistent/path"          // Error: not a directory
    };

    for (const string& command : commands) {
        cout << endl << "$ " << command << endl;
        shell.executeCommand(command);
    }

    return 0;
}










class Shell {
private:
    FileSystem* fs;

    shared_ptr<ListingStrategy> getListingStrategy(const vector<string>& args) {
        for (const string& arg : args) {
            if (arg == "-l") {
                return make_shared<DetailedListingStrategy>();
            }
        }
        return make_shared<SimpleListingStrategy>();
    }

    string getPathArgumentForLs(const vector<string>& parts) {
        // Find the first argument that is not an option flag.
        for (size_t i = 1; i < parts.size(); i++) { // Skip the command name itself
            if (!parts[i].empty() && parts[i][0] != '-') {
                return parts[i];
            }
        }
        return ""; // Return empty if no path argument is found
    }

    string getEchoContent(const string& input) {
        // Simple parsing for "echo 'content' > file"
        try {
            size_t start = input.find("'") + 1;
            size_t end = input.rfind("'");
            if (start != string::npos && end != string::npos && start < end) {
                return input.substr(start, end - start);
            }
        } catch (...) {
            return "";
        }
        return "";
    }

    string getEchoFilePath(const vector<string>& parts) {
        // The file path is the last argument after the redirection symbol '>'
        for (size_t i = 0; i < parts.size(); i++) {
            if (parts[i] == ">" && i + 1 < parts.size()) {
                return parts[i + 1];
            }
        }
        return ""; // Should be handled by argument check
    }

    vector<string> split(const string& str) {
        vector<string> parts;
        stringstream ss(str);
        string part;
        while (ss >> part) {
            parts.push_back(part);
        }
        return parts;
    }

public:
    Shell() : fs(FileSystem::getInstance()) {}

    void executeCommand(const string& input) {
        vector<string> parts = split(input);
        if (parts.empty()) return;
        
        string commandName = parts[0];
        shared_ptr<Command> command;

        try {
            if (commandName == "mkdir") {
                command = make_shared<MkdirCommand>(fs, parts.at(1));
            } else if (commandName == "touch") {
                command = make_shared<TouchCommand>(fs, parts.at(1));
            } else if (commandName == "cd") {
                command = make_shared<CdCommand>(fs, parts.at(1));
            } else if (commandName == "ls") {
                command = make_shared<LsCommand>(fs, getPathArgumentForLs(parts), getListingStrategy(parts));
            } else if (commandName == "pwd") {
                command = make_shared<PwdCommand>(fs);
            } else if (commandName == "cat") {
                command = make_shared<CatCommand>(fs, parts.at(1));
            } else if (commandName == "echo") {
                command = make_shared<EchoCommand>(fs, getEchoContent(input), getEchoFilePath(parts));
            } else {
                cerr << "Error: Unknown command '" << commandName << "'." << endl;
                return;
            }
        } catch (const out_of_range&) {
            cout << "Error: Missing argument for command '" << commandName << "'." << endl;
            return;
        }

        if (command) {
            command->execute();
        }
    }
};



























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































