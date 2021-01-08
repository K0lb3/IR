import org.apache.lucene.document.Document;

import static java.lang.Integer.min;

public class result {
    public int rank;
    public float score;
    public String name;
    public String title;
    public String last_modified;
    public String summary;
    public String path;

}
/*
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
*/