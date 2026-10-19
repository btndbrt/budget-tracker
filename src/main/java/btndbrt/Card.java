package btndbrt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A white box with rounded corners, a title row at the top and content below.
 * Components added with {@link #addToHeader} appear on the right side of the title row.
 */
public class Card extends JPanel {

    static final int ARC = 12;
    static final Color BORDER = new Color(0xe1e0d9);

    private final JLabel titleLabel = new JLabel();
    private final JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

    public Card(String title) {
        super(new BorderLayout(0, 10));
        setOpaque(false); // we paint the rounded background ourselves
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        titleLabel.setText(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() + 3f));
        headerRight.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void addToHeader(JComponent component) {
        headerRight.add(component);
    }

    /** Puts the main content of the card below the title row. */
    public void setContent(JComponent content) {
        add(content, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ChartColors.SURFACE);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g.setColor(BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g.dispose();
        super.paintComponent(graphics);
    }
}
