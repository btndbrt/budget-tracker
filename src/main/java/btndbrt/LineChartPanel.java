package btndbrt;

import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JPanel;

/**
 * Line chart of the money spent so far in a period, day by day, compared with an even pace that
 * would use up the budget exactly on the last day. Hover the chart to see the numbers of a day.
 */
public class LineChartPanel extends JPanel {

    private static final DateTimeFormatter AXIS_DATE = DateTimeFormatter.ofPattern("MM-dd");
    private static final Stroke DASHED = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
            10, new float[] {6, 4}, 0);

    private Period period;
    private String currency = "";
    private double budgetAmount;
    private double[] spentSoFar = new double[0]; // spentSoFar[i] = total spent up to and including day i
    private int lastDay = -1;  // last day the spent line goes to (today, or the end of a past period)
    private int hoverDay = -1; // day under the mouse, -1 if none

    private final Rectangle plot = new Rectangle(); // the area inside the axes, set while painting

    public LineChartPanel() {
        setBackground(ChartColors.SURFACE);
        setToolTipText(""); // turns tooltips on, the text comes from getToolTipText(MouseEvent)
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverDay = dayAt(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverDay = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /**
     * Shows the spending of the given categories in the period.
     */
    public void setData(Period period, List<Category> categories, String currency) {
        this.period = period;
        this.currency = currency;

        budgetAmount = 0;
        for (Category c : categories) {
            budgetAmount += c.getBudgetAmount().doubleValue();
        }
        spentSoFar = cumulativeSpending(period, categories);

        LocalDate today = LocalDate.now();
        if (today.isBefore(period.getStart())) {
            lastDay = -1; // period has not started, no spending line yet
        } else if (today.isAfter(period.getEnd())) {
            lastDay = period.getDayCount() - 1;
        } else {
            lastDay = period.dayIndex(today);
        }
        repaint();
    }

    /**
     * Adds up the spending day by day. Transactions dated outside the period are left out,
     * because they have no place on the chart.
     */
    static double[] cumulativeSpending(Period period, List<Category> categories) {
        double[] result = new double[period.getDayCount()];
        for (Category c : categories) {
            for (Transaction t : c.getTransactions()) {
                if (period.contains(t.getDate())) {
                    result[period.dayIndex(t.getDate())] += t.getAmount().doubleValue();
                }
            }
        }
        for (int i = 1; i < result.length; i++) {
            result[i] += result[i - 1]; // turn "spent on day i" into "spent until day i"
        }
        return result;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (period == null) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = g.getFontMetrics();

        // The card around the chart has the title, so we start with the legend
        drawLegend(g, 0, 14);

        if (lastDay < 0) {
            g.setColor(ChartColors.MUTED);
            g.drawString("This period has not started yet.", 0, 44);
            g.dispose();
            return;
        }

        // Y axis scale: round numbers, high enough for both the budget and the spending
        double highest = Math.max(1, Math.max(budgetAmount, maxSpent()));
        double step = niceStep(highest / 4);
        double yMax = Math.ceil(highest * 1.05 / step) * step;

        int labelWidth = fm.stringWidth(Money.formatShort(yMax, currency));
        plot.setBounds(labelWidth + 8, 40, 0, 0);
        plot.width = Math.max(10, getWidth() - plot.x - 8);
        plot.height = Math.max(10, getHeight() - plot.y - fm.getHeight() - 8);

        // Horizontal grid lines with amounts
        for (double v = 0; v <= yMax + step / 2; v += step) {
            int y = yFor(v, yMax);
            g.setColor(v == 0 ? ChartColors.AXIS : ChartColors.GRID);
            g.drawLine(plot.x, y, plot.x + plot.width, y);
            g.setColor(ChartColors.MUTED);
            String label = Money.formatShort(v, currency);
            g.drawString(label, plot.x - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
        }

        // Dates along the bottom: first day, every 7th day, last day
        int days = period.getDayCount();
        g.setColor(ChartColors.MUTED);
        for (int i = 0; i < days; i++) {
            boolean last = i == days - 1;
            if (i % 7 == 0 && days - 1 - i >= 3 || last) { // skip a weekly label right before the last one
                String label = period.getStart().plusDays(i).format(AXIS_DATE);
                // Centered under the day, but never past the right edge of the panel
                int x = Math.min(xFor(i) - fm.stringWidth(label) / 2, getWidth() - fm.stringWidth(label));
                g.drawString(label, x, plot.y + plot.height + fm.getHeight() + 4);
            }
        }

        // Budget: a thin horizontal line with a label
        int budgetY = yFor(budgetAmount, yMax);
        g.setColor(ChartColors.MUTED);
        g.drawLine(plot.x, budgetY, plot.x + plot.width, budgetY);
        g.setColor(ChartColors.TEXT_SECONDARY);
        String budgetLabel = "Budget " + Money.formatShort(budgetAmount, currency);
        g.drawString(budgetLabel, plot.x + plot.width - fm.stringWidth(budgetLabel), budgetY - 4);

        // Even pace: dashed line from the start up to the budget on the last day
        g.setColor(ChartColors.MUTED);
        g.setStroke(DASHED);
        g.drawLine(xFor(0), yFor(evenPace(0), yMax), xFor(days - 1), yFor(budgetAmount, yMax));

        // Spent so far: the main line, up to today
        Path2D line = new Path2D.Double();
        for (int i = 0; i <= lastDay; i++) {
            if (i == 0) {
                line.moveTo(xFor(i), yFor(spentSoFar[i], yMax));
            } else {
                line.lineTo(xFor(i), yFor(spentSoFar[i], yMax));
            }
        }
        g.setColor(ChartColors.CATEGORIES[0]);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);

        // Marker and label at the end of the line
        int endX = xFor(lastDay);
        int endY = yFor(spentSoFar[lastDay], yMax);
        drawMarker(g, endX, endY);
        g.setColor(ChartColors.TEXT);
        String endLabel = "Spent " + Money.formatShort(spentSoFar[lastDay], currency);
        int labelX = endX + 10;
        if (labelX + fm.stringWidth(endLabel) > plot.x + plot.width) {
            labelX = endX - 10 - fm.stringWidth(endLabel); // not enough room on the right
        }
        g.drawString(endLabel, labelX, endY - 8);

        // Hover: vertical line and a marker on the hovered day
        if (hoverDay >= 0) {
            int x = xFor(hoverDay);
            g.setColor(ChartColors.AXIS);
            g.setStroke(new BasicStroke(1));
            g.drawLine(x, plot.y, x, plot.y + plot.height);
            if (hoverDay <= lastDay) {
                drawMarker(g, x, yFor(spentSoFar[hoverDay], yMax));
            }
        }
        g.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int day = dayAt(e.getX(), e.getY());
        if (day < 0) {
            return null;
        }
        String text = "<html><b>" + period.getStart().plusDays(day) + "</b><br>";
        if (day <= lastDay) {
            text += "Spent so far: " + Money.formatShort(spentSoFar[day], currency) + "<br>";
        }
        return text + "Even pace: " + Money.formatShort(evenPace(day), currency) + "</html>";
    }

    // ---------- Helpers ----------

    private void drawLegend(Graphics2D g, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(ChartColors.CATEGORIES[0]);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(x, y - 4, x + 20, y - 4);
        g.setColor(ChartColors.TEXT_SECONDARY);
        g.drawString("Spent so far", x + 26, y);

        int x2 = x + 26 + fm.stringWidth("Spent so far") + 24;
        g.setColor(ChartColors.MUTED);
        g.setStroke(DASHED);
        g.drawLine(x2, y - 4, x2 + 20, y - 4);
        g.setColor(ChartColors.TEXT_SECONDARY);
        g.drawString("Even pace to use the budget", x2 + 26, y);
        g.setStroke(new BasicStroke(1));
    }

    /** A filled circle with a background-colored ring, so it stands out on top of lines. */
    private void drawMarker(Graphics2D g, int x, int y) {
        g.setColor(ChartColors.SURFACE);
        g.fillOval(x - 6, y - 6, 12, 12);
        g.setColor(ChartColors.CATEGORIES[0]);
        g.fillOval(x - 4, y - 4, 8, 8);
    }

    /** How much would be spent by the end of the day if the budget were spent evenly. */
    private double evenPace(int day) {
        return budgetAmount * (day + 1) / period.getDayCount();
    }

    private double maxSpent() {
        double max = 0;
        for (int i = 0; i <= lastDay && i < spentSoFar.length; i++) {
            max = Math.max(max, spentSoFar[i]);
        }
        return max;
    }

    private int xFor(int day) {
        int days = period.getDayCount();
        if (days == 1) {
            return plot.x + plot.width / 2;
        }
        return plot.x + (int) Math.round((double) day * plot.width / (days - 1));
    }

    private int yFor(double value, double yMax) {
        return plot.y + plot.height - (int) Math.round(value / yMax * plot.height);
    }

    /** The day at the mouse position, or -1 if the mouse is not over the plot. */
    private int dayAt(int mouseX, int mouseY) {
        if (period == null || lastDay < 0 || !plot.contains(mouseX, mouseY)) {
            return -1;
        }
        int days = period.getDayCount();
        if (days == 1) {
            return 0;
        }
        int day = (int) Math.round((double) (mouseX - plot.x) * (days - 1) / plot.width);
        return Math.max(0, Math.min(days - 1, day));
    }

    /** Rounds a step size to 1, 2 or 5 times a power of ten, so the axis shows round numbers. */
    static double niceStep(double rough) {
        double power = Math.pow(10, Math.floor(Math.log10(rough)));
        double fraction = rough / power;
        if (fraction <= 1) {
            return power;
        } else if (fraction <= 2) {
            return 2 * power;
        } else if (fraction <= 5) {
            return 5 * power;
        }
        return 10 * power;
    }
}
