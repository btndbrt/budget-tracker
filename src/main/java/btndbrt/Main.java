package btndbrt;

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
            try {
                // Look like a normal app on this operating system
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to the default look
            }

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
}
