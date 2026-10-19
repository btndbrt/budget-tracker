package btndbrt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JPanel;

/** Pie chart of how much was spent in each category of a period. Hover a slice for details. */
public class PieChartPanel extends JPanel {

    /** More slices than this get hard to compare, so the smallest are grouped as "Other". */
    static final int MAX_SLICES = 6;

    /** One piece of the pie. */
    record Slice(String name, BigDecimal amount, Color color) {
    }

    private List<Slice> slices = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;
    private String currency = "";
    private final List<Arc2D> arcs = new ArrayList<>(); // where each slice was drawn, for the tooltips

    public PieChartPanel() {
        setBackground(ChartColors.SURFACE);
        setToolTipText(""); // turns tooltips on, the text comes from getToolTipText(MouseEvent)
    }

    public void setPeriod(Period period, String currency) {
        this.currency = currency;
        slices = buildSlices(period);
        total = BigDecimal.ZERO;
        for (Slice s : slices) {
            total = total.add(s.amount());
        }
        repaint();
    }

    /**
     * Makes the slices, largest first. Each category keeps its own color (by its position in the
     * list), so colors don't jump around when amounts change.
     */
    static List<Slice> buildSlices(Period period) {
        List<Slice> result = new ArrayList<>();
        BigDecimal other = BigDecimal.ZERO;
        List<Category> categories = period.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            BigDecimal spent = categories.get(i).getSpent();
            if (spent.signum() <= 0) {
                continue; // nothing spent, nothing to show
            }
            if (i < ChartColors.CATEGORIES.length) {
                result.add(new Slice(categories.get(i).getName(), spent, ChartColors.CATEGORIES[i]));
            } else {
                other = other.add(spent); // we only have 8 colors
            }
        }
        result.sort(Comparator.comparing(Slice::amount).reversed());

        int ownSlices = MAX_SLICES - 1; // keep one place for "Other"
        boolean needsOther = other.signum() > 0 || result.size() > MAX_SLICES;
        if (needsOther) {
            while (result.size() > ownSlices) {
                other = other.add(result.remove(result.size() - 1).amount());
            }
            result.add(new Slice("Other", other, ChartColors.OTHER));
        }
        return result;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        arcs.clear();
        if (total.signum() <= 0) {
            g.setColor(ChartColors.MUTED);
            g.drawString("No spending in this period yet.", 0, 20);
            g.dispose();
            return;
        }

        // The card around the chart already has a title and padding
        int top = 4;
        int size = Math.max(50, Math.min(getWidth() / 2 - 16, getHeight() - top - 4));
        int x = 0;

        // Slices go clockwise, starting at 12 o'clock (90 degrees in Java's arc angles)
        double angle = 90;
        for (Slice s : slices) {
            double extent = -360 * s.amount().doubleValue() / total.doubleValue();
            Arc2D arc = new Arc2D.Double(x, top, size, size, angle, extent, Arc2D.PIE);
            g.setColor(s.color());
            g.fill(arc);
            arcs.add(arc);
            angle += extent;
        }
        // Thin background-colored outlines leave a small gap between slices
        g.setColor(ChartColors.SURFACE);
        g.setStroke(new BasicStroke(2));
        for (Arc2D arc : arcs) {
            g.draw(arc);
        }

        // Legend: color, name, amount and percent for every slice
        FontMetrics fm = g.getFontMetrics();
        int legendX = x + size + 24;
        int rowY = top + 8;
        for (Slice s : slices) {
            g.setColor(s.color());
            g.fillRoundRect(legendX, rowY - 11, 12, 12, 4, 4);
            g.setColor(ChartColors.TEXT);
            g.drawString(s.name(), legendX + 20, rowY);
            g.setColor(ChartColors.TEXT_SECONDARY);
            g.drawString(Money.format(s.amount(), currency) + "  (" + percent(s.amount()) + "%)",
                    legendX + 20, rowY + fm.getHeight());
            rowY += fm.getHeight() * 2 + 8;
        }
        g.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = 0; i < arcs.size(); i++) {
            if (arcs.get(i).contains(e.getPoint())) {
                Slice s = slices.get(i);
                return s.name() + ": " + Money.format(s.amount(), currency) + " (" + percent(s.amount()) + "%)";
            }
        }
        return null; // no tooltip outside the pie
    }

    private long percent(BigDecimal amount) {
        return Math.round(100 * amount.doubleValue() / total.doubleValue());
    }
}
