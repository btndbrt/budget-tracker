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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * The main window. At the top you choose the period (month); below are its categories on the left
 * and the transactions of the selected category on the right.
 */
public class BudgetFrame extends JFrame {

    private final Budget budget;
    private final Path dataFile;
    private Period currentPeriod; // the period shown in the window

    private final DefaultComboBoxModel<Period> periodComboModel = new DefaultComboBoxModel<>();
    private final JComboBox<Period> periodCombo = new JComboBox<>(periodComboModel);
    private boolean fillingPeriodCombo = false; // true while we refill the drop-down ourselves

    private final DefaultListModel<Category> categoryListModel = new DefaultListModel<>();
    private final JList<Category> categoryList = new JList<>(categoryListModel);
    private final TransactionTableModel tableModel = new TransactionTableModel();
    private final JTable transactionTable = new JTable(tableModel);
    private final JLabel categoryTotalLabel = new JLabel();
    private final JLabel netTotalLabel = new JLabel();
    private final JTextField currencyField = new JTextField(6);

    public BudgetFrame(Budget budget, Path dataFile) {
        super("Budget Tracker");
        this.budget = budget;
        this.dataFile = dataFile;

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildCategoryPanel(), buildTransactionPanel());
        split.setDividerLocation(250);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel netPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        netPanel.add(netTotalLabel);
        bottom.add(netPanel, BorderLayout.CENTER);
        bottom.add(buildCurrencyPanel(), BorderLayout.EAST);

        add(buildPeriodPanel(), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 550);
        setLocationRelativeTo(null); // center on screen

        fillPeriodCombo(periodToShowAtStart());
    }

    // ---------- Layout ----------

    private JPanel buildPeriodPanel() {
        periodCombo.addActionListener(e -> {
            if (!fillingPeriodCombo) {
                showPeriod((Period) periodCombo.getSelectedItem());
            }
        });

        JButton newButton = new JButton("New period");
        JButton editButton = new JButton("Edit dates");
        JButton deleteButton = new JButton("Delete period");
        newButton.addActionListener(e -> addPeriod());
        editButton.addActionListener(e -> editPeriod());
        deleteButton.addActionListener(e -> deletePeriod());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Period:"));
        panel.add(periodCombo);
        panel.add(newButton);
        panel.add(editButton);
        panel.add(deleteButton);
        return panel;
    }

    private JPanel buildCategoryPanel() {
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setCellRenderer(new CategoryRenderer());
        categoryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editCategory();
                }
            }
        });
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                tableModel.setCategory(categoryList.getSelectedValue());
                refreshTotals();
            }
        });

        JButton addButton = new JButton("New");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        addButton.addActionListener(e -> addCategory());
        editButton.addActionListener(e -> editCategory());
        deleteButton.addActionListener(e -> deleteCategory());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(editButton);
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

    private JPanel buildCurrencyPanel() {
        currencyField.setText(budget.getCurrency());
        // Update the amounts on every keystroke, no button needed
        currencyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                currencyChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                currencyChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                currencyChanged();
            }
        });

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(new JLabel("Currency:"));
        panel.add(currencyField);
        return panel;
    }

    private void currencyChanged() {
        budget.setCurrency(currencyField.getText().trim());
        transactionTable.repaint(); // the table shows amounts too
        changed();
    }

    // ---------- Period actions ----------

    /** The period that contains today, or else the latest one. */
    private Period periodToShowAtStart() {
        for (Period p : budget.getPeriods()) {
            if (p.contains(LocalDate.now())) {
                return p;
            }
        }
        return budget.getLatestPeriod();
    }

    /** Refills the drop-down (newest period first) and shows the given period. */
    private void fillPeriodCombo(Period toSelect) {
        fillingPeriodCombo = true; // stop the drop-down's listener from reacting to our changes
        periodComboModel.removeAllElements();
        List<Period> periods = budget.getPeriods();
        for (int i = periods.size() - 1; i >= 0; i--) {
            periodComboModel.addElement(periods.get(i));
        }
        periodComboModel.setSelectedItem(toSelect);
        fillingPeriodCombo = false;
        showPeriod(toSelect);
    }

    /** Shows the categories of the given period. */
    private void showPeriod(Period period) {
        currentPeriod = period;
        categoryListModel.clear();
        for (Category c : period.getCategories()) {
            categoryListModel.addElement(c);
        }
        if (!categoryListModel.isEmpty()) {
            categoryList.setSelectedIndex(0);
        }
        refreshTotals();
    }

    private void addPeriod() {
        Period period = budget.nextPeriod();
        // The new period already has the categories and amounts of the latest one
        if (!showPeriodDialog(period, "New period")) {
            return;
        }
        budget.addPeriod(period);
        fillPeriodCombo(period);
        changed();
    }

    private void editPeriod() {
        if (showPeriodDialog(currentPeriod, "Edit period dates")) {
            budget.sortPeriods();
            fillPeriodCombo(currentPeriod); // the dates in the drop-down changed
            changed();
        }
    }

    private void deletePeriod() {
        if (budget.getPeriods().size() == 1) {
            showMessage("This is the only period, it cannot be deleted.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete the period " + currentPeriod + " with all its categories and transactions?",
                "Delete period", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        budget.getPeriods().remove(currentPeriod);
        fillPeriodCombo(budget.getLatestPeriod());
        changed();
    }

    /**
     * Shows a form for the start and end date of the period. If the user presses OK and the
     * dates are valid, the period is updated and true is returned.
     */
    private boolean showPeriodDialog(Period period, String title) {
        DateField startField = new DateField(period.getStart());
        DateField endField = new DateField(period.getEnd());

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Start date:"));
        form.add(startField);
        form.add(new JLabel("End date:"));
        form.add(endField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, form, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
            LocalDate start = startField.getDate();
            LocalDate end = endField.getDate();
            if (start == null || end == null) {
                showMessage("Invalid date. Use the format YYYY-MM-DD, e.g. 2026-10-19.");
                continue;
            }
            if (end.isBefore(start)) {
                showMessage("The end date cannot be before the start date.");
                continue;
            }
            Period overlap = budget.findOverlap(start, end, period);
            if (overlap != null) {
                showMessage("These dates overlap with the period " + overlap + ".");
                continue;
            }
            period.setStart(start);
            period.setEnd(end);
            return true;
        }
    }

    // ---------- Category actions ----------

    private void addCategory() {
        Category category = new Category("", BigDecimal.ZERO);
        if (!showCategoryDialog(category, "New category")) {
            return;
        }
        currentPeriod.getCategories().add(category);
        categoryListModel.addElement(category);
        categoryList.setSelectedValue(category, true);
        changed();
    }

    private void editCategory() {
        Category category = categoryList.getSelectedValue();
        if (category == null) {
            showMessage("Select a category first.");
            return;
        }
        if (showCategoryDialog(category, "Edit category")) {
            changed();
        }
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
        currentPeriod.getCategories().remove(category);
        categoryListModel.removeElement(category);
        changed();
    }

    /**
     * Shows a form for the category's name and budget amount. If the user presses OK and the
     * input is valid, the category is updated and true is returned.
     */
    private boolean showCategoryDialog(Category category, String title) {
        String currentName = category.getName();
        JTextField nameField = new JTextField(currentName, 20);
        JTextField amountField = new JTextField(
                category.getBudgetAmount().signum() == 0 ? "" : category.getBudgetAmount().toPlainString());

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Budget amount:"));
        form.add(amountField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, form, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showMessage("The name cannot be empty.");
                continue;
            }
            if (!name.equalsIgnoreCase(currentName) && currentPeriod.hasCategory(name)) {
                showMessage("A category with this name already exists.");
                continue;
            }
            BigDecimal amount = parseAmount(amountField.getText());
            if (amount == null) {
                showMessage("Invalid amount. Examples: 400, 120.50");
                continue;
            }
            category.setName(name);
            category.setBudgetAmount(amount);
            return true;
        }
    }

    // ---------- Transaction actions ----------

    private void addTransaction() {
        Category category = categoryList.getSelectedValue();
        if (category == null) {
            showMessage("Select or create a category first.");
            return;
        }
        // Today if it is in this period, otherwise the first day of the period
        LocalDate today = LocalDate.now();
        LocalDate date = currentPeriod.contains(today) ? today : currentPeriod.getStart();
        Transaction t = new Transaction(date, "", BigDecimal.ZERO);
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
        DateField dateField = new DateField(t.getDate());
        JTextField descriptionField = new JTextField(t.getDescription(), 20);
        JTextField amountField = new JTextField(t.getAmount().signum() == 0 ? "" : t.getAmount().toPlainString());

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Date:"));
        form.add(dateField);
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        form.add(new JLabel("Amount spent:"));
        form.add(amountField);

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, form, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
            LocalDate date = dateField.getDate();
            if (date == null) {
                showMessage("Invalid date. Use the format YYYY-MM-DD, e.g. 2026-10-19.");
                continue;
            }
            BigDecimal amount = parseAmount(amountField.getText());
            if (amount == null) {
                showMessage("Invalid amount. Examples: 12.50 (a refund can be negative: -5)");
                continue;
            }
            if (!currentPeriod.contains(date)) {
                int answer = JOptionPane.showConfirmDialog(this,
                        date + " is outside the period " + currentPeriod + ". Save anyway?",
                        "Date outside period", JOptionPane.YES_NO_OPTION);
                if (answer != JOptionPane.YES_OPTION) {
                    continue;
                }
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
        categoryTotalLabel.setText(category == null ? "" : summary("Budget", category.getBudgetAmount(),
                category.getSpent(), category.getRemaining()));
        netTotalLabel.setText(summary("Net budget", currentPeriod.getTotalBudget(), currentPeriod.getTotalSpent(),
                currentPeriod.getTotalRemaining()));
        categoryList.repaint(); // the list shows amounts too
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private String summary(String label, BigDecimal budgetAmount, BigDecimal spent, BigDecimal remaining) {
        return label + ": " + formatMoney(budgetAmount) + "   Spent: " + formatMoney(spent)
                + "   Remaining: " + formatMoney(remaining);
    }

    /** Parses "12.50" or "12,50". Returns null if the text is not a number. */
    private static BigDecimal parseAmount(String text) {
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Formats an amount with two decimals and the currency, e.g. "1,500.00 HUF". */
    private String formatMoney(BigDecimal amount) {
        String text = String.format("%,.2f", amount);
        return budget.getCurrency().isEmpty() ? text : text + " " + budget.getCurrency();
    }

    /** Shows each category as "Name  (remaining / budget amount)". */
    // Not static, because it needs formatMoney(), which uses this window's budget
    private class CategoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Category c = (Category) value;
            setText(c.getName() + "  (" + formatMoney(c.getRemaining()) + " / " + formatMoney(c.getBudgetAmount()) + ")");
            return this;
        }
    }

    /** Shows amounts right-aligned with two decimals. */
    private class AmountRenderer extends DefaultTableCellRenderer {
        AmountRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object value) {
            setText(value == null ? "" : formatMoney((BigDecimal) value));
        }
    }
}
