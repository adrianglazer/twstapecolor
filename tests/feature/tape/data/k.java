package feature.tape.data;
import javax.swing.table.AbstractTableModel;
/** TWS-shaped null-cell model: renderers read e(row,-1), not getValueAt. */
public final class k extends AbstractTableModel {
  public Object[] rows;
  public k(Object... rows){this.rows=rows;}
  public Object e(int row,int column){return rows[row];}
  public int getRowCount(){return rows.length;}
  public int getColumnCount(){return 3;}
  public String getColumnName(int col){return new String[]{"Time","Last/Midpoint ","Last Size"}[col];}
  public Object getValueAt(int row,int col){return null;}
}
