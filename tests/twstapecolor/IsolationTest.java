package twstapecolor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/** Shared native renderer and foreign-table reuse regressions. */
public final class IsolationTest {
 static int checks;static JTable tape;static TableCellRenderer wrapped;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
 static void paint(Component component){
  component.setSize(120,24);var image=new BufferedImage(120,24,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{component.paint(g);}finally{g.dispose();}
 }
 public static void main(String[] args)throws Exception{
  Field enabled=HookV0318.class.getDeclaredField("enabled");enabled.setAccessible(true);enabled.setBoolean(null,true);
  SwingUtilities.invokeAndWait(()->{
   tape=new feature.tape.ui.aN(new feature.tape.ui.aT(new Object[][]{{101,100,null,null},{null,null,100,101},{100,1000,null,null},{null,null,100,101}}));
   var nativeRenderer=new DefaultTableCellRenderer();nativeRenderer.setBackground(Color.BLACK);
   wrapped=HookV0318.wrap(nativeRenderer,tape);
  });flush();
  SwingUtilities.invokeAndWait(()->{
   final int[] foreignRow={-1};
   var depth=new JTable(new DefaultTableModel(new Object[][]{{"L2"}},new Object[]{"Depth"}));
   depth.setBackground(Color.ORANGE);var nativeDepth=new JLabel("L2");nativeDepth.setBackground(Color.ORANGE);
   TableCellRenderer delegate=(t,v,s,f,r,c)->{foreignRow[0]=r;return nativeDepth;};
   var cached=HookV0318.wrap(delegate,tape);
   check(cached==HookV0318.wrap(delegate,tape),"tape renderer wrapper is cached");
   check(cached.getTableCellRendererComponent(depth,"L2",false,false,0,0)==nativeDepth,"cached wrapper bypasses Level 2");
   check(foreignRow[0]==0,"foreign table row remains untranslated");
   check(HookV0318.wrap(cached,depth)==delegate,"foreign selector unwraps cached tape wrapper");
   var otherTape=new feature.tape.ui.aN(new feature.tape.ui.aT(new Object[][]{{101,1,null,null}}));
   check(cached.getTableCellRendererComponent(otherTape,101,false,false,0,0)==nativeDepth,"wrapper never crosses tape owners");
   var shared=new JLabel("WHITE TEXT");shared.setOpaque(true);shared.setBackground(Color.BLACK);shared.setForeground(Color.WHITE);shared.setBounds(5,6,70,18);
   var level2=new JLabel("depth");level2.setBackground(Color.CYAN);
   int[] changes={0};shared.addPropertyChangeListener(e->{if(e.getPropertyName().equals("background")||e.getPropertyName().equals("opaque")){changes[0]++;level2.setBackground(Color.BLACK);}});
   CellRendererPane parent=new CellRendererPane();parent.add(shared);int[] hierarchy={0};shared.addHierarchyListener(e->hierarchy[0]++);
   Rectangle bounds=shared.getBounds();
   paint(new TintRendererV0318(shared,new Color(22,131,74),.6));
   check(changes[0]==0,"painting never writes native background or opacity");
   check(level2.getBackground().equals(Color.CYAN),"native property observers cannot erase Level 2 color");
   check(hierarchy[0]==0&&shared.getParent()==parent,"paint does not reparent native renderer");
   check(shared.getBounds().equals(bounds),"native bounds restored");
   // DefaultTableCellRenderer.isOpaque() is context-dependent. Saving that
   // answer and writing it back with setOpaque(false) corrupts its real flag.
   var nativeOwner=new JTable();nativeOwner.setBackground(Color.BLACK);nativeOwner.setOpaque(true);
   var nativePane=new CellRendererPane();nativeOwner.add(nativePane);
   var contextual=new DefaultTableCellRenderer();contextual.setBackground(Color.BLACK);contextual.setOpaque(true);nativePane.add(contextual);
   check(!contextual.isOpaque(),"native renderer suppresses redundant background in matching parent");
   paint(new TintRendererV0318(contextual,Color.GREEN,1));
   contextual.setBackground(Color.ORANGE);
   check(contextual.isOpaque(),"native opacity survives a later Level 2 background");
   var colored=HookV0318.wrap((t,v,s,f,r,c)->shared,tape);
   Component first=colored.getTableCellRendererComponent(tape,101,false,false,0,0);
   paint(first);
   Component second=colored.getTableCellRendererComponent(tape,101,false,false,0,0);
   check(first==second,"tinted component reused rather than allocated per cell");
   for(int i=0;i<1000;i++){final int id=i;HookV0318.wrap((t,v,s,f,r,c)->new JLabel(""+id),tape);}
   try{
    Field views=TapeViewV0318.class.getDeclaredField("views");views.setAccessible(true);Object state=((Map<?,?>)views.get(null)).get(tape);
    Field cache=state.getClass().getDeclaredField("renderers");cache.setAccessible(true);
    check(((Map<?,?>)cache.get(state)).size()<=64,"renderer cache remains bounded under changing delegates");
   }catch(ReflectiveOperationException e){throw new AssertionError(e);}
   TapeViewV0318.restoreAll();
   check(tape.getRowSorter()==null,"stop releases private sorter");
   check(colored.getTableCellRendererComponent(depth,"L2",false,false,0,0)==shared,"stored wrapper still bypasses foreign table after stop");
   // Exercise the actual displayability lifecycle, including an explicit close.
   tape.addNotify();TapeViewV0318.schedule(tape,()->true);
  });flush();
  SwingUtilities.invokeAndWait(()->{
   check(tape.getRowSorter()!=null,"displayable tape receives view");
   tape.removeNotify();check(tape.getRowSorter()==null,"closing tape releases view without stop");
   try{Field views=TapeViewV0318.class.getDeclaredField("views");views.setAccessible(true);check(((Map<?,?>)views.get(null)).isEmpty(),"closed tape not retained globally");}catch(Exception e){throw new AssertionError(e);}
  });
  for(int i=0;i<100;i++){
   SwingUtilities.invokeAndWait(()->{
    var closing=new feature.tape.ui.aN(new feature.tape.ui.aT(new Object[][]{{101,1,null,null}}));
    closing.addNotify();TapeViewV0318.schedule(closing,()->true);closing.removeNotify();
   });flush();
  }
  SwingUtilities.invokeAndWait(()->{
   try{Field views=TapeViewV0318.class.getDeclaredField("views");views.setAccessible(true);check(((Map<?,?>)views.get(null)).isEmpty(),"100 rapid closes cannot install orphaned pending views");}catch(Exception e){throw new AssertionError(e);}
  });
  enabled.setBoolean(null,false);
  System.out.println("Isolation tests passed ("+checks+" checks)");
 }
}
