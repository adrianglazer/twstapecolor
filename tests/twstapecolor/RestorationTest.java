package twstapecolor;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
public final class RestorationTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
 public static void main(String[] args)throws Exception{
  JTable[] holder={null};int[] listeners={0},properties={0};
  SwingUtilities.invokeAndWait(()->{
   var model=new DefaultTableModel(new Object[][]{{101,1,null,null},{null,null,100,101},{100,2,null,null}},new Object[]{"Last","Size","Bid","Ask"});
   JTable t=new JTable(model);holder[0]=t;t.setRowHeight(23);t.setRowSelectionInterval(2,2);t.setColumnSelectionInterval(1,1);
   t.getColumnModel().moveColumn(1,0);t.getColumnModel().getColumn(0).setPreferredWidth(91);
   listeners[0]=model.getTableModelListeners().length;properties[0]=t.getPropertyChangeListeners().length;
   TapeViewV0319.schedule(t,()->true);
  });flush();
  SwingUtilities.invokeAndWait(()->{
   JTable t=holder[0];check(t.getSelectedRow()==1,"filter preserves selection by model row");
   check(t.convertRowIndexToModel(t.getSelectedRow())==2,"selected event identity preserved");
   TapeViewV0319.restoreAll();
   check(t.getSelectedRow()==2,"stop preserves current selected event");
   check(t.getSelectedColumn()==0,"column selection restored");
   check(t.getRowSorter()==null,"original sorter restored");
   check(t.getRowHeight()==23,"row height preserved");
   check(t.getColumnModel().getColumn(0).getModelIndex()==1&&t.getColumnModel().getColumn(0).getPreferredWidth()==91,"column order and widths preserved");
   check(((DefaultTableModel)t.getModel()).getTableModelListeners().length==listeners[0],"no model listener leak");
   check(t.getPropertyChangeListeners().length==properties[0],"no property listener leak");
  });
  for(int i=0;i<20;i++){
   SwingUtilities.invokeAndWait(()->TapeViewV0319.schedule(holder[0],()->true));flush();
   SwingUtilities.invokeAndWait(TapeViewV0319::restoreAll);
  }
  SwingUtilities.invokeAndWait(()->{
   JTable t=holder[0];check(((DefaultTableModel)t.getModel()).getTableModelListeners().length==listeners[0],"20 restarts have no listener growth");
   check(t.getPropertyChangeListeners().length==properties[0],"20 restarts release property listeners");
  });
  System.out.println("Restoration tests passed ("+checks+" checks)");
 }
}
