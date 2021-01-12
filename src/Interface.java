import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;


public class Interface {
    private JButton searchButton;
    private JTextField textField1;
    private JProgressBar progressBar1;
    private JPanel MainView;
    private JScrollPane scrollPane;
    private JTextField textField2;
    private JButton createIndexButton;
    private DefaultTableModel tModel;
    private JTable table1;

    public Interface() {
        //table1.revalidate();
        searchButton.addActionListener(new SearchBtnClicked());
        createIndexButton.addActionListener(new CreateIndexButtonClicked());
    }

    private void createUIComponents() {
        String[] cols = {"Image","Rank", "Score", "Name", "Last Modified", "Preview"};
        tModel = new DefaultTableModel(cols, 0);

        table1 = new JTable(tModel){
            public Class getColumnClass(int column) {
                return (column == 0) ? Icon.class : Object.class;
            }
        };
        //table1.getColumn("Image").setMaxWidth(50);
        table1.getColumn("Rank").setMaxWidth(50);
        table1.getColumn("Score").setMaxWidth(50);
        table1.setRowHeight(50);
    }

    private class CreateIndexButtonClicked implements ActionListener{
        public CreateIndexButtonClicked() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //System.out.println("Called IndexButton");
            try {
                pa07.createIndex(textField2.getText());
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
                ArrayList<pa07.Hit> hits = pa07.search(query);

                for (int i=0; i < hits.size(); i++){
                    pa07.Hit hit = hits.get(i);
                }
                displaySearchResults(hits);

            } catch (ParseException | IOException exception) {
                exception.printStackTrace();
            }
        }

        private void displaySearchResults(ArrayList<pa07.Hit> hits) throws IOException {
            tModel.setRowCount(0);

            for (int i = 0; i < hits.size(); i++){
                pa07.Hit hit = hits.get(i);

                Object[] row = {
                        null,
                        Integer.toString(hit.rank),
                        Float.toString(hit.score),
                        hit.doc.get("name"),
                        hit.doc.get("date"),
                        hit.doc.get("content")
                };
                String imgPath = hit.getDocImages();
                ImageIcon icon;
                if (imgPath != null){
                    icon = new ImageIcon(hit.getDocImages());
                    Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(img);
                }else{
                    icon = new ImageIcon("dataSrc/OvGU-Logo.jpg");
                    Image img = icon.getImage();
                    double scaleFactor = 0.05;
                    img = img.getScaledInstance(
                            (int)(img.getWidth(null)*scaleFactor),
                            (int)(img.getHeight(null)*scaleFactor),
                            Image.SCALE_SMOOTH);
                    icon = new ImageIcon(img);
                }
                row[0] = icon;

                tModel.addRow(row);
            }
        }
    }


    public static void main(String[] args) throws IOException, DataFormatException {
        JFrame frame = new JFrame("Search Application");
        frame.setSize(500,500);
        frame.setContentPane(new Interface().MainView);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
