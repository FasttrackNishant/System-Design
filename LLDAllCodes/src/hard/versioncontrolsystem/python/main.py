class Branch:
    def __init__(self, name: str, head: Commit):
        self.name = name
        self.head = head

    def get_name(self) -> str:
        return self.name

    def get_head(self) -> Commit:
        return self.head

    def set_head(self, head: Commit):
        self.head = head






class Commit:
    def __init__(self, author: str, message: str, parent: Optional['Commit'], root_snapshot: Directory):
        self.id = str(uuid.uuid4())[:8]
        self.author = author
        self.message = message
        self.parent = parent
        self.root_snapshot = root_snapshot
        self.timestamp = datetime.now()

    def get_id(self) -> str:
        return self.id

    def get_message(self) -> str:
        return self.message

    def get_author(self) -> str:
        return self.author

    def get_timestamp(self) -> datetime:
        return self.timestamp

    def get_parent(self) -> Optional['Commit']:
        return self.parent

    def get_root_snapshot(self) -> Directory:
        return self.root_snapshot








class Directory(FileSystemNode):
    def __init__(self, name: str):
        super().__init__(name)
        self.children: Dict[str, FileSystemNode] = {}

    def add_child(self, node: FileSystemNode):
        self.children[node.get_name()] = node

    def get_child(self, name: str) -> Optional[FileSystemNode]:
        return self.children.get(name)

    def get_children(self) -> Dict[str, FileSystemNode]:
        return self.children

    def clone(self) -> 'FileSystemNode':
        new_dir = Directory(self.name)
        for child in self.children.values():
            new_dir.add_child(child.clone())
        return new_dir

    def print(self, indent: str):
        print(f"{indent}+ {self.name} (Directory)")
        for child in self.children.values():
            child.print(indent + "  ")







class FileSystemNode(ABC):
    def __init__(self, name: str):
        self.name = name

    def get_name(self) -> str:
        return self.name

    @abstractmethod
    def clone(self) -> 'FileSystemNode':
        pass

    @abstractmethod
    def print(self, indent: str):
        pass








class File(FileSystemNode):
    def __init__(self, name: str, content: str):
        super().__init__(name)
        self.content = content

    def get_content(self) -> str:
        return self.content

    def set_content(self, content: str):
        self.content = content

    def clone(self) -> 'FileSystemNode':
        return File(self.name, self.content)

    def print(self, indent: str):
        print(f"{indent}- {self.name} (File)")








class BranchManager:
    def __init__(self, initial_commit: Commit):
        self.branches: Dict[str, Branch] = {}
        main_branch = Branch("main", initial_commit)
        self.branches["main"] = main_branch
        self.current_branch = main_branch

    def create_branch(self, name: str, head: Commit):
        if name in self.branches:
            print(f"Error: Branch '{name}' already exists.")
            return
        new_branch = Branch(name, head)
        self.branches[name] = new_branch
        print(f"Created branch '{name}'.")

    def switch_branch(self, name: str) -> bool:
        if name not in self.branches:
            print(f"Error: Branch '{name}' not found.")
            return False
        self.current_branch = self.branches[name]
        print(f"Switched to branch '{name}'.")
        return True

    def update_head(self, new_head: Commit):
        self.current_branch.set_head(new_head)

    def get_current_branch(self) -> Branch:
        return self.current_branch









class CommitManager:
    def __init__(self):
        self.commits: Dict[str, Commit] = {}

    def create_commit(self, author: str, message: str, parent: Optional[Commit], root_snapshot: Directory) -> Commit:
        new_commit = Commit(author, message, parent, root_snapshot)
        self.commits[new_commit.get_id()] = new_commit
        return new_commit

    def get_commit(self, commit_id: str) -> Optional[Commit]:
        return self.commits.get(commit_id)

    def print_history(self, head_commit: Optional[Commit]):
        if head_commit is None:
            print("No commits in history.")
            return

        current = head_commit
        while current is not None:
            print(f"Commit: {current.get_id()}")
            print(f"Author: {current.get_author()}")
            print(f"Date: {current.get_timestamp()}")
            print(f"Message: {current.get_message()}")
            print("--------------------")
            current = current.get_parent()









class VersionControlSystemDemo:
    @staticmethod
    def main():
        print("Initializing Version Control System...")
        vcs = VersionControlSystem.get_instance()

        # --- Initial State on 'main' branch ---
        vcs.print_current_state()

        # --- First Commit ---
        print("\n1. Making initial changes and committing...")
        root = vcs.get_working_directory()
        root.add_child(File("README.md", "This is a simple VCS."))
        src_dir = Directory("src")
        root.add_child(src_dir)
        src_dir.add_child(File("Main.java", "public class Main {}"))
        first_commit_id = vcs.commit("Alice", "Add README and initial source structure")
        vcs.print_current_state()

        # --- Second Commit ---
        print("\n2. Modifying a file and committing again...")
        readme = root.get_child("README.md")
        readme.set_content("This is an in-memory version control system.")
        second_commit_id = vcs.commit("Alice", "Update README documentation")
        vcs.print_current_state()

        # --- View History ---
        vcs.log()

        # --- Branching ---
        print("\n3. Creating a new branch 'feature/add-tests'...")
        vcs.create_branch("feature/add-tests")
        vcs.checkout_branch("feature/add-tests")

        print("\n4. Working on the new branch...")
        test_dir = Directory("tests")
        root.add_child(test_dir)
        test_dir.add_child(File("VCS_Test.java", "import org.junit.Test;"))
        feature_commit_id = vcs.commit("Bob", "Add test directory and initial test file")
        vcs.print_current_state()

        # --- View history on feature branch ---
        vcs.log()

        # --- Switch back to main ---
        print("\n5. Switching back to 'main' branch...")
        vcs.checkout_branch("main")
        # Notice the 'tests' directory is gone, as it only exists on the feature branch.
        vcs.print_current_state()
        vcs.log()  # Log shows only main branch history

        # --- Reverting ---
        print("\n6. Reverting 'main' branch to the first commit...")
        vcs.revert(first_commit_id)
        vcs.print_current_state()  # The README content is back to its original state.

        # --- View history after revert ---
        print("\nHistory of 'main' after reverting:")
        vcs.log()  # The head is now the first commit


if __name__ == "__main__":
    VersionControlSystemDemo.main()










class VersionControlSystem:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if hasattr(self, '_initialized'):
            return
        self._initialized = True
        self.commit_manager = CommitManager()
        self.working_directory = Directory("root")
        initial_commit = self.commit_manager.create_commit("system", "Initial commit", None, self.working_directory.clone())
        self.branch_manager = BranchManager(initial_commit)

    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def get_working_directory(self) -> Directory:
        return self.working_directory

    def commit(self, author: str, message: str) -> str:
        parent_commit = self.branch_manager.get_current_branch().get_head()
        snapshot = self.working_directory.clone()

        new_commit = self.commit_manager.create_commit(author, message, parent_commit, snapshot)
        self.branch_manager.update_head(new_commit)

        print(f"Committed {new_commit.get_id()} to branch {self.branch_manager.get_current_branch().get_name()}")
        return new_commit.get_id()

    def create_branch(self, name: str):
        head = self.branch_manager.get_current_branch().get_head()
        self.branch_manager.create_branch(name, head)

    def checkout_branch(self, name: str):
        success = self.branch_manager.switch_branch(name)
        if success:
            new_head = self.branch_manager.get_current_branch().get_head()
            self.working_directory = new_head.get_root_snapshot().clone()

    def revert(self, commit_id: str):
        target_commit = self.commit_manager.get_commit(commit_id)
        if target_commit is None:
            print(f"Error: Commit '{commit_id}' not found.")
            return
        self.working_directory = target_commit.get_root_snapshot().clone()
        self.branch_manager.update_head(target_commit)

        print(f"Repository state reverted to commit {commit_id}")

    def log(self):
        print(f"\n--- Commit History for branch '{self.branch_manager.get_current_branch().get_name()}' ---")
        head_commit = self.branch_manager.get_current_branch().get_head()
        self.commit_manager.print_history(head_commit)

    def print_current_state(self):
        print("\n--- Current Working Directory State ---")
        self.working_directory.print("")



























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































