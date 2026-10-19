package btndbrt;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.swing.table.AbstractTableModel;

/** Tells the JTable how to show the transactions of one category. */
public class TransactionTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Date", "Description", "Amount"};

    private Category category; // null = no category selected, table is empty

    public void setCategory(Category category) {
        this.category = category;
        fireTableDataChanged();
    }

    public Transaction getTransaction(int row) {
        return category.getTransactions().get(row);
    }

    @Override
    public int getRowCount() {
        return category == null ? 0 : category.getTransactions().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    // The column class lets the table sort dates and amounts correctly
    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 0 -> LocalDate.class;
            case 2 -> BigDecimal.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        Transaction t = getTransaction(row);
        return switch (column) {
            case 0 -> t.getDate();
            case 1 -> t.getDescription();
            case 2 -> t.getAmount();
            default -> null;
        };
    }
}
