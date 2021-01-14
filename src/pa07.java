import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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
import org.apache.lucene.queryparser.classic.FastCharStream;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

public class pa07 {
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
        doc.add(new TextField("date", lastMod.substring(0, 10), Field.Store.YES));

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

    public static void createIndex(String path, JProgressBar bar) throws IOException, DataFormatException {
        System.out.println("Indexing...");
        bar.setString("looking for files...");
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

        // prepare progress bar
        BoundedRangeModel model = bar.getModel();
        int c = 0;
        int max = files.size();
        model.setMaximum(max);
        bar.setString(String.format("%d / %d files indexed", c, max));
        // index files
        for (File value : files) {
            addDoc(writer, value);
            model.setValue(++c);
            bar.setString(String.format("%d / %d files indexed", c, max));
        }
        writer.close();
        System.out.println("Done");
    }

    public static class Hit {
        public int rank;
        public float score;
        public Document doc;
        public TextFragment[] frags;

        public Hit(int rank, float score, Document doc, TextFragment[] frags) {
            this.rank = rank;
            this.score = score;
            this.doc = doc;
            this.frags = frags;
        }

        @Override
        public String toString() {
            String str = "";

            str += rank + "; ";
            str += score + "; ";
            str += doc.get("name") + "; ";
            str += doc.get("content");

            return str;
        }

        public ArrayList<String> getHitData() {
            ArrayList<String> ret = new ArrayList<>();

            ret.add(Integer.toString(this.rank));
            ret.add(Float.toString(this.score));
            ret.add(this.doc.get("name"));
            ret.add(this.doc.get("last modified"));
            ret.add(this.doc.get("content"));

            return ret;
        }

        public String getDocImages() {
        /*
            Lists all images of a specific doc
            by looking for images in the directory of the document
         */
            File dir = Paths.get(this.doc.get("path")).getParent().toFile();
            String imagePath = "dataSrc/OvGU-Logo.jpg";
            for (File f : dir.listFiles()) {
                String filename = f.getName().toLowerCase();
                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                        filename.endsWith(".gif") || filename.endsWith(".png")) {
                    imagePath = f.getPath();
                    break;
                }
            }
            return imagePath;
        }
    }


    public static ArrayList<Hit> search(String querystr, JProgressBar bar) throws ParseException, IOException, InvalidTokenOffsetsException {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        Query q = parser.parse(querystr);

        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new ClassicSimilarity());
        TopScoreDocCollector collector = TopScoreDocCollector.create(MAX_RESULTS);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        /** Highlighter Code Start ****/
        // HighLighter Source: https://howtodoinjava.com/lucene/lucene-search-highlight-example/#search-highlight
        Formatter formatter = new SimpleHTMLFormatter("","");
        QueryScorer scorer = new QueryScorer(q);
        Highlighter highlighter = new Highlighter(formatter, scorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 10);
        highlighter.setTextFragmenter(fragmenter);

        ArrayList<Hit> results = new ArrayList<>();
        for (int i = 0; i < hits.length; i++) {
            ScoreDoc hit = hits[i];
            int id = hit.doc;
            Document doc = searcher.doc(id);
            String fieldName = "content";
            String text = doc.get(fieldName);
            TokenStream tokenStream = TokenSources.getTokenStream(fieldName,
                    searcher.getIndexReader().getTermVectors(id), text, analyzer, -1);
            TextFragment[] frags = highlighter.getBestTextFragments(tokenStream,
                    text, true, 10);
            results.add(new Hit(i + 1, hit.score, doc, frags));
        }
        reader.close();
        return results;
    }
}
