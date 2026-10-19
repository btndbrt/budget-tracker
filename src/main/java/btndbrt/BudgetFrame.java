package btndbrt;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * The main window. At the top: the period, the currency and summary tiles.
 * Below, the "Budget" tab has the categories and transactions, the "Charts" tab the charts.
 */
public class BudgetFrame extends JFrame {

    private static final DateTimeFormatter TILE_DATE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private final Budget budget;
    private final Path dataFile;
    private Period currentPeriod; // the period shown in the window

    private final DefaultComboBoxModel<Period> periodComboModel = new DefaultComboBoxModel<>();
    private final JComboBox<Period> periodCombo = new JComboBox<>(periodComboModel);
    private boolean fillingPeriodCombo = false; // true while we refill the drop-down ourselves

    private final StatTile todayTile = new StatTile("Today");
    private final StatTile budgetTile = new StatTile("Budget");
    private final StatTile spentTile = new StatTile("Spent");
    private final StatTile remainingTile = new StatTile("Remaining");
    private final StatTile perDayTile = new StatTile("Left per day");

    private final DefaultListModel<Category> categoryListModel = new DefaultListModel<>();
    private final JList<Category> categoryList = new JList<>(categoryListModel);
    private final CategoryCell categoryCell = new CategoryCell();
    private final TransactionTableModel tableModel = new TransactionTableModel();
    private final JTable transactionTable = new JTable(tableModel);
    private final Card transactionsCard = new Card("Transactions");
    private final JLabel categoryTotalLabel = new JLabel();
    private final JTextField currencyField = new JTextField(6);

    private final PieChartPanel pieChart = new PieChartPanel();
    private final LineChartPanel lineChart = new LineChartPanel();
    private final Card lineChartCard = new Card("Spending over time");
    private final JCheckBox onlySelectedCategoryBox = new JCheckBox();

    public BudgetFrame(Budget budget, Path dataFile) {
        super("Budget Tracker");
        this.budget = budget;
        this.dataFile = dataFile;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Budget", buildBudgetTab());
        tabs.addTab("Charts", buildChartsTab());

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        installClickAway(root);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setLocationRelativeTo(null); // center on screen

        fillPeriodCombo(periodToShowAtStart());

        // Check once a minute, so "today" is right even if the app stays open past midnight
        new Timer(60_000, e -> refreshTotals()).start();
    }

    // ---------- Layout ----------

    /** Period controls and currency on top, summary tiles below. */
    private JPanel buildHeader() {
        periodCombo.addActionListener(e -> {
            if (!fillingPeriodCombo) {
                showPeriod((Period) periodCombo.getSelectedItem());
            }
        });

        JButton newButton = primaryButton("+ New period");
        JButton editButton = new JButton("Edit dates");
        JButton deleteButton = new JButton("Delete");
        newButton.addActionListener(e -> addPeriod());
        editButton.addActionListener(e -> editPeriod());
        deleteButton.addActionListener(e -> deletePeriod());

        JPanel periodControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        periodControls.add(new JLabel("Period"));
        periodControls.add(periodCombo);
        periodControls.add(editButton);
        periodControls.add(deleteButton);
        periodControls.add(newButton);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(periodControls, BorderLayout.WEST);
        topRow.add(buildCurrencyPanel(), BorderLayout.EAST);

        JPanel tiles = new JPanel(new GridLayout(1, 5, 12, 0));
        tiles.add(todayTile);
        tiles.add(budgetTile);
        tiles.add(spentTile);
        tiles.add(remainingTile);
        tiles.add(perDayTile);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.add(topRow, BorderLayout.NORTH);
        header.add(tiles, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildBudgetTab() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildCategoryCard(), buildTransactionsCard());
        split.setDividerLocation(360);
        split.setDividerSize(12);
        split.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        split.setOpaque(false);
        return split;
    }

    private JComponent buildChartsTab() {
        onlySelectedCategoryBox.setOpaque(false);
        onlySelectedCategoryBox.addActionListener(e -> refreshCharts());

        Card pieCard = new Card("Spending by category");
        pieCard.setContent(pieChart);
        lineChartCard.addToHeader(onlySelectedCategoryBox);
        lineChartCard.setContent(lineChart);

        JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        panel.add(pieCard);
        panel.add(lineChartCard);
        return panel;
    }

    private Card buildCategoryCard() {
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.setCellRenderer(categoryCell);
        categoryList.setBackground(ChartColors.SURFACE);
        categoryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // A JList selects the last row even when you click the empty space below it,
                // so clear the selection if the click was not on a row
                if (!isOnCategoryRow(e)) {
                    categoryList.clearSelection();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && isOnCategoryRow(e)) {
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

        JButton addButton = primaryButton("+ New");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        addButton.addActionListener(e -> addCategory());
        editButton.addActionListener(e -> editCategory());
        deleteButton.addActionListener(e -> deleteCategory());

        Card card = new Card("Categories");
        card.addToHeader(editButton);
        card.addToHeader(deleteButton);
        card.addToHeader(addButton);
        card.setContent(borderless(new JScrollPane(categoryList)));
        return card;
    }

    private Card buildTransactionsCard() {
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setAutoCreateRowSorter(true); // click a column header to sort
        transactionTable.setDefaultRenderer(BigDecimal.class, new AmountRenderer());
        transactionTable.setBackground(ChartColors.SURFACE);
        transactionTable.setFillsViewportHeight(true);
        transactionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (transactionTable.rowAtPoint(e.getPoint()) < 0) {
                    transactionTable.clearSelection(); // clicked the empty space below the rows
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && transactionTable.rowAtPoint(e.getPoint()) >= 0) {
                    editTransaction();
                }
            }
        });

        JButton addButton = primaryButton("+ Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        addButton.addActionListener(e -> addTransaction());
        editButton.addActionListener(e -> editTransaction());
        deleteButton.addActionListener(e -> deleteTransaction());

        categoryTotalLabel.setForeground(ChartColors.TEXT_SECONDARY);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(borderless(new JScrollPane(transactionTable)), BorderLayout.CENTER);
        content.add(categoryTotalLabel, BorderLayout.SOUTH);

        transactionsCard.addToHeader(editButton);
        transactionsCard.addToHeader(deleteButton);
        transactionsCard.addToHeader(addButton);
        transactionsCard.setContent(content);
        return transactionsCard;
    }

    private JPanel buildCurrencyPanel() {
        currencyField.setText(budget.getCurrency());
        currencyField.putClientProperty("JTextField.placeholderText", "e.g. HUF");
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

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.add(new JLabel("Currency"));
        panel.add(currencyField);
        return panel;
    }

    private void currencyChanged() {
        budget.setCurrency(currencyField.getText().trim());
        transactionTable.repaint(); // the table shows amounts too
        changed();
    }

    /** True if the mouse event is on a row of the category list (not the empty space below). */
    private boolean isOnCategoryRow(MouseEvent e) {
        int index = categoryList.locationToIndex(e.getPoint());
        return index >= 0 && categoryList.getCellBounds(index, index).contains(e.getPoint());
    }

    /**
     * Swing keeps the focus (the highlighted outline) on the last button or field you clicked,
     * even after you click somewhere empty. This moves the focus to the window background when you
     * click an empty area, and lets Escape do the same and clear the selected transaction.
     */
    private void installClickAway(JPanel root) {
        root.setFocusable(true); // so the background can take the focus

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            MouseEvent e = (MouseEvent) event;
            Component clicked = e.getComponent();
            boolean emptyArea = clicked instanceof JPanel || clicked instanceof JLabel || clicked instanceof JViewport;
            if (e.getID() == MouseEvent.MOUSE_PRESSED && emptyArea
                    && SwingUtilities.getWindowAncestor(clicked) == this) {
                transactionTable.clearSelection();
                root.requestFocusInWindow();
            }
        }, AWTEvent.MOUSE_EVENT_MASK);

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "clickAway");
        root.getActionMap().put("clickAway", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transactionTable.clearSelection();
                root.requestFocusInWindow();
            }
        });
    }

    /** A button in the accent color, for the main action of an area. */
    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        // FlatLaf style: blue background (same blue as the charts), white text
        button.putClientProperty("FlatLaf.style", "background: #2a78d6;"
                + "foreground: #ffffff;"
                + "borderColor: #2a78d6;"
                + "hoverBackground: #256abf;"
                + "pressedBackground: #1c5cab;"
                + "font: bold");
        return button;
    }

    /** Removes the scroll pane's own border, the card around it already has one. */
    private static JScrollPane borderless(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ChartColors.SURFACE);
        return scrollPane;
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
        transactionsCard.setTitle(category == null ? "Transactions" : category.getName());
        if (category == null) {
            categoryTotalLabel.setText("Select a category to see its transactions.");
        } else {
            String left = category.getRemaining().signum() < 0
                    ? "over by " + formatMoney(category.getRemaining().negate())
                    : formatMoney(category.getRemaining()) + " left";
            categoryTotalLabel.setText("Spent " + formatMoney(category.getSpent()) + " of "
                    + formatMoney(category.getBudgetAmount()) + "   \u00b7   " + left);
        }
        categoryCell.setCurrency(budget.getCurrency());
        categoryList.repaint(); // the list shows amounts too
        refreshTiles();
        refreshCharts();
    }

    /** The summary tiles at the top. */
    private void refreshTiles() {
        LocalDate today = LocalDate.now();
        boolean todayInPeriod = currentPeriod.contains(today);
        BigDecimal total = currentPeriod.getTotalBudget();
        BigDecimal spent = currentPeriod.getTotalSpent();
        BigDecimal remaining = currentPeriod.getTotalRemaining();
        boolean over = remaining.signum() < 0;

        if (todayInPeriod) {
            int daysLeft = currentPeriod.daysLeft(today);
            todayTile.setValue(today.format(TILE_DATE), "Day " + (currentPeriod.dayIndex(today) + 1) + " of "
                    + currentPeriod.getDayCount() + "  \u00b7  " + daysLeft + (daysLeft == 1 ? " day" : " days") + " left",
                    false);
            BigDecimal perDay = remaining.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP);
            perDayTile.setValue(formatMoney(perDay), "for the days left, today included", over);
        } else {
            todayTile.setValue(today.format(TILE_DATE), "Not in the shown period", false);
            perDayTile.setValue("\u2013", "Only for the current period", false);
        }

        int count = currentPeriod.getCategories().size();
        budgetTile.setValue(formatMoney(total), count + (count == 1 ? " category" : " categories"), false);
        String share = total.signum() > 0
                ? Math.round(100 * spent.doubleValue() / total.doubleValue()) + "% of the budget"
                : "";
        spentTile.setValue(formatMoney(spent), share, false);
        remainingTile.setValue(formatMoney(remaining), over ? "Over budget" : "", over);
    }

    private void refreshCharts() {
        pieChart.setPeriod(currentPeriod, budget.getCurrency());

        Category selected = categoryList.getSelectedValue();
        onlySelectedCategoryBox.setEnabled(selected != null);
        onlySelectedCategoryBox.setText("Only " + (selected == null ? "the selected category" : selected.getName()));
        if (onlySelectedCategoryBox.isSelected() && selected != null) {
            lineChartCard.setTitle("Spending over time: " + selected.getName());
            lineChart.setData(currentPeriod, List.of(selected), budget.getCurrency());
        } else {
            lineChartCard.setTitle("Spending over time");
            lineChart.setData(currentPeriod, currentPeriod.getCategories(), budget.getCurrency());
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
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
        return Money.format(amount, budget.getCurrency());
    }

    /** Shows amounts right-aligned with two decimals. */
    // Not static, because it needs formatMoney(), which uses this window's budget
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
