
class Document {
private:
    string id;
    string title;
    string content;

public:
    Document(const string& id, const string& title, const string& content) 
        : id(id), title(title), content(content) {}

    string getId() const { return id; }
    string getTitle() const { return title; }
    string getContent() const { return content; }

    string toString() const {
        return "Document(id=" + id + ", title='" + title + "')";
    }
};



class InvertedIndex {
private:
    map<string, vector<Posting*>> index;

public:
    ~InvertedIndex() {
        for (auto& pair : index) {
            for (Posting* posting : pair.second) {
                delete posting;
            }
        }
    }

    void add(const string& term, const string& documentId, int frequency) {
        if (index.find(term) == index.end()) {
            index[term] = vector<Posting*>();
        }
        index[term].push_back(new Posting(documentId, frequency));
    }

    vector<Posting*> getPostings(const string& term) {
        auto it = index.find(term);
        return (it != index.end()) ? it->second : vector<Posting*>();
    }
};





class Posting {
private:
    string documentId;
    int frequency;

public:
    Posting(const string& documentId, int frequency) 
        : documentId(documentId), frequency(frequency) {}

    string getDocumentId() const { return documentId; }
    int getFrequency() const { return frequency; }
};





class SearchResult {
private:
    Document* document;
    double score;

public:
    SearchResult(Document* document, double score) : document(document), score(score) {}

    Document* getDocument() const { return document; }
    double getScore() const { return score; }

    string toString() const {
        char scoreStr[20];
        sprintf(scoreStr, "%.2f", score);
        return "  - " + document->getTitle() + " (Score: " + string(scoreStr) + ")";
    }
};





class RankingStrategy {
public:
    virtual ~RankingStrategy() {}
    virtual void rank(vector<SearchResult*>& results) = 0;
};



class ScoreBasedRankingStrategy : public RankingStrategy {
public:
    void rank(vector<SearchResult*>& results) {
        sort(results.begin(), results.end(), [](SearchResult* a, SearchResult* b) {
            return a->getScore() > b->getScore();
        });
    }
};


class ScoreThenAlphabeticalRankingStrategy : public RankingStrategy {
public:
    void rank(vector<SearchResult*>& results) {
        sort(results.begin(), results.end(), [](SearchResult* a, SearchResult* b) {
            if (a->getScore() != b->getScore()) {
                return a->getScore() > b->getScore();
            }
            return a->getDocument()->getTitle() < b->getDocument()->getTitle();
        });
    }
};






class ScoringStrategy {
public:
    virtual ~ScoringStrategy() {}
    virtual double calculateScore(const string& term, Posting* posting, Document* document) = 0;
};


class TermFrequencyScoringStrategy : public ScoringStrategy {
public:
    double calculateScore(const string& term, Posting* posting, Document* document) {
        return posting->getFrequency();
    }
};


class TitleBoostScoringStrategy : public ScoringStrategy {
private:
    static const double TITLE_BOOST_FACTOR;

public:
    double calculateScore(const string& term, Posting* posting, Document* document) {
        double score = posting->getFrequency();
        string title = document->getTitle();
        transform(title.begin(), title.end(), title.begin(), ::tolower);
        if (title.find(term) != string::npos) {
            score *= TITLE_BOOST_FACTOR;
        }
        return score;
    }
};

const double TitleBoostScoringStrategy::TITLE_BOOST_FACTOR = 2.0;








class DocumentStore {
private:
    map<string, Document*> store;

public:
    ~DocumentStore() {
        for (auto& pair : store) {
            delete pair.second;
        }
    }

    void addDocument(Document* doc) {
        store[doc->getId()] = doc;
    }

    Document* getDocument(const string& docId) {
        auto it = store.find(docId);
        return (it != store.end()) ? it->second : NULL;
    }
};





string toLowerCase(const string& str) {
    string result = str;
    transform(result.begin(), result.end(), result.begin(), ::tolower);
    return result;
}

vector<string> tokenize(const string& text) {
    vector<string> tokens;
    string token;
    for (char c : text) {
        if (isalnum(c)) {
            token += c;
        } else {
            if (!token.empty()) {
                tokens.push_back(token);
                token.clear();
            }
        }
    }
    if (!token.empty()) {
        tokens.push_back(token);
    }
    return tokens;
}





class SearchEngine {
private:
    static SearchEngine* instance;
    InvertedIndex* invertedIndex;
    DocumentStore* documentStore;
    ScoringStrategy* scoringStrategy;
    RankingStrategy* rankingStrategy;

    SearchEngine() {
        invertedIndex = new InvertedIndex();
        documentStore = new DocumentStore();
        scoringStrategy = NULL;
        rankingStrategy = NULL;
    }

public:
    ~SearchEngine() {
        delete invertedIndex;
        delete documentStore;
    }

    static SearchEngine* getInstance() {
        if (instance == NULL) {
            instance = new SearchEngine();
        }
        return instance;
    }

    void setScoringStrategy(ScoringStrategy* scoringStrategy) {
        this->scoringStrategy = scoringStrategy;
    }

    void setRankingStrategy(RankingStrategy* rankingStrategy) {
        this->rankingStrategy = rankingStrategy;
    }

    void indexDocuments(const vector<Document*>& documents) {
        for (Document* doc : documents) {
            indexDocument(doc);
        }
    }

    void indexDocument(Document* doc) {
        documentStore->addDocument(doc);
        map<string, int> termFrequencies;

        string text = toLowerCase(doc->getTitle() + " " + doc->getContent());
        vector<string> tokens = tokenize(text);

        for (const string& token : tokens) {
            if (!token.empty()) {
                termFrequencies[token]++;
            }
        }

        for (const auto& entry : termFrequencies) {
            invertedIndex->add(entry.first, doc->getId(), entry.second);
        }
    }

    vector<SearchResult*> search(const string& query) {
        string processedQuery = toLowerCase(query);

        vector<Posting*> postings = invertedIndex->getPostings(processedQuery);

        vector<SearchResult*> results;
        for (Posting* posting : postings) {
            Document* doc = documentStore->getDocument(posting->getDocumentId());
            if (doc != NULL) {
                double score = scoringStrategy->calculateScore(processedQuery, posting, doc);
                results.push_back(new SearchResult(doc, score));
            }
        }

        rankingStrategy->rank(results);

        return results;
    }
};

// Static member initialization
SearchEngine* SearchEngine::instance = NULL;










class SearchEngineDemo {
public:
    static void main() {
        SearchEngine* engine = SearchEngine::getInstance();

        vector<Document*> documents = {
            new Document("doc1", "Java Performance", "Java is a high-performance language. Tuning Java applications is key."),
            new Document("doc2", "Introduction to Python", "Python is a versatile language, great for beginners."),
            new Document("doc3", "Advanced Java Concepts", "This document covers advanced topics in Java programming."),
            new Document("doc4", "Python vs. Java", "A document comparing Python and Java for web development. Java is faster.")
        };

        cout << "Indexing documents..." << endl;
        engine->indexDocuments(documents);
        cout << "Indexing complete.\n" << endl;

        cout << "====== TermFrequency Scoring + ScoreBased Ranking ======" << endl;
        engine->setScoringStrategy(new TermFrequencyScoringStrategy());
        engine->setRankingStrategy(new ScoreBasedRankingStrategy());

        performSearch(engine, "java");
        performSearch(engine, "language");
        performSearch(engine, "performance");

        cout << "\n====== TitleBoost Scoring + Score-then-Alphabetical Ranking ======" << endl;
        engine->setScoringStrategy(new TitleBoostScoringStrategy());
        engine->setRankingStrategy(new ScoreThenAlphabeticalRankingStrategy());

        performSearch(engine, "java");
        performSearch(engine, "language");
        performSearch(engine, "performance");

        performSearch(engine, "paint");
    }

private:
    static void performSearch(SearchEngine* engine, const string& query) {
        cout << "--- Searching for: '" << query << "' ---" << endl;
        vector<SearchResult*> results = engine->search(query);

        if (results.empty()) {
            cout << "  No results found." << endl;
        } else {
            for (size_t i = 0; i < results.size(); i++) {
                cout << "Rank " << (i + 1) << ":" << results[i]->toString() << endl;
            }
        }
        cout << endl;

        // Clean up results
        for (SearchResult* result : results) {
            delete result;
        }
    }
};

int main() {
    SearchEngineDemo::main();
    return 0;
}

























































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































