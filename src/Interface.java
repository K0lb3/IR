import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;


public class Interface {
    private JButton searchButton;
    private JTextField textField1;
    private JProgressBar progressBar1;
    private JPanel MainView;
    private JTextField textField2;
    private JButton createIndexButton;
    private DefaultTableModel tModel;
    private JTable table1;
    private JTextArea textArea1;
    protected ArrayList<pa07.Hit> hits;

    public Interface() {
        searchButton.addActionListener(new SearchBtnClicked());
        createIndexButton.addActionListener(new CreateIndexButtonClicked());
        table1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jList1MouseReleased();
            }
        });
        progressBar1.setStringPainted(true);
        textArea1.setLineWrap(true);
        textArea1.setHighlighter(null);
    }

    private void createUIComponents() {
        String[] cols = {"Image", "Rank", "Score", "Name", "Last Modified"};
        tModel = new DefaultTableModel(cols, 0);

        table1 = new JTable(tModel) {
            public Class getColumnClass(int column) {
                return (column == 0) ? Icon.class : Object.class;
            }
        };
        //table1.getColumn("Image").setMaxWidth(50);
        table1.getColumn("Rank").setMaxWidth(50);
        table1.getColumn("Score").setMaxWidth(50);
        table1.setRowHeight(50);
    }

    private void jList1MouseReleased() {
        int i = table1.getSelectedRow();
        if (i == -1) return;
        pa07.Hit hit = hits.get(i);
        textArea1.setText(hit.doc.get("content"));
    }

    private class CreateIndexButtonClicked implements ActionListener {
        public CreateIndexButtonClicked() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //System.out.println("Called IndexButton");
            try {
                pa07.createIndex(textField2.getText(), progressBar1);
            } catch (DataFormatException | IOException dataFormatException) {
                dataFormatException.printStackTrace();
            }
        }
    }

    private class SearchBtnClicked implements ActionListener {
        public SearchBtnClicked() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //System.out.println("Called SearchButton");
            try {
                String query = textField1.getText();
                hits = pa07.search(query, progressBar1);
                displaySearchResults(hits);

            } catch (ParseException | IOException | InvalidTokenOffsetsException exception) {
                exception.printStackTrace();
            }
        }

        private ImageIcon scaleIcon(ImageIcon icon){
            int ih = icon.getIconHeight();
            int iw = icon.getIconWidth();
            if (ih > iw){
                iw = iw*50/ih;
                ih = 50;
            }
            else{
                ih = ih*50/iw;
                iw = 50;
            }
            Image img = icon.getImage().getScaledInstance(iw, ih, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        }

        private void displaySearchResults(ArrayList<pa07.Hit> hits) {
            tModel.setRowCount(0);

            for (pa07.Hit hit : hits) {
                /*
                ADD SNIPPET HERE INSTEAD OF CONTENT IN OBJECT ARRAY ROW
                 */
                Object[] row = {
                        null,
                        Integer.toString(hit.rank),
                        Float.toString(hit.score),
                        hit.doc.get("name"),
                        hit.doc.get("date"),
                };
                ImageIcon icon = new ImageIcon(hit.getDocImages());
                row[0] = scaleIcon(icon);
                tModel.addRow(row);
            }
        }
    }




    public static void main(String[] args) {
        JFrame frame = new JFrame("Search Application");
        frame.setSize(800, 450);
        frame.setContentPane(new Interface().MainView);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
