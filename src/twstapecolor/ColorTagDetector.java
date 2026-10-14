package twstapecolor;

import javax.swing.JComponent;

/** Exact, fail-closed mapping of the TWS native direction property seen in the live probe. */
final class ColorTagDetector {
  private ColorTagDetector() {}
  static int detect(JComponent renderer) {
    if (renderer == null) return 0;
    Object tag = renderer.getClientProperty("PROPERTY_NAME_COLOR_TAG");
    if (tag == null || !"com.ib.chart.timesales.columns.ColoredColumn$ColorTag".equals(tag.getClass().getName())) return 0;
    // This is a TWS enum; use its enum constant identity, not generic text or color heuristics.
    if (!(tag instanceof Enum<?> e)) return 0;
    return switch (e.name()) {
      case "UP" -> 1;
      case "DOWN" -> -1;
      default -> 0;
    };
  }
}
