package btndbrt;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads a budget as a plain text file (you can open it in any text editor).
 * One line per item, fields separated by tabs:
 *
 * <pre>
 * CATEGORY  Groceries   400.00
 * TX        2026-10-19  12.50   Bread and milk
 * </pre>
 *
 * Each TX line belongs to the CATEGORY line above it.
 */
public class BudgetStorage {

    private static final String SEPARATOR = "\t";

    public static void save(Budget budget, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Category c : budget.getCategories()) {
            lines.add("CATEGORY" + SEPARATOR + clean(c.getName()) + SEPARATOR + c.getBudgetAmount().toPlainString());
            for (Transaction t : c.getTransactions()) {
                lines.add("TX" + SEPARATOR + t.getDate() + SEPARATOR + t.getAmount().toPlainString()
                        + SEPARATOR + clean(t.getDescription()));
            }
        }
        Files.write(file, lines);
    }

    /** Loads the budget from the file. Returns an empty budget if the file does not exist yet. */
    public static Budget load(Path file) throws IOException {
        Budget budget = new Budget();
        if (!Files.exists(file)) {
            return budget;
        }
        Category current = null;
        int lineNumber = 0;
        for (String line : Files.readAllLines(file)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            // limit -1 keeps empty trailing fields (e.g. an empty description)
            String[] parts = line.split(SEPARATOR, -1);
            try {
                if (parts[0].equals("CATEGORY")) {
                    // Files from older versions have no budget amount, so use 0
                    BigDecimal budgetAmount = parts.length > 2 ? new BigDecimal(parts[2]) : BigDecimal.ZERO;
                    current = new Category(parts[1], budgetAmount);
                    budget.getCategories().add(current);
                } else if (parts[0].equals("TX") && current != null) {
                    LocalDate date = LocalDate.parse(parts[1]);
                    BigDecimal amount = new BigDecimal(parts[2]);
                    current.getTransactions().add(new Transaction(date, parts[3], amount));
                } else {
                    throw new IOException("Unexpected content on line " + lineNumber + " of " + file);
                }
            } catch (RuntimeException e) {
                throw new IOException("Broken line " + lineNumber + " in " + file + ": " + line, e);
            }
        }
        return budget;
    }

    /** Tabs and line breaks would break the file format, so replace them with spaces. */
    private static String clean(String text) {
        return text.replaceAll("[\\t\\r\\n]", " ");
    }
}
