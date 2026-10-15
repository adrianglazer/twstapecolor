package twstapecolor;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

/** EDT-owned view of a newest-first event model. Quote rows remain in the model. */
final class TapeView {
  static final int LIMIT=50;
  record Print(int direction,double intensity) {}
  private static final Print UNKNOWN=new Print(0,0);
  private static final String PENDING=TapeView.class.getName()+".pending";
  private static final Map<JTable,TapeView> views=new IdentityHashMap<>();
  private static final Pattern PAIR=Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:[xX×/]|\\s+-\\s+)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$");
  private final JTable table;
  private final TableModel model;
  private final TableRowSorter<TableModel> sorter;
  private final BooleanSupplier active;
  private Map<Integer,Print> prints=Map.of();
  private boolean queued,closed;
  private final TableModelListener changes=e->invalidate();
  private final PropertyChangeListener replacement=e->{
    if(e.getPropertyName().equals("model")||e.getPropertyName().equals("rowSorter"))close();
  };

  static void schedule(JTable table,BooleanSupplier active){
    if(!EventQueue.isDispatchThread()||views.containsKey(table)||table.getClientProperty(PENDING)!=null)return;
    table.putClientProperty(PENDING,Boolean.TRUE);
    EventQueue.invokeLater(()->{
      table.putClientProperty(PENDING,null);
      // Preserve an application-owned sorter rather than overriding its behavior.
      if(!active.getAsBoolean()||views.containsKey(table)||table.getRowSorter()!=null||columns(table.getModel())==null)return;
      TapeView view=new TapeView(table,active);
      views.put(table,view);
    });
  }
  private TapeView(JTable table,BooleanSupplier active){
    this.table=table;this.model=table.getModel();this.active=active;
    sorter=new TableRowSorter<>(model);
    for(int i=0;i<model.getColumnCount();i++)sorter.setSortable(i,false);
    rebuild();
    table.setRowSorter(sorter);
    model.addTableModelListener(changes);
    table.addPropertyChangeListener(replacement);
  }
  private void invalidate(){
    // Never use a pre-insert row index for a later event.
    prints=Map.of();
    if(queued||closed)return;
    queued=true;
    EventQueue.invokeLater(()->{
      queued=false;
      if(!closed&&active.getAsBoolean())rebuild();
    });
  }
  private void rebuild(){
    Map<Integer,Print> next=snapshot(model);
    prints=next;
    sorter.setRowFilter(new RowFilter<TableModel,Integer>(){
      @Override public boolean include(Entry<? extends TableModel,? extends Integer> entry){return next.containsKey(entry.getIdentifier());}
    });
    table.repaint();
  }
  static Print print(JTable table,int viewRow){
    TapeView view=views.get(table);
    if(view==null||viewRow<0||viewRow>=table.getRowCount())return UNKNOWN;
    return view.prints.getOrDefault(table.convertRowIndexToModel(viewRow),UNKNOWN);
  }
  private void close(){
    if(closed)return;
    closed=true;
    table.removePropertyChangeListener(replacement);
    model.removeTableModelListener(changes);
    views.remove(table);
    if(table.getRowSorter()==sorter)table.setRowSorter(null);
    prints=Map.of();
    table.repaint();
  }
  static void restoreAll(){for(TapeView view:List.copyOf(views.values()))view.close();}

  private static String name(String text){return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
  // Read model columns, including columns hidden in the table view.
  private static int[] columns(TableModel model){
    int last=-1,size=-1,bid=-1,ask=-1,nbbo=-1;
    for(int c=0;c<model.getColumnCount();c++)switch(name(model.getColumnName(c))){
      case "last" -> last=c;
      case "lastsize" -> size=c;
      case "size" -> {if(size<0)size=c;}
      case "bid","nbbobid" -> bid=c;
      case "ask","nbboask" -> ask=c;
      case "nbbo" -> nbbo=c;
    }
    return last<0||size<0?null:new int[]{last,size,bid,ask,nbbo};
  }
  static double number(Object value){
    if(value==null)return Double.NaN;
    try {
      double n=value instanceof Number num?num.doubleValue():Double.parseDouble(value.toString().trim().replace(",",""));
      return Double.isFinite(n)&&n>0?n:Double.NaN;
    }catch(NumberFormatException ex){return Double.NaN;}
  }
  static int classify(double last,double bid,double ask){
    if(!Double.isFinite(last)||!Double.isFinite(bid)||!Double.isFinite(ask)||last<=0||bid<=0||ask<=bid)return 0;
    return last>=ask?1:last<=bid?-1:0;
  }
  static Map<Integer,Print> snapshot(TableModel model){
    int[] c=columns(model);
    if(c==null)return Map.of();
    // Pick trades before scanning quotes; hidden quote events never consume this budget.
    List<Integer> trades=new ArrayList<>();
    double largest=0;
    for(int row=0;row<model.getRowCount()&&trades.size()<LIMIT;row++){
      double last=number(model.getValueAt(row,c[0])),size=number(model.getValueAt(row,c[1]));
      if(Double.isFinite(last)&&Double.isFinite(size)){
        trades.add(row);largest=Math.max(largest,size);
      }
    }
    if(trades.isEmpty())return Map.of();
    Set<Integer> selected=new HashSet<>(trades);
    Map<Integer,Print> result=new HashMap<>();
    double bid=Double.NaN,ask=Double.NaN;
    // Only inspect enough history to find the quote preceding the oldest visible
    // trade. The model may retain thousands of older events.
    int boundary=trades.get(trades.size()-1)+1;
    while(boundary<model.getRowCount()&&!hasQuote(model,boundary,c))boundary++;
    boundary=Math.min(boundary,model.getRowCount()-1);
    for(int row=boundary;row>=0;row--){
      if(selected.contains(row)){
        double last=number(model.getValueAt(row,c[0])),size=number(model.getValueAt(row,c[1]));
        double intensity=.35+.65*Math.log1p(size)/Math.log1p(largest);
        result.put(row,new Print(classify(last,bid,ask),intensity));
      }
      // The current event cannot be the preceding quote for its own trade.
      // A quote-bearing event replaces the snapshot atomically. Invalid/partial
      // quotes invalidate it, avoiding classification against stale market data.
      Object b=c[2]<0?null:model.getValueAt(row,c[2]);
      Object a=c[3]<0?null:model.getValueAt(row,c[3]);
      Object pair=c[4]<0?null:model.getValueAt(row,c[4]);
      if(present(b)||present(a)) {bid=number(b);ask=number(a);}
      else if(present(pair)){
        Matcher m=PAIR.matcher(pair.toString());
        bid=m.matches()?number(m.group(1)):Double.NaN;
        ask=m.matches()?number(m.group(2)):Double.NaN;
      }
    }
    return Map.copyOf(result);
  }
  private static boolean hasQuote(TableModel model,int row,int[] c){
    return (c[2]>=0&&present(model.getValueAt(row,c[2])))||
      (c[3]>=0&&present(model.getValueAt(row,c[3])))||
      (c[4]>=0&&present(model.getValueAt(row,c[4])));
  }
  private static boolean present(Object value){return value!=null&&!value.toString().isBlank();}
}
