package twstapecolor;
import javax.swing.JLabel;
import com.ib.chart.timesales.columns.ColoredColumn.ColorTag;
public final class ColorTagDetectorTest {
  static void expect(int expected, JLabel l) {
    if(ColorTagDetector.detect(l)!=expected) throw new AssertionError("expected="+expected);
  }
  public static void main(String[] args) {
    JLabel l=new JLabel();
    expect(0,l);
    l.putClientProperty("PROPERTY_NAME_COLOR_TAG",ColorTag.UP);expect(1,l);
    l.putClientProperty("PROPERTY_NAME_COLOR_TAG",ColorTag.DOWN);expect(-1,l);
    l.putClientProperty("PROPERTY_NAME_COLOR_TAG",ColorTag.NO_CHANGE);expect(0,l);
    l.putClientProperty("PROPERTY_NAME_COLOR_TAG","UP");expect(0,l);
    l.putClientProperty("PROPERTY_NAME_COLOR_TAG",null);expect(0,l);
    System.out.println("ColorTagDetector tests passed (6 checks)");
  }
}
