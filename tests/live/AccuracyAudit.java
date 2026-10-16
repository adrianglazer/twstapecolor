import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.math.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.*;
import org.objectweb.asm.*;

/** Opt-in diagnostic agent. Samples a running tape; does not place orders or change data. */
public final class AccuracyAudit {
  record Book(double bid,double ask,double bidSize,double askSize){}
  static volatile boolean active;
  static String version;static Path root;
  static Instrumentation instrumentation;static ClassFileTransformer transformer;static Class<?>[] targets;
  static final Map<Object,Book> books=new IdentityHashMap<>();
  static final List<JTable> tables=new ArrayList<>();
  static final List<String> events=new ArrayList<>(),pixels=new ArrayList<>();
  static final Set<Integer> distinctTrades=new HashSet<>();
  static long quoteChecks,eventChecks,eventMismatch,pixelChecks,pixelMismatch,directionMismatch,restorationMismatch,errors,deferred,selected;
  static long askTrades,bidTrades,neutralTrades,arrivalBookDifferences,arrivalDirectionDifferences,quoteSizeChecks,quoteSizeMismatch;
  static Class<?> dataClass,viewClass;static Field capturedTrades;static Method printMethod;
  static int samples;
  static Object get(Object object,String method)throws Exception{
    Method m=object.getClass().getMethod(method);m.setAccessible(true);return m.invoke(object);
  }
  static Object field(Object object,String field)throws Exception{
    Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(object);
  }
  static double number(Object value){return value instanceof Number n?n.doubleValue():Double.parseDouble(value.toString());}
  static Book rawBook(Object record)throws Exception{
    Object top=field(record,"p");if(top==null)return null;Object book=get(top,"cO");if(book==null)return null;
    return new Book(number(get(book,"G")),number(get(book,"F")),number(get(book,"aa")),number(get(book,"Z")));
  }
  static Object record(Object event)throws Exception{return get(get(event,"ar"),"rk");}
  static Object row(JTable table,int row)throws Exception{return table.getModel().getClass().getMethod("e",int.class,int.class).invoke(table.getModel(),row,-1);}
  static boolean valid(double x){return Double.isFinite(x)&&x>0&&x<Double.MAX_VALUE;}
  // Independent decimal oracle. Twelve significant digits remove binary quote
  // noise at the observed stock price precision without rounding to a whole tick.
  static BigDecimal decimal(double x){return BigDecimal.valueOf(x).round(new MathContext(12));}
  static int side(double last,double bid,double ask){
    if(!valid(last)||!valid(bid)||!valid(ask)||decimal(ask).compareTo(decimal(bid))<=0)return 0;
    return decimal(last).compareTo(decimal(ask))>=0?1:decimal(last).compareTo(decimal(bid))<=0?-1:0;
  }
  static void find(Component c){
    if(c instanceof JTable t&&t.getClass().getName().equals("feature.tape.ui.aN"))tables.add(t);
    if(c instanceof Container container)for(Component child:container.getComponents())find(child);
  }
  public static synchronized void quote(Object record){
    if(!active||!books.containsKey(record))return;
    try{books.put(record,rawBook(record));quoteChecks++;}catch(Throwable e){errors++;}
  }
  public static synchronized void trade(Object event){
    if(!active||event==null)return;
    try{
      Object record=record(event);if(!books.containsKey(record))return;
      double last=number(get(event,"D")),size=number(get(event,"F"));if(!valid(last)||!valid(size))return;
      Book expected=books.get(record);Object captured;
      Map<?,?> map=(Map<?,?>)capturedTrades.get(null);synchronized(map){captured=map.get(event);}
      if(captured==null){errors++;return;}
      Object actual=get(captured,"quote");
      double bid=actual==null?Double.NaN:number(get(actual,"bid")),ask=actual==null?Double.NaN:number(get(actual,"ask"));
      int oracle=expected==null?0:side(last,expected.bid,expected.ask);
      boolean matches=expected==null?actual==null:Double.compare(expected.bid,bid)==0&&Double.compare(expected.ask,ask)==0;
      boolean tradeMatches=Double.compare(number(get(captured,"last")),last)==0&&Double.compare(number(get(captured,"size")),size)==0;
      Book arrival=rawBook(record);
      int arrivalSide=arrival==null?0:side(last,arrival.bid,arrival.ask);
      boolean arrivalSame=arrival==null?actual==null:Double.compare(arrival.bid,bid)==0&&Double.compare(arrival.ask,ask)==0;
      if(!arrivalSame)arrivalBookDifferences++;
      if(arrivalSide!=oracle)arrivalDirectionDifferences++;
      if(expected!=null&&actual!=null&&valid(expected.bidSize)&&valid(expected.askSize)){
        quoteSizeChecks++;
        if(Double.compare(number(get(actual,"bidSize")),expected.bidSize)!=0||Double.compare(number(get(actual,"askSize")),expected.askSize)!=0)quoteSizeMismatch++;
      }
      eventChecks++;if(!matches||!tradeMatches)eventMismatch++;
      if(oracle>0)askTrades++;else if(oracle<0)bidTrades++;else neutralTrades++;
      events.add(eventChecks+","+System.identityHashCode(event)+","+last+","+size+","+(expected==null?"NaN,NaN":expected.bid+","+expected.ask)+","+bid+","+ask+","+oracle+","+matches+","+tradeMatches+","+(arrival==null?"NaN,NaN":arrival.bid+","+arrival.ask)+","+arrivalSide+","+arrivalSame);
    }catch(Throwable e){errors++;}
  }
  public static void agentmain(String args,Instrumentation inst)throws Exception{
    if(active)throw new IllegalStateException("Audit already active");
    String[] parts=args.split("\\|",-1);root=Path.of(parts[0]);version=parts[1];int seconds=Integer.parseInt(parts[2]);
    Files.createDirectories(root);instrumentation=inst;
    dataClass=Class.forName("twstapecolor.LiveTapeData"+version);viewClass=Class.forName("twstapecolor.TapeView"+version);
    capturedTrades=dataClass.getDeclaredField("trades");capturedTrades.setAccessible(true);
    printMethod=viewClass.getDeclaredMethod("print",JTable.class,int.class);printMethod.setAccessible(true);
    books.clear();tables.clear();events.clear();pixels.clear();distinctTrades.clear();samples=0;
    quoteChecks=eventChecks=eventMismatch=pixelChecks=pixelMismatch=directionMismatch=restorationMismatch=errors=deferred=selected=askTrades=bidTrades=neutralTrades=arrivalBookDifferences=arrivalDirectionDifferences=quoteSizeChecks=quoteSizeMismatch=0;
    SwingUtilities.invokeAndWait(()->{
      for(Window w:Window.getWindows())find(w);
      for(JTable t:tables)try{if(t.getModel().getRowCount()>0){Object r=record(row(t,0));books.put(r,rawBook(r));}if(t.isShowing())SwingUtilities.getWindowAncestor(t).toFront();}catch(Exception e){errors++;}
    });
    Map<String,Class<?>> found=new HashMap<>();for(Class<?> c:inst.getAllLoadedClasses())if(Set.of("jclient.record.o","feature.tape.data.a").contains(c.getName()))found.put(c.getName().replace('.','/'),c);
    targets=found.values().toArray(Class<?>[]::new);
    transformer=new ClassFileTransformer(){
      @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain pd,byte[] bytes){
        if(type==null||found.get(name)!=type)return null;
        ClassReader r=new ClassReader(bytes);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);
        r.accept(new ClassVisitor(Opcodes.ASM9,w){
          @Override public MethodVisitor visitMethod(int access,String method,String desc,String sig,String[] ex){
            MethodVisitor base=super.visitMethod(access,method,desc,sig,ex);
            if(name.equals("jclient/record/o")&&method.equals("a")&&desc.equals("(Ljclient/IRecObservable$Scope;Ljclient/IRecObservable$ObserverType;[Ljclient/record/RecordChangeEnum;)V"))return new MethodVisitor(Opcodes.ASM9,base){
              @Override public void visitCode(){super.visitCode();super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,"AccuracyAudit","quote","(Ljava/lang/Object;)V",false);}};
            if(name.equals("feature/tape/data/a")&&method.equals("a")&&desc.equals("(Lfeature/timesales/l;)Lfeature/tape/data/a;"))return new MethodVisitor(Opcodes.ASM9,base){
              @Override public void visitInsn(int opcode){if(opcode==Opcodes.ARETURN){super.visitInsn(Opcodes.DUP);super.visitMethodInsn(Opcodes.INVOKESTATIC,"AccuracyAudit","trade","(Ljava/lang/Object;)V",false);}super.visitInsn(opcode);}};
            return base;
          }
        },0);return w.toByteArray();
      }
    };
    inst.addTransformer(transformer,true);inst.retransformClasses(targets);active=true;
    javax.swing.Timer timer=new javax.swing.Timer(500,null);
    timer.addActionListener(e->{
      if(!active){timer.stop();return;}
      sample();
      if(samples>=seconds*2){timer.stop();Thread worker=new Thread(()->{try{finish();}catch(Exception ex){try{Files.writeString(root.resolve("audit-error.txt"),ex.toString());}catch(Exception ignored){}}},"twstapecolor-audit-finish");worker.setDaemon(true);worker.start();}
    });
    timer.start();
  }
  static void sample(){
    samples++;
    for(JTable table:tables)try{
      if(!table.isShowing())continue;
      double largest=0;for(int r=0;r<table.getRowCount();r++){Object event=row(table,table.convertRowIndexToModel(r));double size=number(get(event,"F"));if(valid(size))largest=Math.max(largest,size);}
      for(int r=0;r<table.getRowCount();r++){
        int modelRow=table.convertRowIndexToModel(r);Object event=row(table,modelRow);double last=number(get(event,"D")),size=number(get(event,"F"));
        Map<?,?> map=(Map<?,?>)capturedTrades.get(null);Object captured;synchronized(map){captured=map.get(event);}
        Object quote=captured==null?null:get(captured,"quote");
        double bid=quote==null?Double.NaN:number(get(quote,"bid")),ask=quote==null?Double.NaN:number(get(quote,"ask"));
        int expected=side(last,bid,ask);Object print=printMethod.invoke(null,table,r);int applied=((Number)get(print,"direction")).intValue();
        // Pending filter work must not excuse transient neutral colors.
        Field viewsField=viewClass.getDeclaredField("views");viewsField.setAccessible(true);Object state=((Map<?,?>)viewsField.get(null)).get(table);
        if(state!=null&&Boolean.TRUE.equals(field(state,"queued"))){deferred++;}
        if(!table.getModel().getClass().getName().equals("feature.tape.ui.aT"))continue;
        distinctTrades.add(System.identityHashCode(event));
        if(applied!=expected)directionMismatch++;
        for(int c=0;c<table.getColumnCount();c++){
          if(table.isCellSelected(r,c)){selected++;continue;}
          Component comp=table.prepareRenderer(table.getCellRenderer(r,c),r,c);
          boolean wrapped=comp.getClass().getName().equals("twstapecolor.TintRendererV0320");
          Component original=wrapped?(Component)field(comp,"delegate"):comp;
          Color background=original.getBackground();boolean opaque=original instanceof JComponent jc&&jc.isOpaque();Container parent=original.getParent();Rectangle bounds=original.getBounds();
          int width=table.getColumnModel().getColumn(c).getWidth(),height=table.getRowHeight(r);
          BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
          Rectangle outer=comp.getBounds();try{g.setColor(table.getBackground());g.fillRect(0,0,width,height);comp.setBounds(0,0,width,height);comp.paint(g);}finally{g.dispose();comp.setBounds(outer);}
          boolean restored=Objects.equals(background,original.getBackground())&&opaque==(original instanceof JComponent jc&&jc.isOpaque())&&parent==original.getParent()&&bounds.equals(original.getBounds());
          if(!restored)restorationMismatch++;
          if(expected!=0){
            double intensity=.18+.82*Math.sqrt(size/largest);
            Color tint=expected>0?new Color(22,131,74):new Color(184,59,75);Color base=background==null?Color.BLACK:background;
            int red=(int)Math.round(base.getRed()+(tint.getRed()-base.getRed())*intensity),green=(int)Math.round(base.getGreen()+(tint.getGreen()-base.getGreen())*intensity),blue=(int)Math.round(base.getBlue()+(tint.getBlue()-base.getBlue())*intensity);
            int oracleRgb=new Color(red,green,blue).getRGB(),actual=image.getRGB(width-3,1);
            boolean match=wrapped&&actual==oracleRgb;pixelChecks++;if(!match)pixelMismatch++;
            pixels.add(samples+","+System.identityHashCode(event)+","+r+","+c+","+last+","+size+","+bid+","+ask+","+expected+","+applied+","+String.format("%06x",oracleRgb&0xffffff)+","+String.format("%06x",actual&0xffffff)+","+match+","+restored);
          }else if(wrapped){pixelChecks++;pixelMismatch++;}
        }
      }
      if(samples==4||samples%20==0){
        BufferedImage im=new BufferedImage(table.getWidth(),table.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();try{table.paint(g);}finally{g.dispose();}ImageIO.write(im,"png",root.resolve("sample-"+samples+"-"+table.getModel().getClass().getSimpleName()+".png").toFile());
        Point p=table.getLocationOnScreen();ImageIO.write(new Robot().createScreenCapture(new Rectangle(p.x,p.y,table.getWidth(),table.getHeight())),"png",root.resolve("screen-"+samples+"-"+table.getModel().getClass().getSimpleName()+".png").toFile());
      }
    }catch(Throwable e){errors++;try{Files.writeString(root.resolve("sample-errors.txt"),e+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}
  }
  static void finish()throws Exception{
    synchronized(AccuracyAudit.class){active=false;}
    try{
    instrumentation.removeTransformer(transformer);instrumentation.retransformClasses(targets);
    Files.writeString(root.resolve("events.csv"),"index,event,last,size,oracleBid,oracleAsk,capturedBid,capturedAsk,oracleDirection,quoteMatch,tradeMatch,arrivalBid,arrivalAsk,arrivalDirection,arrivalBookMatch\n"+String.join("\n",events)+"\n");
    Files.writeString(root.resolve("pixels.csv"),"sample,event,row,column,last,size,bid,ask,oracleDirection,appliedDirection,oracleRGB,actualRGB,pixelMatch,stateRestored\n"+String.join("\n",pixels)+"\n");
    Files.writeString(root.resolve("summary.txt"),"samples="+samples+"\nquoteChecks="+quoteChecks+"\neventChecks="+eventChecks+"\neventMismatch="+eventMismatch+"\narrivalBookDifferences="+arrivalBookDifferences+"\narrivalDirectionDifferences="+arrivalDirectionDifferences+"\nquoteSizeChecks="+quoteSizeChecks+"\nquoteSizeMismatch="+quoteSizeMismatch+"\naskTrades="+askTrades+"\nbidTrades="+bidTrades+"\nneutralTrades="+neutralTrades+"\ndistinctPaintedTrades="+distinctTrades.size()+"\npixelChecks="+pixelChecks+"\npixelMismatch="+pixelMismatch+"\ndirectionMismatch="+directionMismatch+"\nrestorationMismatch="+restorationMismatch+"\ndeferred="+deferred+"\nselectedSkipped="+selected+"\nerrors="+errors+"\n");
    }finally{books.clear();tables.clear();events.clear();pixels.clear();distinctTrades.clear();transformer=null;targets=null;instrumentation=null;}
  }
}
