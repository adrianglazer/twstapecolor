package twstapecolor;

import java.awt.EventQueue;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

/** EDT-owned view of a newest-first event model. Quote rows remain in the model. */
final class TapeViewV0316 {
  static final int LIMIT=50;
  record Print(int direction,double intensity) {}
  private static final Print UNKNOWN=new Print(0,0);
  private static final String PENDING=TapeViewV0316.class.getName()+".pending";
  private static final Map<JTable,TapeViewV0316> views=new IdentityHashMap<>();
  private static final Pattern PAIR=Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:[xX×/]|\\s+-\\s+)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$");
  private final JTable table;
  private final TableModel model;
  private final TableRowSorter<TableModel> sorter;
  private final BooleanSupplier active;
  private Map<Integer,Print> prints=Map.of();
  private Set<Integer> included=Set.of();
  private final Map<TableCellRenderer,TableCellRenderer> renderers=new IdentityHashMap<>();
  private boolean dirty=true,queued,closed;
  private final HierarchyListener disposal=e->{
    if((e.getChangeFlags()&HierarchyEvent.DISPLAYABILITY_CHANGED)!=0&&!e.getComponent().isDisplayable())close();
  };
  private final TableModelListener changes=e->invalidate();
  private final PropertyChangeListener replacement=e->{
    if(e.getPropertyName().equals("model")||e.getPropertyName().equals("rowSorter"))close();
  };

  static void schedule(JTable table,BooleanSupplier active){
    if(!EventQueue.isDispatchThread()||views.containsKey(table)||table.getClientProperty(PENDING)!=null)return;
    boolean displayed=table.isDisplayable();
    table.putClientProperty(PENDING,Boolean.TRUE);
    EventQueue.invokeLater(()->{
      table.putClientProperty(PENDING,null);
      // Preserve an application-owned sorter rather than overriding its behavior.
      if((displayed&&!table.isDisplayable())||!active.getAsBoolean()||views.containsKey(table)||table.getRowSorter()!=null||(!LiveTapeDataV0316.supports(table.getModel())&&columns(table.getModel())==null))return;
      TapeViewV0316 view=new TapeViewV0316(table,active);
      views.put(table,view);
    });
  }
  private TapeViewV0316(JTable table,BooleanSupplier active){
    this.table=table;this.model=table.getModel();this.active=active;
    sorter=new TableRowSorter<>(model);
    for(int i=0;i<model.getColumnCount();i++)sorter.setSortable(i,false);
    rebuild();
    Selection selection=Selection.capture(table);
    table.setRowSorter(sorter);
    selection.restore(table);
    model.addTableModelListener(changes);
    table.addPropertyChangeListener(replacement);
    table.addHierarchyListener(disposal);
  }
  private void invalidate(){
    // Refresh from the current model before any paint, without changing the
    // sorter inside rendering or exposing an empty cache between EDT tasks.
    dirty=true;
    if(queued||closed)return;
    queued=true;
    EventQueue.invokeLater(()->{
      queued=false;
      if(!closed&&active.getAsBoolean())rebuild();
    });
  }
  private void refresh(){
    if(dirty){prints=snapshot(model);dirty=false;}
  }
  private void rebuild(){
    refresh();
    Set<Integer> next=Set.copyOf(prints.keySet());
    if(sorter.getRowFilter()==null||!included.equals(next)){
      included=next;
      sorter.setRowFilter(new RowFilter<TableModel,Integer>(){
        @Override public boolean include(Entry<? extends TableModel,? extends Integer> entry){return next.contains(entry.getIdentifier());}
      });
    }
    table.repaint();
  }
  static Print print(JTable table,int viewRow){
    TapeViewV0316 view=views.get(table);
    if(view==null||viewRow<0||viewRow>=table.getRowCount())return UNKNOWN;
    view.refresh();
    return view.prints.getOrDefault(table.convertRowIndexToModel(viewRow),UNKNOWN);
  }
  private void close(){
    if(closed)return;
    closed=true;
    table.removePropertyChangeListener(replacement);
    table.removeHierarchyListener(disposal);
    renderers.clear();
    model.removeTableModelListener(changes);
    views.remove(table);
    if(table.getRowSorter()==sorter){
      Selection selection=Selection.capture(table);
      table.setRowSorter(null);
      selection.restore(table);
    }
    prints=Map.of();
    table.repaint();
  }
  private record Selection(int[] rows,int[] columns,int rowAnchor,int rowLead,int columnAnchor,int columnLead){
    static int modelRow(JTable table,int row){return row>=0&&row<table.getRowCount()?table.convertRowIndexToModel(row):-1;}
    static Selection capture(JTable table){
      return new Selection(java.util.Arrays.stream(table.getSelectedRows()).map(row->modelRow(table,row)).toArray(),table.getSelectedColumns(),
        modelRow(table,table.getSelectionModel().getAnchorSelectionIndex()),modelRow(table,table.getSelectionModel().getLeadSelectionIndex()),
        table.getColumnModel().getSelectionModel().getAnchorSelectionIndex(),table.getColumnModel().getSelectionModel().getLeadSelectionIndex());
    }
    void restore(JTable table){
      var rowSelection=table.getSelectionModel();var columnSelection=table.getColumnModel().getSelectionModel();
      boolean rowsAdjusting=rowSelection.getValueIsAdjusting(),columnsAdjusting=columnSelection.getValueIsAdjusting();
      rowSelection.setValueIsAdjusting(true);columnSelection.setValueIsAdjusting(true);
      try{
        rowSelection.clearSelection();columnSelection.clearSelection();
        for(int row:rows)if(row>=0&&row<table.getModel().getRowCount()){
          int view=table.convertRowIndexToView(row);if(view>=0)rowSelection.addSelectionInterval(view,view);
        }
        for(int col:columns)if(col>=0&&col<table.getColumnCount())columnSelection.addSelectionInterval(col,col);
        int anchor=rowAnchor>=0&&rowAnchor<table.getModel().getRowCount()?table.convertRowIndexToView(rowAnchor):-1;
        int lead=rowLead>=0&&rowLead<table.getModel().getRowCount()?table.convertRowIndexToView(rowLead):-1;
        rowSelection.setAnchorSelectionIndex(anchor);columnSelection.setAnchorSelectionIndex(columnAnchor);
        if(rowSelection instanceof DefaultListSelectionModel d)d.moveLeadSelectionIndex(lead);
        if(columnSelection instanceof DefaultListSelectionModel d)d.moveLeadSelectionIndex(columnLead);
      }finally{rowSelection.setValueIsAdjusting(rowsAdjusting);columnSelection.setValueIsAdjusting(columnsAdjusting);}
    }
  }
  static void restoreAll(){for(TapeViewV0316 view:List.copyOf(views.values()))view.close();}

  static TableCellRenderer renderer(JTable table,TableCellRenderer delegate){
    TapeViewV0316 view=views.get(table);
    if(view==null)return new HookV0316.TapeRenderer(delegate,table);
    TableCellRenderer cached=view.renderers.get(delegate);
    if(cached==null){
      if(view.renderers.size()>=64)view.renderers.clear();
      cached=new HookV0316.TapeRenderer(delegate,table);view.renderers.put(delegate,cached);
    }
    return cached;
  }
  static double intensity(double size,double largest){
    return .18+.82*Math.sqrt(Math.clamp(size/largest,0,1));
  }
  private static String name(String text){return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
  // Read model columns, including columns hidden in the table view.
  private static int[] columns(TableModel model){
    int last=-1,size=-1,bid=-1,ask=-1,nbbo=-1;
    for(int c=0;c<model.getColumnCount();c++)switch(name(model.getColumnName(c))){
      case "last","lastmidpoint" -> last=c;
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
      return Double.isFinite(n)&&n>0&&n<Double.MAX_VALUE?n:Double.NaN;
    }catch(NumberFormatException ex){return Double.NaN;}
  }
  static int classify(double last,double bid,double ask){
    if(!Double.isFinite(last)||!Double.isFinite(bid)||!Double.isFinite(ask)||last<=0||bid<=0||last==Double.MAX_VALUE||bid==Double.MAX_VALUE||ask==Double.MAX_VALUE||ask<=bid)return 0;
    // TWS can encode a decimal quote as 2.3600000000000003 while the trade
    // arrives as 2.36. Allow only a few floating-point ULPs, not a price tick.
    double epsilon=4*Math.ulp(Math.max(last,Math.max(bid,ask)));
    return last>=ask-epsilon?1:last<=bid+epsilon?-1:0;
  }
  static Map<Integer,Print> snapshot(TableModel model){
    if(LiveTapeDataV0316.supports(model))return nativeSnapshot(model);
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
        double intensity=intensity(size,largest);
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
  private static Map<Integer,Print> nativeSnapshot(TableModel model){
    Map<Integer,LiveTapeDataV0316.Trade> trades=new LinkedHashMap<>();
    double largest=0;
    for(int row=0;row<model.getRowCount()&&trades.size()<LIMIT;row++){
      LiveTapeDataV0316.Trade trade=LiveTapeDataV0316.read(model,row);
      if(trade!=null&&Double.isFinite(trade.last())&&Double.isFinite(trade.size())){
        trades.put(row,trade);largest=Math.max(largest,trade.size());
      }
    }
    Map<Integer,Print> result=new HashMap<>();
    for(var entry:trades.entrySet()){
      var trade=entry.getValue();
      result.put(entry.getKey(),new Print(classify(trade.last(),trade.bid(),trade.ask()),intensity(trade.size(),largest)));
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
