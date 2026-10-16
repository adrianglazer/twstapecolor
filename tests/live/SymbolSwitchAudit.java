import java.awt.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/** Measures a UI-driven symbol switch; never chooses instruments or sends orders. */
public final class SymbolSwitchAudit {
 static boolean active;static Path root;static String symbol,version;static JTable history,current;static final List<JComboBox<?>> headers=new ArrayList<>();
 static int samples,readySamples,headerMismatches,historyMismatches,historyAdvances,errors,transitionSamples,transitionalHistoryRows,settledSample;static boolean ready;static Object previous;
 static final Set<Object> captured=Collections.newSetFromMap(new IdentityHashMap<>());static StringBuilder rows;
 static Object get(Object o,String name,Object...args)throws Exception{for(Method m:o.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(o,args);}throw new NoSuchMethodException(name);}
 static void find(Component c){
  if(c.isShowing()){
   if(c instanceof JComboBox<?> b&&c.getClass().getName().equals("jtscomponents.contract.ae"))headers.add(b);
   if(c instanceof JTable t&&t.getClass().getName().equals("feature.tape.ui.aN")){if(t.getModel().getClass().getName().equals("feature.tape.ui.aT"))history=t;else if(t.getModel().getClass().getName().equals("feature.tape.data.k"))current=t;}
  }
  if(c instanceof Container box)for(Component child:box.getComponents())find(child);
 }
 static String symbol(Object event)throws Exception{if(event==null)return "unknown";Object instrument=get(event,"ar");if(instrument==null)return "unknown";String value=instrument.toString();var m=java.util.regex.Pattern.compile("(?:^|\\s)Symbol=([^\\s]+)").matcher(value);return m.find()?m.group(1):"unknown";}
 public static void agentmain(String args,Instrumentation ignored)throws Exception{
  if(active)throw new IllegalStateException("audit active");String[] p=args.split("\\|",-1);root=Path.of(p[0]);Files.createDirectories(root);symbol=p[1];version=p[2];int seconds=Integer.parseInt(p[3]);
  if(!Set.of("1810","9988","XLAB","WETO","TCRT").contains(symbol)||seconds<1||seconds>300)throw new IllegalArgumentException();
  active=true;samples=readySamples=headerMismatches=historyMismatches=historyAdvances=errors=transitionSamples=transitionalHistoryRows=settledSample=0;ready=false;previous=null;headers.clear();captured.clear();history=current=null;rows=new StringBuilder();
  SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())if(w.isShowing())find(w);javax.swing.Timer timer=new javax.swing.Timer(250,e->{sample();if(samples>=seconds*4||(readySamples>=8&&historyAdvances>=1&&captured.size()>=2)){((javax.swing.Timer)e.getSource()).stop();finish();}});timer.start();});
 }
 static void sample(){samples++;try{
  if(history==null||current==null||headers.size()<2)throw new IllegalStateException("missing native tape/depth panels");
  boolean matching=headers.stream().allMatch(h->(String.valueOf(h.getSelectedItem()).equals(symbol)||String.valueOf(h.getSelectedItem()).startsWith(symbol+" ")));
  int wrongHistory=0;for(int r=0;r<history.getModel().getRowCount();r++)if(!symbol.equals(symbol(get(history.getModel(),"e",r,-1))))wrongHistory++;
  boolean currentMatches=current.getModel().getRowCount()>0&&symbol.equals(symbol(get(current.getModel(),"e",0,-1)));
  // Combo captions can change before TWS has completed its asynchronous native
  // model transfer. Keep that transition visible in evidence; verify settled data.
  if(!ready){
    if(matching&&currentMatches&&wrongHistory==0){ready=true;settledSample=samples;}
    else{transitionSamples++;transitionalHistoryRows+=wrongHistory;return;}
  }
  readySamples++;if(!matching)headerMismatches++;
  Class<?> data=Class.forName("twstapecolor.LiveTapeData"+version);Field field=data.getDeclaredField("trades");field.setAccessible(true);Map<?,?> live=(Map<?,?>)field.get(null);
  for(int r=0;r<history.getModel().getRowCount();r++){
   Object event=get(history.getModel(),"e",r,-1);String actual=symbol(event);if(!symbol.equals(actual))historyMismatches++;
   synchronized(live){if(live.containsKey(event)&&symbol.equals(actual))captured.add(event);}
  }
  Object top=history.getModel().getRowCount()>0?get(history.getModel(),"e",0,-1):null;
  if(top!=null&&previous!=null&&top!=previous)historyAdvances++;if(top!=null)previous=top;
  if(current.getRowSorter()!=null)throw new IllegalStateException("Current summary acquired a sorter");
  if(history.getRowCount()>50)throw new IllegalStateException("history exceeds 50 visible trades");
  for(int r=0;r<history.getRowCount();r++){int modelRow=history.convertRowIndexToModel(r);if(modelRow<0||modelRow>=history.getModel().getRowCount())throw new IllegalStateException("invalid native row mapping");}
  rows.append(samples+","+history.getModel().getRowCount()+","+history.getRowCount()+","+current.getRowCount()+","+historyAdvances+","+captured.size()+","+matching+"\n");
 }catch(Throwable e){errors++;try{Files.writeString(root.resolve("errors.txt"),e+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}}
 static void finish(){active=false;try{
  Files.writeString(root.resolve("samples.csv"),"sample,rawHistory,visibleHistory,currentRows,historyAdvances,capturedLiveTrades,headersMatch\n"+rows);
  Files.writeString(root.resolve("summary.txt"),"symbol="+symbol+"\nsamples="+samples+"\ntransitionSamples="+transitionSamples+"\ntransitionalHistoryRows="+transitionalHistoryRows+"\nsettledAtMillis="+(settledSample*250)+"\nreadySamples="+readySamples+"\nheaderMismatches="+headerMismatches+"\nhistoryMismatches="+historyMismatches+"\nhistoryAdvances="+historyAdvances+"\ncapturedLiveTrades="+captured.size()+"\nfinalRawHistory="+(history==null?-1:history.getModel().getRowCount())+"\nfinalVisibleHistory="+(history==null?-1:history.getRowCount())+"\nerrors="+errors+"\n");
 }catch(Exception ignored){}finally{headers.clear();captured.clear();history=current=null;previous=null;rows=null;}}
}
