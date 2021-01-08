import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

import static java.lang.Integer.min;

public class Interface {
    private JButton searchButton;
    private JTextField textField1;
    private JProgressBar progressBar1;
    private JTable table1;
    private JPanel MainView;

    public Interface() {
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
            } catch (ParseException exception) {
                exception.printStackTrace();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException, DataFormatException {
        JFrame frame = new JFrame("Calculator");
        frame.setContentPane(new Interface().MainView);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        pa06.init("D:\\InfoRet\\dump");
    }
}
