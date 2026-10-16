import java.awt.*;import java.lang.instrument.*;import java.util.*;import java.util.List;import javax.swing.*;
/** Opt-in UI driver: selects an existing SEHK history item, only 1810 or 9988. */
public final class SymbolChoice{
 public static void agentmain(String args,Instrumentation ignored)throws Exception{
  String[] p=args.split("\\|",-1);if(p.length!=2||!Set.of("1810","9988").contains(p[0])||!Set.of("tape","depth").contains(p[1]))throw new IllegalArgumentException();
  SwingUtilities.invokeAndWait(()->{
   List<JComboBox<?>> boxes=new ArrayList<>();for(Window w:Window.getWindows())if(w.isShowing())find(w,boxes);
   if(boxes.size()!=2)throw new IllegalStateException("expected exactly two visible tape/depth selectors");
   boxes.sort(Comparator.comparingInt(b->b.getLocationOnScreen().x));JComboBox<?> box=boxes.get(p[1].equals("depth")?0:1);
   Object choice=null;for(int k=0;k<box.getItemCount();k++)if(String.valueOf(box.getItemAt(k)).equals(p[0]+" SEHK")){choice=box.getItemAt(k);break;}
   if(choice==null)throw new IllegalStateException("requested SEHK stock is absent from existing symbol history");
   box.hidePopup();box.setSelectedItem(choice);
  });
 }
 static void find(Component c,List<JComboBox<?>> boxes){if(c.isShowing()&&c instanceof JComboBox<?> b&&c.getClass().getName().equals("jtscomponents.contract.ae"))boxes.add(b);if(c instanceof Container parent)for(Component child:parent.getComponents())find(child,boxes);}
}
