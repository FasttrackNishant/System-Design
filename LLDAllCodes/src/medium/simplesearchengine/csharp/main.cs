class Document
{
    private readonly string id;
    private readonly string title;
    private readonly string content;

    public Document(string id, string title, string content)
    {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public string GetId() { return id; }
    public string GetTitle() { return title; }
    public string GetContent() { return content; }

    public override string ToString()
    {
        return "Document(id=" + id + ", title='" + title + "')";
    }
}






class InvertedIndex
{
    private readonly Dictionary<string, List<Posting>> index = new Dictionary<string, List<Posting>>();

    public void Add(string term, string documentId, int frequency)
    {
        if (!index.ContainsKey(term))
        {
            index[term] = new List<Posting>();
        }
        index[term].Add(new Posting(documentId, frequency));
    }

    public List<Posting> GetPostings(string term)
    {
        return index.ContainsKey(term) ? index[term] : new List<Posting>();
    }
}




class Posting
{
    private readonly string documentId;
    private readonly int frequency;

    public Posting(string documentId, int frequency)
    {
        this.documentId = documentId;
        this.frequency = frequency;
    }

    public string GetDocumentId() { return documentId; }
    public int GetFrequency() { return frequency; }
}




class SearchResult
{
    private readonly Document document;
    private readonly double score;

    public SearchResult(Document document, double score)
    {
        this.document = document;
        this.score = score;
    }

    public Document GetDocument() { return document; }
    public double GetScore() { return score; }

    public override string ToString()
    {
        return "  - " + document.GetTitle() + " (Score: " + score.ToString("F2") + ")";
    }
}














interface RankingStrategy
{
    void Rank(List<SearchResult> results);
}




class ScoreBasedRankingStrategy : RankingStrategy
{
    public void Rank(List<SearchResult> results)
    {
        results.Sort((a, b) => b.GetScore().CompareTo(a.GetScore()));
    }
}




class ScoreThenAlphabeticalRankingStrategy : RankingStrategy
{
    public void Rank(List<SearchResult> results)
    {
        results.Sort((a, b) =>
        {
            int scoreComparison = b.GetScore().CompareTo(a.GetScore());
            if (scoreComparison != 0)
            {
                return scoreComparison;
            }
            return a.GetDocument().GetTitle().CompareTo(b.GetDocument().GetTitle());
        });
    }
}









interface ScoringStrategy
{
    double CalculateScore(string term, Posting posting, Document document);
}


class TermFrequencyScoringStrategy : ScoringStrategy
{
    public double CalculateScore(string term, Posting posting, Document document)
    {
        return posting.GetFrequency();
    }
}




class TitleBoostScoringStrategy : ScoringStrategy
{
    private static readonly double TITLE_BOOST_FACTOR = 1.5;

    public double CalculateScore(string term, Posting posting, Document document)
    {
        double score = posting.GetFrequency();
        if (document.GetTitle().ToLower().Contains(term))
        {
            score *= TITLE_BOOST_FACTOR;
        }
        return score;
    }
}








class DocumentStore
{
    private readonly Dictionary<string, Document> store = new Dictionary<string, Document>();

    public void AddDocument(Document doc)
    {
        store[doc.GetId()] = doc;
    }

    public Document GetDocument(string docId)
    {
        store.TryGetValue(docId, out Document doc);
        return doc;
    }
}






class SearchEngine
{
    private static SearchEngine instance;
    private readonly InvertedIndex invertedIndex;
    private readonly DocumentStore documentStore;
    private ScoringStrategy scoringStrategy;
    private RankingStrategy rankingStrategy;

    private SearchEngine()
    {
        this.invertedIndex = new InvertedIndex();
        this.documentStore = new DocumentStore();
    }

    public static SearchEngine GetInstance()
    {
        if (instance == null)
        {
            instance = new SearchEngine();
        }
        return instance;
    }

    public void SetScoringStrategy(ScoringStrategy scoringStrategy)
    {
        this.scoringStrategy = scoringStrategy;
    }

    public void SetRankingStrategy(RankingStrategy rankingStrategy)
    {
        this.rankingStrategy = rankingStrategy;
    }

    public void IndexDocuments(List<Document> documents)
    {
        foreach (Document doc in documents)
        {
            IndexDocument(doc);
        }
    }

    public void IndexDocument(Document doc)
    {
        documentStore.AddDocument(doc);
        Dictionary<string, int> termFrequencies = new Dictionary<string, int>();

        string text = (doc.GetTitle() + " " + doc.GetContent()).ToLower();
        string[] tokens = Regex.Split(text, @"\W+");

        foreach (string token in tokens)
        {
            if (!string.IsNullOrEmpty(token))
            {
                if (termFrequencies.ContainsKey(token))
                {
                    termFrequencies[token]++;
                }
                else
                {
                    termFrequencies[token] = 1;
                }
            }
        }

        foreach (KeyValuePair<string, int> entry in termFrequencies)
        {
            invertedIndex.Add(entry.Key, doc.GetId(), entry.Value);
        }
    }

    public List<SearchResult> Search(string query)
    {
        string processedQuery = query.ToLower();

        List<Posting> postings = invertedIndex.GetPostings(processedQuery);

        List<SearchResult> results = new List<SearchResult>();
        foreach (Posting posting in postings)
        {
            Document doc = documentStore.GetDocument(posting.GetDocumentId());
            if (doc != null)
            {
                double score = scoringStrategy.CalculateScore(processedQuery, posting, doc);
                results.Add(new SearchResult(doc, score));
            }
        }

        rankingStrategy.Rank(results);

        return results;
    }
}







using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;

public class SearchEngineDemo
{
    public static void Main()
    {
        SearchEngine engine = SearchEngine.GetInstance();

        List<Document> documents = new List<Document>
        {
            new Document("doc1", "Java Performance", "Java is a high-performance language. Tuning Java applications is key."),
            new Document("doc2", "Introduction to Python", "Python is a versatile language, great for beginners."),
            new Document("doc3", "Advanced Java Concepts", "This document covers advanced topics in Java programming."),
            new Document("doc4", "Python vs. Java", "A document comparing Python and Java for web development. Java is faster.")
        };

        Console.WriteLine("Indexing documents...");
        engine.IndexDocuments(documents);
        Console.WriteLine("Indexing complete.\n");

        Console.WriteLine("====== TermFrequency Scoring + ScoreBased Ranking ======");
        engine.SetScoringStrategy(new TermFrequencyScoringStrategy());
        engine.SetRankingStrategy(new ScoreBasedRankingStrategy());

        PerformSearch(engine, "java");
        PerformSearch(engine, "language");
        PerformSearch(engine, "performance");

        Console.WriteLine("\n====== TitleBoost Scoring + Score-then-Alphabetical Ranking ======");
        engine.SetScoringStrategy(new TitleBoostScoringStrategy());
        engine.SetRankingStrategy(new ScoreThenAlphabeticalRankingStrategy());

        PerformSearch(engine, "java");
        PerformSearch(engine, "language");
        PerformSearch(engine, "performance");

        PerformSearch(engine, "paint");
    }

    private static void PerformSearch(SearchEngine engine, string query)
    {
        Console.WriteLine("--- Searching for: '" + query + "' ---");
        List<SearchResult> results = engine.Search(query);

        if (results.Count == 0)
        {
            Console.WriteLine("  No results found.");
        }
        else
        {
            for (int i = 0; i < results.Count; i++)
            {
                Console.WriteLine("Rank " + (i + 1) + ":" + results[i]);
            }
        }
        Console.WriteLine();
    }
}
















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































