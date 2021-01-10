package my.IRInterface;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import static java.lang.Integer.min;

public class search {
    static int MAX_RESULTS = 10;
    // max amount of results that will be printed
    static ArrayList<File> files = new ArrayList<>();
    // list of indexed files
    static ArrayList<String> accepted_types = new ArrayList<>(Arrays.asList(".txt", ".html"));
    // types to be index
    static String[] fields = {"content", "title", "date"};
    private static CustomAnalyzer analyzer;
    static Directory index = new RAMDirectory();

    public static void listFilesForFolder(File folder) {
        // walks through the given folder and saves all files with accepted types
        File[] f_files = folder.listFiles();
        if (f_files == null)
            return;

        for (final File fileEntry : f_files) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                String tmp = fileEntry.getName();
                if (accepted_types.contains(tmp.substring(tmp.lastIndexOf("."))))
                    files.add(fileEntry);
            }
        }
    }

    public static void addDoc(IndexWriter w, File file) throws IOException, DataFormatException {
        // indexes a file if it has an accepted type
        Document doc = new Document();
        String content = Files.lines(Paths.get(file.getPath()))
                .collect(Collectors.joining(System.lineSeparator()));
        String lastMod = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(file.lastModified());
        doc.add(new StringField("name", file.getName(), Field.Store.YES));
        doc.add(new StringField("path", file.getPath(), Field.Store.YES));
        doc.add(new StringField("last modified", lastMod, Field.Store.YES));

        switch (file.getName().substring(file.getName().lastIndexOf("."))) {
            case (".txt"): {
                doc.add(new TextField("content", content, Field.Store.YES));
                break;
            }
            case (".html"): {
                org.jsoup.nodes.Document html = Jsoup.parse(content);
                String body = html.select("body").text();
                String title = html.select("title").text();
                String summary = html.select("summary").text();
                doc.add(new TextField("title", title, Field.Store.YES));
                doc.add(new TextField("date", lastMod.substring(0, 10), Field.Store.YES));
                doc.add(new StringField("summary", summary, Field.Store.YES));
                doc.add(new TextField("content", body, Field.Store.YES));
                break;
            }
            default: {
                throw new DataFormatException("Invalid Data Type added to Index");
            }
        }
        w.addDocument(doc);
    }

    public static void init(String path) throws IOException, DataFormatException {
        System.out.println("Indexing...");
        File file = new File(path);
        listFilesForFolder(file);

        // index files & process query
        analyzer = CustomAnalyzer.builder()
                .withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class)
                .addTokenFilter(PorterStemFilterFactory.class)
                .build();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new ClassicSimilarity());
        IndexWriter writer = new IndexWriter(index, config);

        for (File value : files) {
            addDoc(writer, value);
        }
        writer.close();
        System.out.println("Done");
    }

    public static class Hit{
        public int rank;
        public float score;
        public Document doc;

        public Hit(int rank,float score,Document doc){
            this.rank = rank;
            this.score = score;
            this.doc = doc;
        }
    }

    public static ArrayList<Hit> search(String querystr) throws ParseException, IOException {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        Query q = parser.parse(querystr);

        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new ClassicSimilarity());
        TopScoreDocCollector collector = TopScoreDocCollector.create(MAX_RESULTS);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        ArrayList<Hit> results = new ArrayList<Hit>();
        for (int i = 0; i < hits.length; i++) {
            results.add(new Hit(i + 1, hits[i].score, searcher.doc(hits[i].doc)));
        }
        reader.close();
        return results;
    }



    public static void main(String[] args) throws IOException, ParseException, DataFormatException {
        System.out.println("-------------------------------");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));

        // read the path & list all files in the given path
        String path = "";
        while (true) {
            if (args.length < 1) {
                System.out.println("Enter path: ");
                path = br.readLine();
            } else {
                path = args[0];
            }
            File file = new File(path);

            listFilesForFolder(file);

            if (files.size() != 0)
                break;

            System.out.println("No files index, please enter another path");
        }

        // read query
        System.out.println("Enter Query: ");
        String querystr = br.readLine();

        // index files & process query
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class)
                .addTokenFilter(PorterStemFilterFactory.class)
                .build();

        Directory index = new RAMDirectory();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new ClassicSimilarity());
        IndexWriter writer = new IndexWriter(index, config);

        for (File value : files) {
            addDoc(writer, value);
        }
        writer.close();

        String[] fields = {"content", "title", "date"};

        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        Query q = parser.parse(querystr);

        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new ClassicSimilarity());
        TopScoreDocCollector collector = TopScoreDocCollector.create(MAX_RESULTS);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        // output result
        System.out.println("-------------------------------");
        System.out.println("Query: " + querystr);
        System.out.println("Found " + hits.length + " hits.");
        System.out.println("-------------------------------");

        System.out.format("%5s\t%8s\t%20s\t%20s\t%20s\t%40s\t%s\n",
                "Rank", "Score", "Name", "Title", "Last Modified", "Summary", "Path");
        for (int i = 0; i < hits.length; i++) {
            int docID = hits[i].doc;
            float score = hits[i].score;
            Document d = searcher.doc(docID);
            System.out.format(
                    "%5d\t%8f\t%20s\t%20s\t%20s\t%40s\t%s\n",
                    i + 1,
                    score,
                    d.get("name"),
                    (d.get("title") == null) ? "/" : d.get("title"),
                    d.get("last modified"),
                    (d.get("summary") == null) ? "/" : d.get("summary").substring(0, min(d.get("summary").length(), 40)),
                    d.get("path"));
        }
        reader.close();
        index.close();
    }
}
