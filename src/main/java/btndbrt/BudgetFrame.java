package btndbrt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/** The main window: categories on the left, transactions of the selected category on the right. */
public class BudgetFrame extends JFrame {

    private final Budget budget;
    private final Path dataFile;

    private final DefaultListModel<Category> categoryListModel = new DefaultListModel<>();
    private final JList<Category> categoryList = new JList<>(categoryListModel);
    private final TransactionTableModel tableModel = new TransactionTableModel();
    private final JTable transactionTable = new JTable(tableModel);
    private final JLabel categoryTotalLabel = new JLabel();
    private final JLabel netTotalLabel = new JLabel();

    public BudgetFrame(Budget budget, Path dataFile) {
        super("Budget Tracker");
        this.budget = budget;
        this.dataFile = dataFile;

        for (Category c : budget.getCategories()) {
            categoryListModel.addElement(c);
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildCategoryPanel(), buildTransactionPanel());
        split.setDividerLocation(250);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(netTotalLabel);

        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null); // center on screen

        if (!categoryListModel.isEmpty()) {
            categoryList.setSelectedIndex(0);
        }
        refreshTotals();
    }

    // ---------- Layout ----------

    private JPanel buildCategoryPanel() {
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setCellRenderer(new CategoryRenderer());
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                tableModel.setCategory(categoryList.getSelectedValue());
                refreshTotals();
            }
        });

        JButton addButton = new JButton("New");
        JButton renameButton = new JButton("Rename");
        JButton deleteButton = new JButton("Delete");
        addButton.addActionListener(e -> addCategory());
        renameButton.addActionListener(e -> renameCategory());
        deleteButton.addActionListener(e -> deleteCategory());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(renameButton);
        buttons.add(deleteButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Categories"));
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTransactionPanel() {
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setAutoCreateRowSorter(true); // click a column header to sort
        transactionTable.setDefaultRenderer(BigDecimal.class, new AmountRenderer());
        transactionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editTransaction();
                }
            }
        });

        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        addButton.addActionListener(e -> addTransaction());
        editButton.addActionListener(e -> editTransaction());
        deleteButton.addActionListener(e -> deleteTransaction());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(categoryTotalLabel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Transactions"));
        panel.add(new JScrollPane(transactionTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // ---------- Category actions ----------

    private void addCategory() {
        String name = askCategoryName("New category name:", "");
        if (name == null) {
            return;
        }
        Category category = new Category(name);
        budget.getCategories().add(category);
        categoryListModel.addElement(category);
        categoryList.setSelectedValue(category, true);
        changed();
    }

    private void renameCategory() {
        Category category = categoryList.getSelectedValue();
        if (category == null) {
            showMessage("Select a category first.");
            return;
        }
        String name = askCategoryName("New name:", category.getName());
        if (name == null) {
            return;
        }
        category.setName(name);
        changed();
    }

    private void deleteCategory() {
        Category category = categoryList.getSelectedValue();
        if (category == null) {
            showMessage("Select a category first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + category.getName() + "\" and its " + category.getTransactions().size() + " transaction(s)?",
                "Delete category", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        budget.getCategories().remove(category);
        categoryListModel.removeElement(category);
        changed();
    }

    /** Asks for a category name. Returns null if the user cancelled. */
    private String askCategoryName(String prompt, String currentName) {
        String shown = currentName;
        while (true) {
            String name = (String) JOptionPane.showInputDialog(this, prompt, "Category",
                    JOptionPane.PLAIN_MESSAGE, null, null, shown);
            if (name == null) {
                return null;
            }
            name = name.trim();
            if (name.isEmpty()) {
                showMessage("The name cannot be empty.");
            } else if (!name.equalsIgnoreCase(currentName) && budget.hasCategory(name)) {
                showMessage("A category with this name already exists.");
            } else {
                return name;
            }
            shown = name; // keep what the user typed so they can fix it
        }
    }

    // ---------- Transaction actions ----------

    private void addTransaction() {
        Category category = categoryList.getSelectedValue();
        if (category == null) {
            showMessage("Select or create a category first.");
            return;
        }
        Transaction t = new Transaction(LocalDate.now(), "", BigDecimal.ZERO);
        if (showTransactionDialog(t, "Add transaction")) {
            category.getTransactions().add(t);
            tableModel.fireTableDataChanged();
            changed();
        }
    }

    private void editTransaction() {
        int row = selectedTransactionRow();
        if (row < 0) {
            return;
        }
        if (showTransactionDialog(tableModel.getTransaction(row), "Edit transaction")) {
            tableModel.fireTableRowsUpdated(row, row);
            changed();
        }
    }

    private void deleteTransaction() {
        int row = selectedTransactionRow();
        if (row < 0) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete the selected transaction?",
                "Delete transaction", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        categoryList.getSelectedValue().getTransactions().remove(row);
        tableModel.fireTableRowsDeleted(row, row);
        changed();
    }

    /** Returns the selected row in model terms, or -1 (and shows a message) if nothing is selected. */
    private int selectedTransactionRow() {
        int viewRow = transactionTable.getSelectedRow();
        if (viewRow < 0) {
            showMessage("Select a transaction first.");
            return -1;
        }
        // When the table is sorted, the visible row number differs from the index in our list
        return transactionTable.convertRowIndexToModel(viewRow);
    }

    /**
     * Shows a form for the given transaction. If the user presses OK and the input is valid,
     * the transaction is updated and true is returned.
     */
    private boolean showTransactionDialog(Transaction t, String title) {
        JTextField dateField = new JTextField(t.getDate().toString());
        JTextField descriptionField = new JTextField(t.getDescription(), 20);
        JTextField amountField = new JTextField(t.getAmount().signum() == 0 ? "" : t.getAmount().toPlainString());

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Date (YYYY-MM-DD):"));
        form.add(dateField);
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        form.add(new JLabel("Amount (- for expense):"));
        form.add(amountField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, form, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
            LocalDate date;
            BigDecimal amount;
            try {
                date = LocalDate.parse(dateField.getText().trim());
            } catch (DateTimeParseException e) {
                showMessage("Invalid date. Use the format YYYY-MM-DD, e.g. 2026-10-19.");
                continue;
            }
            try {
                amount = new BigDecimal(amountField.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                showMessage("Invalid amount. Examples: 1500, -12.50");
                continue;
            }
            t.setDate(date);
            t.setDescription(descriptionField.getText().trim());
            t.setAmount(amount);
            return true;
        }
    }

    // ---------- Helpers ----------

    /** Call after every change: updates the totals and saves to disk. */
    private void changed() {
        refreshTotals();
        try {
            BudgetStorage.save(budget, dataFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not save to " + dataFile + ":\n" + e.getMessage(),
                    "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTotals() {
        Category category = categoryList.getSelectedValue();
        categoryTotalLabel.setText(category == null ? "" : "Category total: " + formatMoney(category.getTotal()));
        netTotalLabel.setText("Net budget: " + formatMoney(budget.getNetTotal()));
        categoryList.repaint(); // the list shows totals too
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    static String formatMoney(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }

    /** Shows each category as "Name (total)". */
    private static class CategoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Category c = (Category) value;
            setText(c.getName() + "  (" + formatMoney(c.getTotal()) + ")");
            return this;
        }
    }

    /** Shows amounts right-aligned with two decimals. */
    private static class AmountRenderer extends DefaultTableCellRenderer {
        AmountRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object value) {
            setText(value == null ? "" : formatMoney((BigDecimal) value));
        }
    }
}
