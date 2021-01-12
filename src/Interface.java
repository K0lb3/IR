import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.DataFormatException;


public class Interface {
    private JButton searchButton;
    private JTextField textField1;
    private JProgressBar progressBar1;
    private JPanel MainView;
    private JScrollPane scrollPane;

    public Interface() {
        //table1.revalidate();
        searchButton.addActionListener(new SearchBtnClicked());
    }

    private class SearchBtnClicked implements ActionListener {
        public SearchBtnClicked() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String query = textField1.getText();
                ArrayList<pa06.Hit> hits = pa06.search(query);
                System.out.println(hits.size());
                Document document = hits.get(0).doc;
                System.out.println(document.get("name"));

                String[][] data = new String[hits.size()][3];
                int i = 0;
                for(pa06.Hit hit: hits){
                    data[i][0] = Integer.toString(hit.rank);
                    data[i][1] = Float.toString(hit.score);
                    data[i][2] = hit.doc.get("name");
                }
                //table1.repaint();
            } catch (ParseException exception) {
                exception.printStackTrace();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public ArrayList<File> getDocImages(Document d){
        /*
            Lists all images of a specific doc
            by looking for images in the directory of the document
         */
        File dir = Paths.get(d.get("path")).getParent().toFile();
        ArrayList<File> images = new ArrayList<File>();
        for (File f : dir.listFiles()) {
            String filename = f.getName().toLowerCase();
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                    filename.endsWith(".gif") || filename.endsWith(".png")) {
                images.add(f);
            }
        }
        return images;
    }

    public static void main(String[] args) throws IOException, DataFormatException {
        JFrame frame = new JFrame("Calculator");
        frame.setContentPane(new Interface().MainView);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        pa06.init("C:/Users/denni/Desktop/uni/IR/dump");
    }
}
