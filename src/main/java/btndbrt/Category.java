package btndbrt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A named group of transactions, e.g. "Groceries" or "Salary". */
public class Category {

    private String name;
    private final List<Transaction> transactions = new ArrayList<>();

    public Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /** Sum of all transaction amounts in this category. */
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            total = total.add(t.getAmount());
        }
        return total;
    }
}
