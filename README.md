# Budget Tracker

A simple Swing budget tracker. You create categories, each with a budget amount (e.g. Groceries: 400).
Transactions in a category are subtracted from its amount, so you see what is left.
The net budget is the sum of all category amounts.

The budget is split into periods (usually months) with a start and end date, picked with a small calendar.
"New period" copies the categories and amounts of the latest period. Old periods stay stored as history
and can be opened from the drop-down at the top.

Data is saved automatically after every change to `~/budget-tracker.txt` (plain text, tab-separated).
The currency (e.g. `HUF`) is typed into the field at the bottom right and is shown after every amount.

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
├── Budget.java                # currency + all periods
├── Period.java                # start/end date, categories, net totals
├── Category.java              # name, budget amount, transactions, remaining
├── Transaction.java           # date, description, amount
├── BudgetStorage.java         # save/load the text file
├── BudgetFrame.java           # the main window and all button actions
├── CalendarDialog.java        # month calendar for picking a date
├── DateField.java             # date text field with a "Pick" button
└── TransactionTableModel.java # connects a category's transactions to the JTable
src/test/java/btndbrt/
└── BudgetTest.java            # totals, periods and save/load tests
```
