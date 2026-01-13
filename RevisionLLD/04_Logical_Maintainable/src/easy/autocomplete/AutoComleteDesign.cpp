#include <iostream>
#include <map>
#include <bits/stdc++.h>
using namespace std;

class TrieNode
{

private:
    map<char, TrieNode *> children;
    bool isTerminal;
    int frequency;

public:
    TrieNode()
    {
        this->isTerminal = false;
        this->frequency = 0;
    }

    map<char, TrieNode *>& getChildren()
    {
        return this->children;
    }

    void setTerminal(bool flag)
    {
        this->isTerminal = flag;
    }

    bool getTerminal()
    {
        return this->isTerminal;
    }

    void increaseFreq()
    {
        this->frequency++;
    }

    int getFrequency() const
    {
        return frequency;
    }
};


class Suggestion
{

private:
    string word;
    int weight;

public:
    Suggestion(string word, int weight)
    {
        this->word = word;
        this->weight = weight;
    }

    string getWord()
    {
        return this->word;
    }

    int getWeight()
    {
        return this->weight;
    }
};

class Trie
{
private:
    TrieNode *root;

    void collect(TrieNode *node, string prefix, vector<Suggestion> &output)
    {

        if (node->getTerminal())
        {
            output.push_back(Suggestion(prefix, node->getFrequency()));
        }

        for(auto child : node->getChildren()){
            collect(child.second, prefix + child.first, output);
        }

    }

public:
    Trie()
    {
        root = new TrieNode();
    }

    void insert(const string &word)
    {
        TrieNode *curr = root;

        for (char ch : word)
        {

            if (curr->getChildren().find(ch) == curr->getChildren().end())
            {
                curr->getChildren()[ch] = new TrieNode();
            }

            curr = curr->getChildren()[ch];
        }

        curr->setTerminal(true);
        curr->increaseFreq();
    }

    TrieNode *searchPrefix(string prefix)
    {

        TrieNode *current = root;

        for (char ch : prefix)
        {

            auto it = current->getChildren().find(ch);

            if (it == current->getChildren().end())
            {
                // present nahi hain
                return nullptr;
            }

            current = it->second;
        }
        return current;
    }

    vector<Suggestion> collectSuggestion(TrieNode *startNode, string prefix)
    {
        vector<Suggestion> suggestions;
        collect(startNode, prefix, suggestions);
        return suggestions;
    }
};


class AutoCompleteSystem
{

private:
    Trie trie;
    int maxSuggesions;

    string toLowercase(string &str)
    {
        string result = str;
        transform(result.begin(), result.end(), result.begin(), ::tolower);
        return result;
    }

public:

    AutoCompleteSystem() {

    }

    void addWord(string word)
    {
        trie.insert(word);
    }

    vector<string> getSuggesions(string prefix)
    {

        // first find that node
        TrieNode * searchNode = trie.searchPrefix(prefix);

        if(searchNode == nullptr){
            return {};
        }

        vector<Suggestion> suggestions = trie.collectSuggestion(searchNode, prefix);

        vector<string> result;

        for(auto it : suggestions){
            result.push_back(it.getWord());
        }

        return result;
    }
};

int main()
{

    AutoCompleteSystem *  system = new AutoCompleteSystem();

    system->addWord("code");
    system->addWord("coding");
    system->addWord("codigninja");
    system->addWord("codly");
    system->addWord("codlldy");

    vector<string> result ;

    result = system->getSuggesions("codl");

    for(auto str : result){
        cout << str << endl;
    }

    return 0;
}