package twstapecolor;
import java.util.*;import javax.swing.*;import javax.swing.table.*;
/** Seeded resets with changing counts, stale native ranges and active selections. */
public final class NativeResetStressTest{
 static JTable table;static feature.tape.ui.aT model;static int checks;
 static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
 @SuppressWarnings({"unchecked","rawtypes"})
 static void rows(int trades,int price){Vector data=model.getDataVector();data.clear();for(int k=0;k<trades;k++)data.add(new Vector<>(Arrays.asList(price+1,100+k,null,null)));if(trades>0)data.add(new Vector<>(Arrays.asList(null,null,price,price+1)));}
 public static void main(String[] args)throws Exception{
  Random random=new Random(16102026);
  SwingUtilities.invokeAndWait(()->{model=new feature.tape.ui.aT(new Object[][]{{101,100,null,null},{null,null,100,101}});table=new feature.tape.ui.aN(model);TapeViewV0318.schedule(table,()->true);});SwingUtilities.invokeAndWait(()->{});
  for(int i=0;i<1000;i++){
   int count=new int[]{0,1,17,50,70}[random.nextInt(5)],kind=random.nextInt(5),price=(i%2==0?100:10000);
   SwingUtilities.invokeAndWait(()->{
    int old=model.getRowCount();if(table.getRowCount()>0)table.setRowSelectionInterval(table.getRowCount()-1,table.getRowCount()-1);
    rows(count,price);
    switch(kind){case 0->model.fireTableRowsUpdated(0,old-1);case 1->model.fireTableRowsUpdated(0,16);case 2->model.fireTableRowsInserted(0,0);case 3->model.fireTableRowsDeleted(0,0);case 4->model.fireTableDataChanged();default->throw new AssertionError();}
   });SwingUtilities.invokeAndWait(()->{});
   SwingUtilities.invokeAndWait(()->{
    check(table.getRowCount()==Math.min(count,50),"native reset did not recover the requested trade count");
    for(int r=0;r<table.getRowCount();r++){check(table.convertRowIndexToModel(r)==r,"stale native mapping after reset");check(TapeViewV0318.print(table,r).direction()==1,"reset reused previous instrument's quote");}
   });
  }
  SwingUtilities.invokeAndWait(()->{TapeViewV0318.restoreAll();check(table.getRowSorter()==null,"stress stop did not restore native view");});
  System.out.println("Native reset stress passed ("+checks+" checks)");
 }
}
