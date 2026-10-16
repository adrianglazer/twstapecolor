package twstapecolor;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/** Native symbol reset notifications need not follow incremental sorter ranges. */
public final class SymbolSwitchTest {
 static int checks;static feature.tape.ui.aT model;static JTable tape;static int downstream;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
 static Vector<Object> row(Object...values){return new Vector<>(Arrays.asList(values));}
 @SuppressWarnings({"rawtypes","unchecked"})
 static void replaceSilently(Object[][] rows){Vector data=model.getDataVector();data.clear();for(Object[] r:rows)data.add(row(r));}
 public static void main(String[] args)throws Exception{
  SwingUtilities.invokeAndWait(()->{
   model=new feature.tape.ui.aT(new Object[][]{{101,100,null,null},{null,null,100,101}});
   // Registered before JTable, as an earlier member of TWS's linked callback chain.
   model.addTableModelListener(e->downstream++);tape=new feature.tape.ui.aN(model);TapeViewV0319.schedule(tape,()->true);
  });flush();
  for(int cycle=0;cycle<100;cycle++){
   SwingUtilities.invokeAndWait(()->{
    replaceSilently(new Object[0][]);int before=downstream;
    model.fireTableRowsUpdated(0,-1);
    check(downstream==before+1,"empty reset interrupted linked symbol callbacks");
    check(tape.getRowCount()==0,"empty reset left stale sorter rows");
    // Native Current notifications can describe an insertion without increasing row count.
    replaceSilently(new Object[][]{{201,100,null,null},{null,null,200,201}});
    model.fireTableRowsInserted(0,1);
   });flush();
   SwingUtilities.invokeAndWait(()->{
    check(tape.getRowCount()==1,"new symbol history never repopulated");
    check(TapeViewV0319.print(tape,0).direction()==1,"new instrument did not get its own preceding quote");
    int before=downstream;model.fireTableRowsInserted(0,0);
    check(downstream==before+1,"duplicate insertion notification interrupted linked windows");
    replaceSilently(new Object[0][]);model.fireTableRowsDeleted(0,1);model.fireTableRowsDeleted(0,0);
   });flush();
   SwingUtilities.invokeAndWait(()->{
    check(tape.getRowCount()==0,"repeated clear left phantom rows");
    replaceSilently(new Object[][]{{300,500,null,null},{null,null,300,301}});
    model.fireTableRowsUpdated(0,16);
   });flush();
   SwingUtilities.invokeAndWait(()->{
    check(tape.getRowCount()==1&&TapeViewV0319.print(tape,0).direction()==-1,"oversized reset range broke live bid rows");
   });
  }
  SwingUtilities.invokeAndWait(()->{
   TapeViewV0319.restoreAll();check(tape.getRowSorter()==null,"stop restores native sorter after repeated switches");
   var old=new LiveDataTest.Event(new LiveDataTest.Instrument(new LiveDataTest.Record(new LiveDataTest.Book(10,11,1,1))),11,100);
   var current=new feature.tape.data.k(old);JTable summary=new JTable(current);TapeViewV0319.schedule(summary,()->true);
   summary.putClientProperty("test.current",current);tape=summary;
  });flush();
  SwingUtilities.invokeAndWait(()->{
   check(tape.getRowSorter()==null,"Current summary must retain native row handling");
   var current=(feature.tape.data.k)tape.getClientProperty("test.current");
   current.fireTableRowsInserted(0,0);current.fireTableRowsInserted(0,0);
   check(tape.getRowCount()==1,"Current duplicated notifications broke summary");TapeViewV0319.restoreAll();
  });
  System.out.println("Symbol-switch tests passed ("+checks+" checks)");
 }
}
