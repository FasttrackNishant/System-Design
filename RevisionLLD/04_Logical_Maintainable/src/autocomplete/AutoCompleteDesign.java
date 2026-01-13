package autocomplete;

import java.util.*;

class TrieNode {
    private Map<Character, TrieNode> children;
    private boolean isTerminal;
    private int frequency;

    public TrieNode() {
        this.children = new HashMap<>();
        this.isTerminal = false;
        this.frequency = 0;
    }

    public Map<Character, TrieNode> getChildren() {
        return this.children;
    }

    public void setTerminal(boolean flag) {
        this.isTerminal = flag;
    }

    public boolean getTerminal() {
        return this.isTerminal;
    }

    public void increaseFreq() {
        this.frequency++;
    }

    public int getFrequency() {
        return this.frequency;
    }
}

class Suggestion {
    private String word;
    private int weight;

    public Suggestion(String word, int weight) {
        this.word = word;
        this.weight = weight;
    }

    public String getWord() {
        return this.word;
    }

    public int getWeight() {
        return this.weight;
    }
}

class Trie {
    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    private void collect(TrieNode node, String prefix, List<Suggestion> output) {
        if (node.getTerminal()) {
            output.add(new Suggestion(prefix, node.getFrequency()));
        }

        for (Map.Entry<Character, TrieNode> entry : node.getChildren().entrySet()) {
            collect(entry.getValue(), prefix + entry.getKey(), output);
        }
    }

    public void insert(String word) {
        TrieNode curr = root;

        for (char ch : word.toCharArray()) {
            if (!curr.getChildren().containsKey(ch)) {
                curr.getChildren().put(ch, new TrieNode());
            }
            curr = curr.getChildren().get(ch);
        }

        curr.setTerminal(true);
        curr.increaseFreq();
    }

    public TrieNode searchPrefix(String prefix) {
        TrieNode current = root;

        for (char ch : prefix.toCharArray()) {
            if (!current.getChildren().containsKey(ch)) {
                return null;
            }
            current = current.getChildren().get(ch);
        }
        return current;
    }

    public List<Suggestion> collectSuggestion(TrieNode startNode, String prefix) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (startNode != null) {
            collect(startNode, prefix, suggestions);

            // Sort by weight in descending order
            suggestions.sort((a, b) -> b.getWeight() - a.getWeight());
        }
        return suggestions;
    }
}

class AutoCompleteSystem {
    private Trie trie;
    private int maxSuggestions;

    public AutoCompleteSystem() {
        this.trie = new Trie();
        this.maxSuggestions = 10; // default value
    }

    public AutoCompleteSystem(int maxSuggestions) {
        this.trie = new Trie();
        this.maxSuggestions = maxSuggestions;
    }

    private String toLowercase(String str) {
        return str.toLowerCase();
    }

    public void addWord(String word) {
        trie.insert(word);
    }

    public List<String> getSuggestions(String prefix) {
        TrieNode searchNode = trie.searchPrefix(prefix);

        if (searchNode == null) {
            return new ArrayList<>();
        }

        List<Suggestion> suggestions = trie.collectSuggestion(searchNode, prefix);
        List<String> result = new ArrayList<>();

        // Get top N suggestions
        int count = Math.min(suggestions.size(), maxSuggestions);
        for (int i = 0; i < count; i++) {
            result.add(suggestions.get(i).getWord());
        }

        return result;
    }
}

class Main {
    public static void main(String[] args) {
        AutoCompleteSystem system = new AutoCompleteSystem();

        system.addWord("code");
        system.addWord("coding");
        system.addWord("codigninja");
        system.addWord("codly");
        system.addWord("codlldy");

        List<String> result = system.getSuggestions("codl");

        System.out.println("Suggestions for 'codl':");
        for (String str : result) {
            System.out.println(str);
        }
    }
}