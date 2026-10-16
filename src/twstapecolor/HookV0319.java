package twstapecolor;
import java.awt.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.table.*;
import org.objectweb.asm.*;

/** Injects one method-exit hook into TWS's generic renderer selector; fails closed for non-tape tables. */
public final class HookV0319 {
  private static final String TARGET="jtable/basic/by";
  private static final Set<String> MODELS=Set.of("feature.tape.ui.aT","feature.tape.data.k");
  private static volatile Color askColor=new Color(22,131,74), bidColor=new Color(184,59,75);
  private static volatile boolean enabled, visualTest, probeMode;
  private static final int MAX_SAMPLES=80;
  private static final java.util.concurrent.ConcurrentLinkedQueue<String> samples=new java.util.concurrent.ConcurrentLinkedQueue<>();
  private static final AtomicLong probeSeen=new AtomicLong(), probeStored=new AtomicLong();
  private static volatile boolean injected;
  private static Class<?> targetClass;
  private static Instrumentation instrumentation;
  private static ClassFileTransformer transformer;
  private static final AtomicReference<Throwable> transformFailure=new AtomicReference<>();
  // Guard against a delegate reentering the rendering hook.
  private static final ThreadLocal<Boolean> rendering=ThreadLocal.withInitial(()->false);
  private static final AtomicLong rendererSelections=new AtomicLong(), renderCalls=new AtomicLong(), paintCalls=new AtomicLong(), greenCalls=new AtomicLong(), redCalls=new AtomicLong(), neutralCalls=new AtomicLong();

  public static synchronized String command(String cmd,Instrumentation inst) throws Exception {
    String[] parts=cmd.split("\\|",-1);
    String action=parts[0];
    switch(action) {
      case "report": return diagnosticReport();
      case "probe": {
        if(!inst.isRetransformClassesSupported())throw new UnsupportedOperationException("Retransformation not supported");
        install(inst); enabled=false;visualTest=false;probeMode=true;
        onEdt(TapeViewV0319::restoreAll);
        DebugLogV0319.stop();
        samples.clear();probeSeen.set(0);probeStored.set(0);
        SwingUtilities.invokeLater(()->{for(Window w:Window.getWindows())repaintTapes(w);});
        return "Probe active (NO COLOR CHANGES). Wait for tape activity, then run report --pid. Samples capped at "+MAX_SAMPLES;
      }
      case "status": return DebugLogV0319.status()+" "+LiveTapeDataV0319.status()+" feedHooks="+FeedHookV0319.installed()+" probeMode="+probeMode+" probeSeen="+probeSeen+" probeStored="+probeStored+" enabled="+enabled+" visualTest="+visualTest+" injected="+injected+" rendererSelections="+rendererSelections+" rendererCalls="+renderCalls+" visualPaintCalls="+paintCalls+" green="+greenCalls+" red="+redCalls+" neutral="+neutralCalls+" askColor="+hex(askColor)+" bidColor="+hex(bidColor);
      case "dump": return dumpLoadedClass(inst);
      case "stop": {
        enabled=false;visualTest=false;probeMode=false;
        String feedRestored=FeedHookV0319.stop();
        onEdt(TapeViewV0319::restoreAll);
        LiveTapeDataV0319.clear();
        DebugLogV0319.stop();
        String restored=detach();
        return "Disabled. "+restored+" "+feedRestored+" Reopen or restart TWS if the interface behaves unexpectedly.";
      }
      case "test-paint":case "start": {
        if(action.equals("start") && parts.length==3) {
          askColor=parseHexColor(parts[1]);
          bidColor=parseHexColor(parts[2]);
        } else if(parts.length!=1) throw new IllegalArgumentException("Invalid command arguments");
        if(!inst.isRetransformClassesSupported())throw new UnsupportedOperationException("TWS JVM does not support class retransformation");
        boolean wasInjected=injected;
        try{install(inst);FeedHookV0319.install(inst);}
        catch(Throwable failure){if(!wasInjected)detach();throw failure;}
        probeMode=false;
        visualTest=action.equals("test-paint");enabled=true;
        SwingUtilities.invokeLater(()->{for(Window w:Window.getWindows())repaintTapes(w);});
        return (visualTest?"VISUAL TEST (NOT TRADE CLASSIFICATION): alternating green/red tape rows. ":"NBBO full-row coloring: ask="+hex(askColor)+" bid="+hex(bidColor)+"; neutral untouched; newest 50 trades visible. ")+"Hook installed="+injected+". Use status to inspect counters, stop to restore.";
      }
      default:throw new IllegalArgumentException(cmd);
    }
  }
  /** Obtain class bytes from the live JVM, independent of the installer's on-disk JARs.
   *  The transformer returns null, so this command does not modify the target bytecode.
   */
  private static String dumpLoadedClass(Instrumentation inst) throws Exception {
    Class<?> candidate=null;
    int matches=0;
    for(Class<?> c:inst.getAllLoadedClasses()) if(c.getName().equals("jtable.basic.by")) {
      matches++;
      if(candidate==null)candidate=c;
    }
    if(candidate==null)throw new IllegalStateException("jtable.basic.by not loaded; open a TWS table first");
    if(!inst.isRetransformClassesSupported()||!inst.isModifiableClass(candidate))
      throw new UnsupportedOperationException("JVM cannot capture jtable.basic.by through retransformation");
    final Class<?> target=candidate;
    final java.util.concurrent.atomic.AtomicReference<byte[]> snapshot=new java.util.concurrent.atomic.AtomicReference<>();
    final java.util.concurrent.atomic.AtomicReference<String> methodDetails=new java.util.concurrent.atomic.AtomicReference<>();
    ClassFileTransformer capture=new ClassFileTransformer() {
      @Override public byte[] transform(ClassLoader loader,String name,Class<?> redef,ProtectionDomain pd,byte[] bytes) {
        if(redef!=target||!TARGET.equals(name))return null;
        snapshot.set(bytes.clone());
        StringBuilder methods=new StringBuilder();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
          @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
            methods.append(String.format("%04x",access)).append(' ').append(method).append(descriptor).append('\n');
            return null;
          }
        }, ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        methodDetails.set(methods.toString());
        return null; // DO NOT ALTER THE TARGET CLASS
      }
    };
    inst.addTransformer(capture,true);
    try {inst.retransformClasses(target);} finally {inst.removeTransformer(capture);}
    byte[] bytes=snapshot.get();
    if(bytes==null)throw new IllegalStateException("JVM did not provide target class bytes");
    Path agent=Path.of(HookV0319.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path logs=agent.getParent().getParent().resolve("logs");Files.createDirectories(logs);
    Path output=logs.resolve("twstapecolor-live-by-"+ProcessHandle.current().pid()+".class");
    Files.write(output,bytes);
    String superName=new ClassReader(bytes).getSuperName();
    return "DUMP OK: "+output+" ("+bytes.length+" bytes)"+
      "\nloadedClassCount="+matches+
      "\nclassLoader="+target.getClassLoader()+
      "\nsuperClass="+superName+
      "\ngetCellRendererMatches="+methodDetails.get().lines().filter(x->x.contains("getCellRenderer")).count()+
      "\nmethods:\n"+methodDetails.get();
  }

  private static void install(Instrumentation inst) throws Exception {
    if(injected)return;
    Class<?> candidate=null;
    for(Class<?> type:inst.getAllLoadedClasses()) if(type.getName().equals("jtable.basic.by")){candidate=type;break;}
    if(candidate==null)throw new IllegalStateException("jtable.basic.by is not loaded. Open Time & Sales before attaching.");
    if(!inst.isModifiableClass(candidate))throw new UnsupportedOperationException("Target table class not modifiable");
    targetClass=candidate;
    final Class<?> chosenTarget=candidate;
    transformFailure.set(null);
    transformer=new ClassFileTransformer(){
      @Override public byte[] transform(ClassLoader loader,String name,Class<?> redef,ProtectionDomain domain,byte[] bytes) {
        if(!TARGET.equals(name)||redef!=chosenTarget)return null;
        try {
        ClassReader cr=new ClassReader(bytes);
        // TWS uses its own class loader; ASM must resolve frame types through that loader.
        ClassWriter cw=new ClassWriter(cr,ClassWriter.COMPUTE_MAXS){
          @Override protected String getCommonSuperClass(String a,String b){
            try {
              ClassLoader cl=loader!=null?loader:ClassLoader.getPlatformClassLoader();
              Class<?> x=Class.forName(a.replace('/','.'),false,cl);
              Class<?> y=Class.forName(b.replace('/','.'),false,cl);
              if(x.isAssignableFrom(y))return a;
              if(y.isAssignableFrom(x))return b;
              if(x.isInterface()||y.isInterface())return "java/lang/Object";
              do {x=x.getSuperclass();} while(x!=null&&!x.isAssignableFrom(y));
              return x==null?"java/lang/Object":x.getName().replace('.','/');
            }catch(ClassNotFoundException | LinkageError ex){
              throw new IllegalStateException("Cannot compute bytecode frames for "+a+" and "+b+" in TWS class loader",ex);
            }
          }
        };
        final int[] count={0};
        cr.accept(new ClassVisitor(Opcodes.ASM9,cw){
          @Override public MethodVisitor visitMethod(int flags,String n,String desc,String sig,String[] exceptions){
            MethodVisitor mv=super.visitMethod(flags,n,desc,sig,exceptions);
            if(n.equals("getCellRenderer")&&desc.equals("(II)Ljavax/swing/table/TableCellRenderer;")){
              count[0]++;
              return new MethodVisitor(Opcodes.ASM9,mv){
                @Override public void visitInsn(int opcode){
                  if(opcode==Opcodes.ARETURN){
                    // The target's StackMapTable marks slots 1 and 2 dead at the return.
                    // Never ILOAD those arguments here: this was the VerifyError trigger.
                    super.visitVarInsn(Opcodes.ALOAD,0);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,"twstapecolor/HookV0319","wrap","(Ljavax/swing/table/TableCellRenderer;Ljava/lang/Object;)Ljavax/swing/table/TableCellRenderer;",false);
                  }
                  super.visitInsn(opcode);
                }
              };
            }
            return mv;
          }
        },0);
        if(count[0]!=1)throw new IllegalStateException("Expected getCellRenderer method once, found "+count[0]);
        return cw.toByteArray();
        } catch(Throwable failure) {
          transformFailure.set(failure);
          // Instrumentation may suppress transformer exceptions: retain the real cause.
          return null;
        }
      }
    };
    instrumentation=inst; // Retain the registering Instrumentation instance across later commands.
    inst.addTransformer(transformer,true);
    try {
      inst.retransformClasses(candidate);
      Throwable failure=transformFailure.get();
      if(failure!=null)throw new IllegalStateException("Bytecode transformation failed; TWS class was left unchanged",failure);
      injected=true;
    } catch(Throwable error) {
      inst.removeTransformer(transformer);transformer=null;targetClass=null;
      Throwable failure=transformFailure.get();
      if(failure!=null && failure!=error)error.addSuppressed(failure);
      throw error;
    }
  }

  private static Color parseHexColor(String hex) {
    if(!hex.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Color must be #RRGGBB");
    return new Color(Integer.parseInt(hex.substring(1),16));
  }
  private static String hex(Color c){return String.format("#%06X",c.getRGB()&0xffffff);}
  private static String diagnosticReport(){
    StringBuilder sb=new StringBuilder("Probe seen="+probeSeen+" stored="+probeStored+" max="+MAX_SAMPLES+"\n");
    for(String line:samples)sb.append(line).append('\n');
    return sb.toString();
  }
  private static void recordProbe(JTable table,Object value,Component component,TableCellRenderer delegate,int row,int col,boolean selected){
    // Keep instrumentation on the EDT fast; capture only the first bounded number of values.
    probeSeen.incrementAndGet();
    long slot=probeStored.getAndIncrement();
    if(slot>=MAX_SAMPLES)return;
    try {
      int modelCol=table.convertColumnIndexToModel(col);
      StringBuilder sb=new StringBuilder("row=").append(row).append(" col=").append(col).append(" modelCol=").append(modelCol)
        .append(" selected=").append(selected).append(" column=").append(shortText(table.getColumnName(col)))
        .append(" table=").append(table.getClass().getName()).append(" model=").append(table.getModel().getClass().getName())
        .append(" valueType=").append(className(value)).append(" value=").append(shortText(value))
        .append(" renderer=").append(className(delegate)).append(" comp=").append(className(component))
        .append(" bg=").append(rgb(component.getBackground())).append(" fg=").append(rgb(component.getForeground()));
      appendComponent(sb,component,0);
      samples.add(sb.toString());
    }catch(Throwable e){samples.add("probeError="+e.getClass().getName()+":"+shortText(e.getMessage()));}
  }
  private static void appendComponent(StringBuilder sb,Component c,int depth){
    if(depth>2)return;
    if(c instanceof JLabel l){sb.append(" label=").append(shortText(l.getText()));appendIcon(sb,l.getIcon());}
    if(c instanceof AbstractButton b){sb.append(" button=").append(shortText(b.getText()));appendIcon(sb,b.getIcon());}
    if(c instanceof JComponent j){sb.append(" opaque=").append(j.isOpaque());
      for(String key:new String[]{"PROPERTY_NAME_COLOR_TAG","colorTag","direction","tradeDirection","arrow","icon"}){
        Object v=j.getClientProperty(key);if(v!=null)sb.append(" client[").append(key).append("]=").append(className(v)).append(':').append(shortText(v));
      }
    }
    if(c instanceof Container container){int n=Math.min(container.getComponentCount(),5);for(int i=0;i<n;i++){
      Component child=container.getComponent(i);sb.append(" child").append(depth).append('[').append(i).append("]=").append(className(child));appendComponent(sb,child,depth+1);
    }}
  }
  private static void appendIcon(StringBuilder sb,Icon icon){if(icon!=null)sb.append(" icon=").append(className(icon)).append("[").append(icon.getIconWidth()).append('x').append(icon.getIconHeight()).append(']');}
  private static String className(Object o){return o==null?"null":o.getClass().getName();}
  private static String rgb(Color c){return c==null?"null":String.format("#%08X",c.getRGB());}
  private static String shortText(Object o){if(o==null)return "null";
    String x;try{x=String.valueOf(o);}catch(Throwable e){return "<toString-error>";}
    x=x.replaceAll("[\\r\\n\\t]"," ");return x.length()>80?x.substring(0,80)+"...":x;
  }

  private static String detach(){
    if(!injected)return "No active bytecode hook.";
    try{
      if(!instrumentation.removeTransformer(transformer))return "WARNING: renderer transformer could not be removed; it remains disabled. Restart TWS.";
      instrumentation.retransformClasses(targetClass);
      injected=false;transformer=null;targetClass=null;
      return "Original getCellRenderer bytecode restored.";
    }catch(Throwable error){return "WARNING: hook restoration failed: "+error+". Restart TWS to clear the hook.";}
  }
  private static void onEdt(Runnable task) throws Exception {
    if(SwingUtilities.isEventDispatchThread())task.run();
    else SwingUtilities.invokeAndWait(task);
  }
  // No row/column argument loads are added to the target bytecode.
  public static TableCellRenderer wrap(TableCellRenderer original,Object receiver){
    while(original instanceof TapeRenderer prior)original=prior.delegate;
    if(!(receiver instanceof JTable table)||!(enabled||probeMode)||original==null||!target(table))return original;
    if(enabled)TapeViewV0319.schedule(table,()->enabled);
    rendererSelections.incrementAndGet();
    return TapeViewV0319.renderer(table,original);
  }
  private static void repaintTapes(Component component){
    if(component instanceof JTable table&&target(table))table.repaint();
    else if(component instanceof Container container)for(Component child:container.getComponents())repaintTapes(child);
  }
  private static boolean target(JTable table){
    return table.getClass().getName().equals("feature.tape.ui.aN")&&MODELS.contains(table.getModel().getClass().getName());
  }
  static final class TapeRenderer implements TableCellRenderer {
    private final TableCellRenderer delegate;
    private final java.lang.ref.WeakReference<JTable> owner;
    private final TintRendererV0319 tintRenderer=new TintRendererV0319();
    TapeRenderer(TableCellRenderer r,JTable table){delegate=r;owner=new java.lang.ref.WeakReference<>(table);}
    @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col){
      // Native renderers may be shared/cached across unrelated tables. Never
      // translate rows, inspect data, or tint outside this wrapper's owner.
      if(owner.get()!=table||!target(table)||!(enabled||probeMode))
        return delegate.getTableCellRendererComponent(table,value,selected,focus,row,col);
      renderCalls.incrementAndGet();
      if(Boolean.TRUE.equals(rendering.get()))return delegate.getTableCellRendererComponent(table,value,selected,focus,row,col);
      Component comp;
      rendering.set(true);
      try {
        // Native TWS renderers fetch the event directly by row, ignoring JTable's
        // normal value argument. Translate the private filtered view explicitly.
        int nativeRow=LiveTapeDataV0319.supports(table.getModel())?table.convertRowIndexToModel(row):row;
        comp=delegate.getTableCellRendererComponent(table,value,selected,focus,nativeRow,col);
      }
      finally {rendering.set(false);}
      if(probeMode) {recordProbe(table,value,comp,delegate,row,col,selected);return comp;}
      if(!enabled||selected)return comp;
      TapeViewV0319.Print print=TapeViewV0319.print(table,row);
      int direction=visualTest?((row&1)==0?1:-1):print.direction();
      if(direction==0){neutralCalls.incrementAndGet();return comp;}
      Color tint=direction>0?askColor:bidColor;
      if(direction>0)greenCalls.incrementAndGet();else redCalls.incrementAndGet();
      paintCalls.incrementAndGet();
      return tintRenderer.configure(comp,tint,visualTest?1:print.intensity());
    }
  }
}
