class CatCommand(Command):
    def __init__(self, fs: 'FileSystem', path: str):
        self.fs = fs
        self.path = path

    def execute(self) -> None:
        content = self.fs.read_file(self.path)
        if content is not None and content:
            print(content)





class CdCommand(Command):
    def __init__(self, fs: 'FileSystem', path: str):
        self.fs = fs
        self.path = path

    def execute(self) -> None:
        self.fs.change_directory(self.path)




class Command(ABC):
    @abstractmethod
    def execute(self) -> None:
        pass



class EchoCommand(Command):
    def __init__(self, fs: 'FileSystem', content: str, file_path: str):
        self.fs = fs
        self.content = content
        self.file_path = file_path

    def execute(self) -> None:
        # The '>' redirection character is handled implicitly by the command's nature.
        # In a more complex shell, this would be more sophisticated.
        self.fs.write_to_file(self.file_path, self.content)





class LsCommand(Command):
    def __init__(self, fs: 'FileSystem', path: Optional[str], strategy: 'ListingStrategy'):
        self.fs = fs
        self.path = path  # Path can be None, meaning "current directory"
        self.strategy = strategy

    def execute(self) -> None:
        if self.path is None:
            self.fs.list_contents(self.strategy)
        else:
            self.fs.list_contents_path(self.path, self.strategy)





class MkdirCommand(Command):
    def __init__(self, fs: 'FileSystem', path: str):
        self.fs = fs
        self.path = path

    def execute(self) -> None:
        self.fs.create_directory(self.path)







class PwdCommand(Command):
    def __init__(self, fs: 'FileSystem'):
        self.fs = fs

    def execute(self) -> None:
        print(self.fs.get_working_directory())







class TouchCommand(Command):
    def __init__(self, fs: 'FileSystem', path: str):
        self.fs = fs
        self.path = path

    def execute(self) -> None:
        self.fs.create_file(self.path)
















class Directory(FileSystemNode):
    def __init__(self, name: str, parent: Optional['Directory']):
        super().__init__(name, parent)
        self.children: Dict[str, FileSystemNode] = {}
        self._lock = Lock()

    def add_child(self, node: FileSystemNode) -> None:
        with self._lock:
            self.children[node.get_name()] = node

    def get_children(self) -> Dict[str, FileSystemNode]:
        with self._lock:
            return self.children.copy()

    def get_child(self, name: str) -> Optional[FileSystemNode]:
        with self._lock:
            return self.children.get(name)





class FileSystemNode(ABC):
    def __init__(self, name: str, parent: Optional['Directory']):
        self.name = name
        self.parent = parent
        self.created_time = datetime.now()

    def get_path(self) -> str:
        if self.parent is None:  # This is the root directory
            return self.name
        # Avoid double slash for root's children
        if self.parent.get_parent() is None:
            return self.parent.get_path() + self.name
        return self.parent.get_path() + "/" + self.name

    def get_name(self) -> str:
        return self.name

    def set_name(self, name: str) -> None:
        self.name = name

    def get_parent(self) -> Optional['Directory']:
        return self.parent

    def get_created_time(self) -> datetime:
        return self.created_time






class File(FileSystemNode):
    def __init__(self, name: str, parent: Optional['Directory']):
        super().__init__(name, parent)
        self.content = ""

    def get_content(self) -> str:
        return self.content

    def set_content(self, content: str) -> None:
        self.content = content










class DetailedListingStrategy(ListingStrategy):
    def list(self, directory: Directory) -> None:
        children = directory.get_children()
        for node in children.values():
            node_type = 'd' if isinstance(node, Directory) else 'f'
            print(f"{node_type}\t{node.get_name()}\t{node.get_created_time()}")



class ListingStrategy(ABC):
    @abstractmethod
    def list(self, directory: Directory) -> None:
        pass





class SimpleListingStrategy(ListingStrategy):
    def list(self, directory: Directory) -> None:
        children = directory.get_children()
        for name in children.keys():
            print(name, end="  ")
        print()









def main():
    shell = Shell()
    commands = [
        "pwd",                          # /
        "mkdir /home",
        "mkdir /home/user",
        "touch /home/user/file1.txt",
        "ls -l /home",                  # d user
        "cd /home/user",
        "pwd",                          # /home/user
        "ls",                           # file1.txt
        "echo 'Hello World!' > file1.txt",
        "cat file1.txt",                # Hello World!
        "echo 'Overwriting content' > file1.txt",
        "cat file1.txt",                # Overwriting content
        "mkdir documents",
        "cd documents",
        "pwd",                          # /home/user/documents
        "touch report.docx",
        "ls",                           # report.docx
        "cd ..",
        "pwd",                          # /home/user
        "ls -l",                        # d documents, f file1.txt
        "cd /",
        "pwd",                          # /
        "ls -l",                        # d home
        "cd /nonexistent/path"          # Error: not a directory
    ]

    for command in commands:
        print(f"\n$ {command}")
        shell.execute_command(command)

if __name__ == "__main__":
    main()








class FileSystem:
    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if not hasattr(self, '_initialized') or not self._initialized:
            self.root = Directory("/", None)
            self.current_directory = self.root
            self._initialized = True

    @classmethod
    def get_instance(cls):
        return cls()

    def create_directory(self, path: str) -> None:
        self._create_node(path, True)

    def create_file(self, path: str) -> None:
        self._create_node(path, False)

    def change_directory(self, path: str) -> None:
        node = self._get_node(path)
        if isinstance(node, Directory):
            self.current_directory = node
        else:
            print(f"Error: '{path}' is not a directory.")

    def list_contents(self, strategy: ListingStrategy) -> None:
        strategy.list(self.current_directory)

    def list_contents_path(self, path: str, strategy: ListingStrategy) -> None:
        node = self._get_node(path)
        if node is None:
            print(f"ls: cannot access '{path}': No such file or directory", file=__import__('sys').stderr)
            return

        if isinstance(node, Directory):
            strategy.list(node)
        else:
            # Mimic Unix behavior: if ls is pointed at a file, it just prints the file name.
            print(node.get_name())

    def get_working_directory(self) -> str:
        return self.current_directory.get_path()

    def write_to_file(self, path: str, content: str) -> None:
        node = self._get_node(path)
        if isinstance(node, File):
            node.set_content(content)
        else:
            print(f"Error: Cannot write to '{path}'. It is not a file or does not exist.")

    def read_file(self, path: str) -> str:
        node = self._get_node(path)
        if isinstance(node, File):
            return node.get_content()
        print(f"Error: Cannot read from '{path}'. It is not a file or does not exist.")
        return ""

    def _create_node(self, path: str, is_directory: bool) -> None:
        print(self.current_directory.get_name())

        if "/" in path:
            # Path has directory components (e.g., "/a/b/c" or "b/c")
            last_slash_index = path.rfind('/')
            name = path[last_slash_index + 1:]
            parent_path = path[:last_slash_index]

            # Handle creating in root, e.g., "/testfile"
            if not parent_path:
                parent_path = "/"

            parent_node = self._get_node(parent_path)
            if not isinstance(parent_node, Directory):
                print(f"Error: Invalid path. Parent '{parent_path}' is not a directory or does not exist.")
                return
            parent = parent_node
        else:
            # Path is a simple name in the current directory (e.g., "c")
            name = path
            parent = self.current_directory

        if not name:
            print("Error: File or directory name cannot be empty.", file=__import__('sys').stderr)
            return

        # --- Common logic from here ---
        if parent.get_child(name) is not None:
            print(f"Error: Node '{name}' already exists in '{parent.get_path()}'.")
            return

        new_node = Directory(name, parent) if is_directory else File(name, parent)
        parent.add_child(new_node)

    def _get_node(self, path: str) -> Optional[FileSystemNode]:
        if path == "/":
            return self.root

        start_dir = self.root if path.startswith("/") else self.current_directory
        # Use a non-empty string split to handle leading/trailing slashes gracefully
        parts = path.split("/")

        current = start_dir
        for part in parts:
            if not part or part == ".":
                continue
            if not isinstance(current, Directory):
                return None  # Part of the path is a file, so it's invalid

            if part == "..":
                current = current.get_parent()
                if current is None:
                    current = self.root  # Can't go above root
            else:
                current = current.get_child(part)

            if current is None:
                return None  # Path component does not exist
        return current







class Shell:
    def __init__(self):
        self.fs = FileSystem.get_instance()

    def execute_command(self, input_str: str) -> None:
        parts = input_str.strip().split()
        command_name = parts[0]

        try:
            if command_name == "mkdir":
                command = MkdirCommand(self.fs, parts[1])
            elif command_name == "touch":
                command = TouchCommand(self.fs, parts[1])
            elif command_name == "cd":
                command = CdCommand(self.fs, parts[1])
            elif command_name == "ls":
                command = LsCommand(self.fs, self._get_path_argument_for_ls(parts), self._get_listing_strategy(parts))
            elif command_name == "pwd":
                command = PwdCommand(self.fs)
            elif command_name == "cat":
                command = CatCommand(self.fs, parts[1])
            elif command_name == "echo":
                command = EchoCommand(self.fs, self._get_echo_content(input_str), self._get_echo_file_path(parts))
            else:
                command = lambda: print(f"Error: Unknown command '{command_name}'.", file=__import__('sys').stderr)
        except IndexError:
            print(f"Error: Missing argument for command '{command_name}'.")
            command = lambda: None  # No-op command

        command.execute()

    def _get_listing_strategy(self, args: List[str]) -> ListingStrategy:
        if "-l" in args:
            return DetailedListingStrategy()
        return SimpleListingStrategy()

    def _get_path_argument_for_ls(self, parts: List[str]) -> Optional[str]:
        # Find the first argument that is not an option flag.
        for part in parts[1:]:  # Skip the command name itself
            if not part.startswith("-"):
                return part
        return None  # Return None if no path argument is found

    def _get_echo_content(self, input_str: str) -> str:
        # Simple parsing for "echo 'content' > file"
        try:
            start = input_str.index("'") + 1
            end = input_str.rindex("'")
            return input_str[start:end]
        except ValueError:
            return ""

    def _get_echo_file_path(self, parts: List[str]) -> str:
        # The file path is the last argument after the redirection symbol '>'
        for i in range(len(parts)):
            if parts[i] == ">" and i + 1 < len(parts):
                return parts[i + 1]
        return ""  # Should be handled by argument check











































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































