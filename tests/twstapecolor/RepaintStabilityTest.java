package twstapecolor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.table.*;
public final class RepaintStabilityTest {
 static int checks;static JTable table;static DefaultTableModel model;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
 public static void main(String[] args)throws Exception{
  SwingUtilities.invokeAndWait(()->{
   model=new DefaultTableModel(new Object[][]{{101,200,null,null},{100,100,null,null},{null,null,100,101}},new Object[]{"Last","Size","Bid","Ask"});
   table=new JTable(model);TapeViewV0319.schedule(table,()->true);
  });flush();
  AtomicInteger sortEvents=new AtomicInteger();
  SwingUtilities.invokeAndWait(()->{
   table.getRowSorter().addRowSorterListener(e->sortEvents.incrementAndGet());
   for(int i=0;i<100;i++){
    model.fireTableRowsUpdated(0,1);
    // This is the paint opportunity before the queued filter refresh runs.
    check(TapeViewV0319.print(table,0).direction()==1,"ask row flashed neutral before deferred refresh");
    check(TapeViewV0319.print(table,1).direction()==-1,"bid row flashed neutral before deferred refresh");
   }
  });flush();
  SwingUtilities.invokeAndWait(()->{
   check(sortEvents.get()==0,"unchanged membership needlessly reset the sorter");
   // Reusing old row indices would avoid the flash but color new rows incorrectly.
   model.setValueAt(100,0,0);model.setValueAt(101,1,0);
   check(TapeViewV0319.print(table,0).direction()==-1,"changed row received stale ask classification");
   check(TapeViewV0319.print(table,1).direction()==1,"changed row received stale bid classification");
  });flush();
  SwingUtilities.invokeAndWait(()->{
   check(TapeViewV0319.print(table,0).direction()==-1,"classification changed after queued refresh");
   model.setValueAt(null,0,1);
   check(TapeViewV0319.print(table,0).direction()==0,"incomplete row kept a stale tint");
  });flush();
  SwingUtilities.invokeAndWait(()->{
   check(table.getRowCount()==1,"membership changes still update the filter");
   check(TapeViewV0319.print(table,0).direction()==1,"filtered view no longer maps to its model row");
   TapeViewV0319.restoreAll();
   check(table.getRowSorter()==null&&table.getRowCount()==3,"stop restores native rows");
  });
  System.out.println("Repaint stability passed ("+checks+" checks)");
 }
}
