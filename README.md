# Budget Tracker

A simple Swing budget tracker. The net budget is made of categories you create; each category holds transactions.
Positive amounts are income, negative amounts are expenses.

Data is saved automatically after every change to `~/budget-tracker.txt` (plain text, tab-separated).

## Requirements

- JDK 21 (check with `java -version`)
- Maven 3.9+ (or use the included `./mvnw`)

## Usage

| Task | Command |
|------|---------|
| Run during development | `mvn -q compile exec:java` |
| Run tests | `mvn test` |
| Build the jar (also runs tests) | `mvn package` |
| Run the built jar | `java -jar target/*.jar` |

## Project structure

```
src/main/java/btndbrt/
├── Main.java                  # entry point: loads data, opens the window
├── Budget.java                # list of categories + net total
├── Category.java              # name + list of transactions + total
├── Transaction.java           # date, description, amount
├── BudgetStorage.java         # save/load the text file
├── BudgetFrame.java           # the main window and all button actions
└── TransactionTableModel.java # connects a category's transactions to the JTable
src/test/java/btndbrt/
└── BudgetTest.java            # totals and save/load tests
```
