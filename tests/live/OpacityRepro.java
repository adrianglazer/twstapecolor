import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.table.*;
/** Separate-JVM reproduction of context-dependent native renderer opacity. */
public final class OpacityRepro {
 public static void main(String[] args)throws Exception{SwingUtilities.invokeAndWait(()->{try{
  JTable nativeOwner=new JTable();nativeOwner.setBackground(Color.BLACK);nativeOwner.setOpaque(true);
  CellRendererPane pane=new CellRendererPane();nativeOwner.add(pane);
  var renderer=new DefaultTableCellRenderer();renderer.setBackground(Color.BLACK);renderer.setOpaque(true);pane.add(renderer);
  boolean initial=renderer.isOpaque();var constructor=Class.forName("twstapecolor."+args[0]).getDeclaredConstructor(Component.class,Color.class,double.class);constructor.setAccessible(true);
  Component wrapper=(Component)constructor.newInstance(renderer,Color.GREEN,1);wrapper.setSize(120,24);
  var image=new BufferedImage(120,24,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{wrapper.paint(g);}finally{g.dispose();}
  renderer.setBackground(Color.ORANGE);
  System.out.println("matchingTableOpaque="+initial+" laterDepthOpaque="+renderer.isOpaque()+" parentPreserved="+(renderer.getParent()==pane));
 }catch(Exception e){throw new RuntimeException(e);}});}
}
