package btndbrt;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    /** Where the budget is stored: budget-tracker.txt in the user's home folder. */
    static final Path DATA_FILE = Path.of(System.getProperty("user.home"), "budget-tracker.txt");

    public static void main(String[] args) {
        // Swing code must run on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            setUpLookAndFeel();

            Budget budget;
            try {
                budget = BudgetStorage.load(DATA_FILE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Could not load " + DATA_FILE + ":\n" + e.getMessage(),
                        "Load error", JOptionPane.ERROR_MESSAGE);
                return; // don't start with an empty budget, it would overwrite the file on the first change
            }
            if (budget.getPeriods().isEmpty()) {
                budget.addPeriod(budget.nextPeriod()); // first start: the current month
            }
            new BudgetFrame(budget, DATA_FILE).setVisible(true);
        });
    }

    /** FlatLaf gives Swing a clean, modern look. The UIManager keys fine-tune it for the whole app. */
    private static void setUpLookAndFeel() {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);            // rounded buttons
        UIManager.put("Component.arc", 8);         // rounded combo boxes, spinners
        UIManager.put("TextComponent.arc", 8);     // rounded text fields
        UIManager.put("ScrollBar.thumbArc", 999);  // pill-shaped scroll bars
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("TabbedPane.showTabSeparators", false);
        // Light blue selection with dark text, instead of strong blue with white text
        Color selection = new Color(0xdbe8f8);
        UIManager.put("List.selectionBackground", selection);
        UIManager.put("List.selectionInactiveBackground", selection);
        UIManager.put("List.selectionForeground", ChartColors.TEXT);
        UIManager.put("List.selectionInactiveForeground", ChartColors.TEXT);
        UIManager.put("Table.selectionBackground", selection);
        UIManager.put("Table.selectionInactiveBackground", selection);
        UIManager.put("Table.selectionForeground", ChartColors.TEXT);
        UIManager.put("Table.selectionInactiveForeground", ChartColors.TEXT);
    }
}
