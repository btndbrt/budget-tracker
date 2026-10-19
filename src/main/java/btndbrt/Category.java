package btndbrt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A named part of the budget, e.g. "Groceries" with 400.00 to spend.
 * Every transaction in the category is subtracted from that amount.
 */
public class Category {

    private String name;
    private BigDecimal budgetAmount;
    private final List<Transaction> transactions = new ArrayList<>();

    public Category(String name, BigDecimal budgetAmount) {
        this.name = name;
        this.budgetAmount = budgetAmount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** How much money is planned for this category. */
    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /** Sum of all transaction amounts in this category. */
    public BigDecimal getSpent() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            total = total.add(t.getAmount());
        }
        return total;
    }

    /** Budget amount minus what was spent. Negative means over budget. */
    public BigDecimal getRemaining() {
        return budgetAmount.subtract(getSpent());
    }
}
