import java.awt.*;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Label;

/** Compares stable table state and normalized target bytecode around stop/start. */
public final class StateAudit {
 static Map<String,String> baseline;
 static final Set<String> names=Set.of("jtable.basic.by","jclient.record.o","feature.tape.data.a");
 public static void agentmain(String args,Instrumentation inst)throws Exception{
  String[] parts=args.split("\\|",-1);Path root=Path.of(parts[0]);String mode=parts[1];Files.createDirectories(root);
  SortedMap<String,String> state=new TreeMap<>();
  SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())walk(window,state,root);});
  for(Class<?> type:inst.getAllLoadedClasses())if(names.contains(type.getName())){
   ClassFileTransformer capture=new ClassFileTransformer(){public byte[] transform(ClassLoader l,String name,Class<?> c,ProtectionDomain pd,byte[] bytes){
    if(c!=type)return null;
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
     @Override public MethodVisitor visitMethod(int access,String method,String desc,String sig,String[] ex){
      boolean target=type.getName().equals("jtable.basic.by")&&method.equals("getCellRenderer")&&desc.equals("(II)Ljavax/swing/table/TableCellRenderer;")||type.getName().equals("jclient.record.o")&&method.equals("a")&&desc.equals("(Ljclient/IRecObservable$Scope;Ljclient/IRecObservable$ObserverType;[Ljclient/record/RecordChangeEnum;)V")||type.getName().equals("feature.tape.data.a")&&method.equals("a")&&desc.equals("(Lfeature/timesales/l;)Lfeature/tape/data/a;");
      if(!target)return null;
      String key=type.getName()+"."+method+desc;
      return new MethodVisitor(Opcodes.ASM9){
       StringBuilder code=new StringBuilder();Map<Label,Integer> labels=new IdentityHashMap<>();int hooks;
       int label(Label l){return labels.computeIfAbsent(l,k->labels.size());}
       void add(Object... values){code.append(Arrays.deepToString(values)).append('\n');}
       public void visitInsn(int o){add(o);}public void visitIntInsn(int o,int v){add(o,v);}public void visitVarInsn(int o,int v){add(o,v);}
       public void visitTypeInsn(int o,String t){add(o,t);}public void visitFieldInsn(int o,String own,String n,String d){add(o,own,n,d);}
       public void visitMethodInsn(int o,String own,String n,String d,boolean itf){add(o,own,n,d,itf);if(own.startsWith("twstapecolor/"))hooks++;}
       public void visitInvokeDynamicInsn(String n,String d,Handle h,Object... a){add(n,d,h,a);}public void visitJumpInsn(int o,Label l){add(o,label(l));}
       public void visitLabel(Label l){add("label",label(l));}public void visitLdcInsn(Object v){add("ldc",v);}public void visitIincInsn(int v,int i){add("inc",v,i);}
       public void visitTableSwitchInsn(int min,int max,Label d,Label... ls){add("switch",min,max,label(d),Arrays.stream(ls).mapToInt(this::label).toArray());}
       public void visitLookupSwitchInsn(Label d,int[] keys,Label[] ls){add("lookup",label(d),keys,Arrays.stream(ls).mapToInt(this::label).toArray());}
       public void visitMultiANewArrayInsn(String d,int n){add("array",d,n);}public void visitTryCatchBlock(Label a,Label b,Label h,String t){add("catch",label(a),label(b),label(h),t);}
       public void visitEnd(){try{state.put(key+".sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));state.put(key+".agentCalls",String.valueOf(hooks));}catch(Exception e){throw new RuntimeException(e);}}
      };
     }
    },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return null;
   }};
   inst.addTransformer(capture,true);try{inst.retransformClasses(type);}finally{inst.removeTransformer(capture);}
  }
  StringBuilder out=new StringBuilder();state.forEach((k,v)->out.append(k).append('=').append(v).append('\n'));
  if(mode.equals("baseline")){baseline=Map.copyOf(state);out.append("BASELINE_SAVED\n");}
  else if(mode.equals("check")){
   if(baseline==null)throw new IllegalStateException("No baseline");
   int mismatch=0;for(String key:baseline.keySet())if(!Objects.equals(baseline.get(key),state.get(key))){mismatch++;out.append("MISMATCH ").append(key).append(" before=").append(baseline.get(key)).append(" after=").append(state.get(key)).append('\n');}
   for(String key:state.keySet())if(!baseline.containsKey(key)){mismatch++;out.append("EXTRA ").append(key).append('\n');}
   out.append("mismatches=").append(mismatch).append("\ncomparedProperties=").append(baseline.size()).append('\n');
  }
  Files.writeString(root.resolve(mode+".txt"),out);
 }
 static String ids(Object[] objects){return Arrays.stream(objects).map(x->{if(x instanceof java.beans.PropertyChangeListenerProxy proxy){Object listener=proxy.getListener();return proxy.getPropertyName()+":"+listener.getClass().getName()+"@"+Integer.toHexString(System.identityHashCode(listener));}return x.getClass().getName()+"@"+Integer.toHexString(System.identityHashCode(x));}).sorted().toList().toString();}
 static void walk(Component c,Map<String,String> out,Path root){
  if(c instanceof JTable t&&t.getClass().getName().equals("feature.tape.ui.aN")){
   String key="table@"+Integer.toHexString(System.identityHashCode(t))+".";
   out.put(key+"model",t.getModel().getClass().getName()+"@"+Integer.toHexString(System.identityHashCode(t.getModel())));
   out.put(key+"sorter",t.getRowSorter()==null?"null":t.getRowSorter().getClass().getName()+"@"+System.identityHashCode(t.getRowSorter()));
   out.put(key+"rowHeight",String.valueOf(t.getRowHeight()));out.put(key+"rowMargin",String.valueOf(t.getRowMargin()));
   out.put(key+"autoCreateRowSorter",String.valueOf(t.getAutoCreateRowSorter()));out.put(key+"spacing",t.getIntercellSpacing().toString());
   out.put(key+"rowSelectionAllowed",String.valueOf(t.getRowSelectionAllowed()));out.put(key+"columnSelectionAllowed",String.valueOf(t.getColumnSelectionAllowed()));
   out.put(key+"selectionMode",String.valueOf(t.getSelectionModel().getSelectionMode()));
   out.put(key+"selectedColumns",Arrays.toString(t.getSelectedColumns()));out.put(key+"selectionEmpty",String.valueOf(t.getSelectionModel().isSelectionEmpty()));
   out.put(key+"background",String.valueOf(t.getBackground()));out.put(key+"foreground",String.valueOf(t.getForeground()));
   out.put(key+"propertyListeners",ids(t.getPropertyChangeListeners()));
   if(t.getModel() instanceof AbstractTableModel model)out.put(key+"modelListeners",ids(model.getTableModelListeners()));
   if(t.isShowing())try{
    var image=new java.awt.image.BufferedImage(t.getWidth(),t.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
    var graphics=image.createGraphics();try{t.paint(graphics);}finally{graphics.dispose();}
    javax.imageio.ImageIO.write(image,"png",root.resolve("state-"+t.getModel().getClass().getSimpleName()+".png").toFile());
    Point location=t.getLocationOnScreen();javax.imageio.ImageIO.write(new Robot().createScreenCapture(new Rectangle(location.x,location.y,t.getWidth(),t.getHeight())),"png",root.resolve("screen-"+t.getModel().getClass().getSimpleName()+".png").toFile());
   }catch(Exception error){out.put(key+"captureError",error.toString());}
   for(int i=0;i<t.getColumnCount();i++){TableColumn col=t.getColumnModel().getColumn(i);out.put(key+"column"+i,col.getModelIndex()+":"+col.getHeaderValue()+":"+col.getWidth()+":"+col.getPreferredWidth()+":"+col.getResizable()+":"+(col.getCellRenderer()==null?"default":col.getCellRenderer().getClass().getName()));}
  }
  if(c instanceof Container container)for(Component child:container.getComponents())walk(child,out,root);
 }
}
