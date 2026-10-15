import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.table.*;

/** Headless repeated rendering benchmark. Run in a separate JVM for each version. */
public final class RendererBenchmark {
 static JTable table;static TableCellRenderer nativeRenderer;static Method wrap;static int mutations;
 public static void main(String[] args)throws Exception{
  Class<?> hook=Class.forName("twstapecolor.Hook"+args[0]);Field enabled=hook.getDeclaredField("enabled");enabled.setAccessible(true);enabled.setBoolean(null,true);
  wrap=hook.getMethod("wrap",TableCellRenderer.class,Object.class);
  SwingUtilities.invokeAndWait(()->{try{
   Class<?> model=Class.forName("feature.tape.ui.aT");Object m=model.getConstructor(Object[][].class).newInstance((Object)new Object[][]{{101,100,null,null},{null,null,100,101}});
   table=(JTable)Class.forName("feature.tape.ui.aN").getConstructor(model).newInstance(m);
   var renderer=new DefaultTableCellRenderer();renderer.setBackground(Color.BLACK);renderer.addPropertyChangeListener(e->{if(e.getPropertyName().equals("background")||e.getPropertyName().equals("opaque"))mutations++;});nativeRenderer=renderer;
   wrap.invoke(null,nativeRenderer,table);
  }catch(Exception e){throw new RuntimeException(e);}});
  SwingUtilities.invokeAndWait(()->{});
  int iterations=Integer.parseInt(args[1]);SwingUtilities.invokeAndWait(()->{
   try{
    var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();long id=Thread.currentThread().threadId();
    var image=new BufferedImage(110,18,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
    long cpu=bean.getCurrentThreadCpuTime(),allocated=bean.getThreadAllocatedBytes(id),start=System.nanoTime();int wrappers=0,components=0;Object lastWrapper=null,lastComponent=null;
    for(int i=0;i<iterations;i++){
     TableCellRenderer r=(TableCellRenderer)wrap.invoke(null,nativeRenderer,table);
     Component c=r.getTableCellRendererComponent(table,101,false,false,0,0);c.setBounds(0,0,110,18);c.paint(g);
     if(r!=lastWrapper)wrappers++;if(c!=lastComponent)components++;lastWrapper=r;lastComponent=c;
    }
    g.dispose();System.out.println("iterations="+iterations+" cpuMs="+(bean.getCurrentThreadCpuTime()-cpu)/1e6+" wallMs="+(System.nanoTime()-start)/1e6+" allocatedBytes="+(bean.getThreadAllocatedBytes(id)-allocated)+" wrapperChanges="+wrappers+" componentChanges="+components+" nativeStyleWrites="+mutations);
   }catch(Exception e){throw new RuntimeException(e);}
  });
 }
}
