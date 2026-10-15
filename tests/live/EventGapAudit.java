import java.awt.*;import java.awt.image.*;import java.lang.instrument.*;import java.lang.reflect.*;import java.math.*;import java.nio.file.*;import java.util.*;import java.util.List;import javax.swing.*;import javax.swing.event.*;import javax.swing.table.*;import javax.imageio.ImageIO;
/** Forces a paint in the real model-event/deferred-rebuild gap. Bounded, opt-in. */
public final class EventGapAudit {
 static boolean active;static Path root;static Class<?> data,view;static Method read,print;
 static final List<JTable> tables=new ArrayList<>();static final Map<JTable,TableModelListener> listeners=new IdentityHashMap<>();static final Set<JTable> queued=Collections.newSetFromMap(new IdentityHashMap<>());
 static long events,gapSamples,directionalChecks,neutralFlashes,wrongSides,errors;
 static Object get(Object o,String n)throws Exception{Method m=o.getClass().getMethod(n);m.setAccessible(true);return m.invoke(o);}
 static int side(double p,double b,double a){if(!Double.isFinite(p)||!Double.isFinite(b)||!Double.isFinite(a)||p<=0||b<=0||a<=b)return 0;MathContext c=new MathContext(12);BigDecimal x=BigDecimal.valueOf(p).round(c),bid=BigDecimal.valueOf(b).round(c),ask=BigDecimal.valueOf(a).round(c);if(ask.compareTo(bid)<=0)return 0;return x.compareTo(ask)>=0?1:x.compareTo(bid)<=0?-1:0;}
 static void find(Component c){if(c instanceof JTable t&&t.getClass().getName().equals("feature.tape.ui.aN")&&t.getModel().getClass().getName().equals("feature.tape.ui.aT"))tables.add(t);if(c instanceof Container box)for(Component child:box.getComponents())find(child);}
 public static void agentmain(String args,Instrumentation inst)throws Exception{
  if(active)throw new IllegalStateException("audit active");String[] p=args.split("\\|",-1);root=Path.of(p[0]);Files.createDirectories(root);
  data=Class.forName("twstapecolor.LiveTapeData"+p[1]);view=Class.forName("twstapecolor.TapeView"+p[1]);
  read=data.getDeclaredMethod("read",TableModel.class,int.class);read.setAccessible(true);print=view.getDeclaredMethod("print",JTable.class,int.class);print.setAccessible(true);
  events=gapSamples=directionalChecks=neutralFlashes=wrongSides=errors=0;tables.clear();listeners.clear();queued.clear();active=true;
  SwingUtilities.invokeAndWait(()->{
   for(Window w:Window.getWindows())find(w);
   for(JTable t:tables){
    TableModelListener listener=e->{
     if(!active)return;events++;
     if(queued.add(t))EventQueue.invokeLater(()->{queued.remove(t);if(active)sample(t);});
    };
    listeners.put(t,listener);t.getModel().addTableModelListener(listener);
   }
   javax.swing.Timer timer=new javax.swing.Timer(Integer.parseInt(p[2])*1000,e->finish());timer.setRepeats(false);timer.start();
  });
 }
 static void sample(JTable t){
  try{
   Field f=view.getDeclaredField("views");f.setAccessible(true);Object state=((Map<?,?>)f.get(null)).get(t);if(state==null)return;
   Field pending=state.getClass().getDeclaredField("queued");pending.setAccessible(true);if(!pending.getBoolean(state))return;
   gapSamples++;
   for(int row=0;row<t.getRowCount();row++){
    int modelRow=t.convertRowIndexToModel(row);if(modelRow>=t.getModel().getRowCount())continue;
    Object trade=read.invoke(null,t.getModel(),modelRow);if(trade==null)continue;
    double price=((Number)get(trade,"last")).doubleValue(),bid=((Number)get(trade,"bid")).doubleValue(),ask=((Number)get(trade,"ask")).doubleValue();
    int expected=side(price,bid,ask);if(expected==0||t.isRowSelected(row))continue;
    int actual=((Number)get(print.invoke(null,t,row),"direction")).intValue();directionalChecks++;
    if(actual==0)neutralFlashes++;else if(actual!=expected)wrongSides++;
   }
   if(gapSamples<=3){BufferedImage image=new BufferedImage(t.getWidth(),t.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{t.paint(g);}finally{g.dispose();}ImageIO.write(image,"png",root.resolve("gap-"+gapSamples+".png").toFile());}
  }catch(Throwable e){errors++;try{Files.writeString(root.resolve("errors.txt"),e+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}
 }
 static void finish(){active=false;listeners.forEach((t,l)->t.getModel().removeTableModelListener(l));listeners.clear();try{Files.writeString(root.resolve("summary.txt"),"events="+events+"\ngapSamples="+gapSamples+"\ndirectionalChecks="+directionalChecks+"\nneutralFlashes="+neutralFlashes+"\nwrongSides="+wrongSides+"\nerrors="+errors+"\n");}catch(Exception ignored){}finally{tables.clear();queued.clear();}}
}
