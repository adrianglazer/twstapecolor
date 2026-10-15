package jtable.basic;
import javax.swing.*;
import javax.swing.table.*;
public class by extends JTable {
  public by(TableModel model){super(model);}
  @Override public TableCellRenderer getCellRenderer(int row,int col){return super.getCellRenderer(row,col);}
}
