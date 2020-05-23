/**
 * TextEditor.java
 *
 * A Text Editor written in Java that can be used to open, edit, and save any text-based file in a user's directory.
 * Features a search toolbar that finds every instance of a given input, with the ability to search using regular expressions.
 * Provides seamless forward and backward traversal of the matches found.
 *
 * @author: Saul Melo
 * Spring 2020
 */

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEditor extends JFrame {
    private File selectedFile; // Selected file accessed by open & save buttons
    private Matcher matcher; // Holds the current matcher object that powers the search features
    private final List<Integer> matchStartIndexes = new ArrayList<>(); // Tracks the start indexes of the matches traversed by nextMatch in order for previousMatch to work
    private boolean regexSelected; // Regex search can be toggled

    public TextEditor() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Text Editor");
        setSize(1000, 1000);
        setLocationRelativeTo(null); // Places window in center of screen

        // * TEXT AREA - Where the text of the selected file goes, takes up majority of the window
        JTextArea textArea = new JTextArea();
        JScrollPane spTextArea = new JScrollPane(textArea);
        textArea.setName("TextArea");
        spTextArea.setName("ScrollPane");
        setMargin(spTextArea, 100, 50, 50, 50);

        // * MAIN PANEL - Holds the open and save buttons, search bar with a GridBag layout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBounds(550, 35, 400, 25);
        GridBagConstraints c = new GridBagConstraints(); // Components are all set on one horizontal plane
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.weightx = 0.1; // Load and save buttons have weightx of 0.1

        add(mainPanel); // mainPanel has to be added first else textArea won't appear
        add(spTextArea);

        // * OPEN & SAVE BUTTONS - Any text-based file in the present file system can be opened, edited, and saved
        // OPEN BUTTON - Writes the contents of the selected file onto the textArea
        JButton openButton = new JButton("");
        openButton.setName("OpenButton");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setName("FileChooser");
        fileChooser.setVisible(false); // Only make fileChooser visible when opening or saving a file
        add(fileChooser, BorderLayout.PAGE_END); // Equivalent to SOUTH in BorderLayout
        openButton.addActionListener(e -> {
            try {
                fileChooser.setCurrentDirectory(FileSystemView.getFileSystemView().getHomeDirectory());
                fileChooser.setVisible(true);
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) { // Selected element must be text-based file
                    selectedFile = fileChooser.getSelectedFile();
                    String loadedFileContent = new String(Files.readAllBytes(selectedFile.toPath())); // Create a string that contains all the text in the selected file
                    textArea.replaceRange(loadedFileContent, 0, textArea.getText().length()); // Write the contents of the selected file to the textArea
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                fileChooser.setVisible(false);
            }
            /* OPEN TESTING BLOCK - Set the instance variable selectedFile to the testing text file
            String loadedFileContent = null;
            try {
                loadedFileContent = new String(Files.readAllBytes(selectedFile.toPath()));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            textArea.replaceRange(loadedFileContent, 0, textArea.getText().length());
            */
        });
        // Set openButton icon
        String openIconFilePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("OpenIcon.jpeg")).getFile();
        openButton.setIcon(new ImageIcon(openIconFilePath));
        openButton.setBorder(BorderFactory.createEmptyBorder());
        c.gridx = 0;
        mainPanel.add(openButton, c);

        // SAVE BUTTON - Writes the contents of TextArea onto the selected file, overwriting existing content
        JButton saveButton = new JButton("");
        saveButton.setName("SaveButton");
        saveButton.addActionListener(e -> {
            try {
                fileChooser.setVisible(true);
                if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    FileWriter writer = new FileWriter(selectedFile);
                    writer.write(textArea.getText());
                    writer.close();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                fileChooser.setVisible(false);
            }
        });
        // Set saveButton icon
        String saveIconFilePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("SaveIcon.jpg")).getFile();
        saveButton.setIcon(new ImageIcon(saveIconFilePath));
        saveButton.setBorder(BorderFactory.createEmptyBorder());
        c.gridx = 1;
        mainPanel.add(saveButton, c);

        // * SEARCH PANEL - Iterates through all the matches in the text; regex matching can be toggled
        // SEARCH TEXT FIELD
        JTextField textField = new JTextField();
        textField.setName("SearchField");
        c.weightx = 5;
        c.gridx = 2;
        mainPanel.add(textField, c);

        // SEARCH BUTTON - Selects the first match in the text
        JButton searchButton = new JButton("");
        searchButton.setName("StartSearchButton");
        searchButton.addActionListener(e -> {
            Thread searchThread = new Thread(() -> { // Search done in separate thread to prevent GUI thread lag
                matchStartIndexes.clear(); // Clear the matchStartIndexes list on each new search
                String searchInput = textField.getText();
                if (!regexSelected) { // If regex search is not selected and searchInput contains non-alphanumeric characters, create and pass in string with literal versions of these special chars.
                    if (searchInput.matches(".*\\W.*")) {
                        StringBuilder builder = new StringBuilder();
                        String curChar;
                        for (int i = 0; i < searchInput.length(); i++) {
                            curChar = String.valueOf(searchInput.charAt(i));
                            if (curChar.matches("\\W")) {
                                builder.append("\\").append(curChar);
                            } else {
                                builder.append(curChar);
                            }
                        }
                        searchInput = builder.toString();
                    }
                }
                Pattern searchPattern = Pattern.compile(searchInput);
                matcher = searchPattern.matcher(textArea.getText()); // Instance variable matcher now holds current matcher object
                if (matcher.find()) { // Move carat to end of the current match in the textArea, select the match, and focus on textArea
                    matchStartIndexes.add(matcher.start());
                    textArea.setCaretPosition(matcher.start() + matcher.group().length());
                    textArea.select(matcher.start(), matcher.start() + matcher.group().length());
                    textArea.grabFocus();
                } else {
                    textArea.setCaretPosition(0);
                    System.out.println("NO MATCH FOUND");
                }
            });
            searchThread.start();
        });
        // Set searchButton icon
        String searchIconFilePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("SearchIcon.jpg")).getFile();
        searchButton.setIcon(new ImageIcon(searchIconFilePath));
        searchButton.setBorder(BorderFactory.createEmptyBorder());
        c.gridx = 3;
        c.weightx = 0.1; // All the components to the right of search bar have weightx of 0.1
        mainPanel.add(searchButton, c);

        // PREVIOUS MATCH BUTTON - Iterates through the matches backwards by using the matchStartIndexes list to get the desired match. If clicked while at the first match, wraps around to the last match
        JButton previousButton = new JButton("");
        previousButton.setName("PreviousMatchButton");
        previousButton.addActionListener(e -> {
            // Four cases:
            // 1. Current match is not the first or last match: Select the match preceding it.
            // 2. Current match is the first match: Wrap around to the last match.
            // 3. Current match is the last match & nextButton was clicked, thus clearing matchStartIndexes & resetting matcher: Select the penultimate match
            // 4. No matches exist: Do nothing
            try {
                int desiredStart;
                if (matchStartIndexes.size() > 1) { // If matcher has been iterated forward more than once, select the match that precedes the current match
                    desiredStart = matchStartIndexes.get(matchStartIndexes.indexOf(matcher.start()) - 1);
                } else { // Either current match is the first match, the last match after matchStartIndexes cleared & matcher reset, or no matches exist
                    int firstOrLast = 1 - matchStartIndexes.size(); // If matchStartIndexes is empty but matches exist, select penultimate match. If matchStartIndexes only has one match, select the final match.
                    while (matcher.find()) { // Regardless of case iterate forward through all matches, adding the match start indexes to matchStartIndexes
                        matchStartIndexes.add(matcher.start());
                    }
                    desiredStart = matchStartIndexes.size() - 1 - firstOrLast > 0 ? matchStartIndexes.get(matchStartIndexes.size() - 1 - firstOrLast) : 0; // If calculated index is negative, no matches exist
                }
                matchStartIndexes.clear(); // Clear matchStartIndexes since the matcher will be reset and iterated forward, adding start indexes one by one again
                matcher.reset(); // Reset the matcher object in order to start at the first match and iterate to the desired match
                while (matcher.find()) {
                    matchStartIndexes.add(matcher.start());
                    if (matcher.start() == desiredStart) { // Once at the desired match, select it and break out of the loop
                        textArea.setCaretPosition(matcher.start() + matcher.group().length());
                        textArea.select(matcher.start(), matcher.start() + matcher.group().length());
                        textArea.grabFocus();
                        break;
                    }
                }
            } catch (IllegalStateException illState) {
                illState.printStackTrace();
            }
        });
        // Set previousButton icon
        String previousIconFilePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("PreviousIcon.jpg")).getFile();
        previousButton.setIcon(new ImageIcon(previousIconFilePath));
        previousButton.setBorder(BorderFactory.createEmptyBorder());
        c.gridx = 4;
        mainPanel.add(previousButton, c);

        // NEXT MATCH BUTTON - Iterates forward through the matches. If clicked while at the last match, matcher is reset. If clicked again, wraps around to the first match.
        JButton nextButton = new JButton("");
        nextButton.setName("NextMatchButton");
        nextButton.addActionListener(e -> {
            try {
                if (matcher.find()) {
                    matchStartIndexes.add(matcher.start());
                    textArea.setCaretPosition(matcher.start() + matcher.group().length());
                    textArea.select(matcher.start(), matcher.start() + matcher.group().length());
                    textArea.grabFocus();
                    System.out.println(matcher.start());
                } else {
                    matchStartIndexes.clear();
                    matcher.reset();
                    System.out.println("NEXT MATCH NOT FOUND");
                }
            } catch (NullPointerException nullPointEx) {
                nullPointEx.printStackTrace();
            }
        });
        // Set nextButton icon
        String nextIconFilePath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("NextIcon.jpg")).getFile();
        nextButton.setIcon(new ImageIcon(nextIconFilePath));
        nextButton.setBorder(BorderFactory.createEmptyBorder());
        c.gridx = 5;
        mainPanel.add(nextButton, c);

        // REGEX CHECKBOX
        JCheckBox regexCheckbox = new JCheckBox("Use Regex");
        regexCheckbox.setName("UseRegExCheckbox");
        regexCheckbox.addActionListener(e -> regexSelected = regexCheckbox.isSelected());
        c.gridx = 6;
        mainPanel.add(regexCheckbox, c);

        // * MENU BAR - Contains File & Search menus
        JMenuBar menuBar = new JMenuBar();

        // FILE MENU
        JMenu fileMenu = new JMenu("File");
        fileMenu.setName("MenuFile");

        // OPEN
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.setName("MenuOpen");
        openMenuItem.addActionListener(e -> openButton.doClick());

        // SAVE
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setName("MenuSave");
        saveMenuItem.addActionListener(e -> saveButton.doClick());

        // EXIT
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setName("MenuExit");
        exitMenuItem.addActionListener(e -> dispose());

        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(exitMenuItem);

        // SEARCH MENU
        JMenu searchMenu = new JMenu("Search");
        searchMenu.setName("MenuSearch");

        // START SEARCH
        JMenuItem startSearch = new JMenuItem("Start Search");
        startSearch.setName("MenuStartSearch");
        startSearch.addActionListener(e -> searchButton.doClick());

        // PREVIOUS MATCH
        JMenuItem previousMatch = new JMenuItem("Previous Match");
        previousMatch.setName("MenuPreviousMatch");
        previousMatch.addActionListener(e -> previousButton.doClick());

        // NEXT MATCH
        JMenuItem nextMatch = new JMenuItem("Next Match");
        nextMatch.setName("MenuNextMatch");
        nextMatch.addActionListener(e -> nextButton.doClick());

        // USE REGEX
        JMenuItem useRegex = new JMenuItem("Use Regular Expressions");
        useRegex.setName("MenuUseRegExp");
        useRegex.addActionListener(e -> regexCheckbox.doClick());

        searchMenu.add(startSearch);
        searchMenu.add(previousMatch);
        searchMenu.add(nextMatch);
        searchMenu.add(useRegex);

        menuBar.add(fileMenu);
        menuBar.add(searchMenu);
        setJMenuBar(menuBar);

        setVisible(true);
    }

    public static void setMargin(JComponent aComponent, int aTop, int aRight, int aBottom, int aLeft) {
        Border border = aComponent.getBorder();
        Border marginBorder = new EmptyBorder(new Insets(aTop, aLeft, aBottom, aRight));
        aComponent.setBorder(border == null ? marginBorder : new CompoundBorder(marginBorder, border));
    }

    public static void main(String[] args) {
        new TextEditor();
    }
}
