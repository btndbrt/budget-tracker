package btndbrt;

import java.awt.Color;

/**
 * Colors used by the charts. The category colors are a tested color-blind friendly set:
 * keep them in this order, the order is what keeps neighbouring slices distinguishable.
 */
public class ChartColors {

    public static final Color[] CATEGORIES = {
        new Color(0x2a78d6), // blue
        new Color(0xeb6834), // orange
        new Color(0x1baf7a), // aqua
        new Color(0xeda100), // yellow
        new Color(0xe87ba4), // magenta
        new Color(0x008300), // green
        new Color(0x6250d6), // violet
        new Color(0xe34948), // red
    };

    public static final Color OTHER = new Color(0xc3c2b7); // gray, for the "Other" slice

    public static final Color SURFACE = new Color(0xfcfcfb);    // chart background
    public static final Color TEXT = new Color(0x0b0b0b);       // main text
    public static final Color TEXT_SECONDARY = new Color(0x52514e);
    public static final Color MUTED = new Color(0x898781);      // axis labels, reference lines
    public static final Color GRID = new Color(0xe1e0d9);       // thin grid lines
    public static final Color AXIS = new Color(0xc3c2b7);       // the x axis line

    public static final Color CRITICAL = new Color(0xd03b3b);   // over budget, always shown together with text
}
