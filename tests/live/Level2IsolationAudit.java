import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.instrument.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Bounded paired native Level 2 renders around tape paints on the same EDT turn. */
public final class Level2IsolationAudit {
 static boolean active;static Path root;static int samples,paired,changed,foreignWrappers,errors,nativeColoredRows;
 static final List<JTable> depth=new ArrayList<>(),tapes=new ArrayList<>();
 static final Map<JTable,Object> sorters=new IdentityHashMap<>();
 static int sorterChanges;static StringBuilder details;
 static void find(Component c){if(c instanceof JTable t&&t.isShowing()){if(t.getClass().getName().equals("jbook.OneSideBookTable"))depth.add(t);else if(t.getClass().getName().equals("feature.tape.ui.aN"))tapes.add(t);}if(c instanceof Container box)for(Component child:box.getComponents())find(child);}
 static BufferedImage paint(JTable t){var image=new BufferedImage(t.getWidth(),t.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{t.paint(g);}finally{g.dispose();}return image;}
 public static void agentmain(String args,Instrumentation ignored)throws Exception{
  if(active)throw new IllegalStateException("audit active");String[] p=args.split("\\|",-1);root=Path.of(p[0]);Files.createDirectories(root);int duration=Integer.parseInt(p[1]);
  active=true;samples=paired=changed=foreignWrappers=errors=nativeColoredRows=sorterChanges=0;depth.clear();tapes.clear();sorters.clear();details=new StringBuilder();
  SwingUtilities.invokeAndWait(()->{
   for(Window w:Window.getWindows())find(w);for(JTable t:depth)sorters.put(t,t.getRowSorter());
   javax.swing.Timer timer=new javax.swing.Timer(1000,e->{sample();if(samples>=duration){((javax.swing.Timer)e.getSource()).stop();finish();}});timer.start();
  });
 }
 static void sample(){samples++;try{
  for(JTable t:depth){
   BufferedImage before=paint(t);
   for(JTable tape:tapes)for(int k=0;k<3;k++)paint(tape);
   BufferedImage after=paint(t);int different=0,colors=0;
   for(int y=0;y<before.getHeight();y++)for(int x=0;x<before.getWidth();x++)if(before.getRGB(x,y)!=after.getRGB(x,y))different++;
   for(int r=0;r<t.getRowCount();r++){
    Rectangle cell=t.getCellRect(r,0,true);if(cell.y+1<before.getHeight()){Color c=new Color(after.getRGB(cell.x+1,cell.y+1));if(c.getRed()!=c.getGreen()||c.getGreen()!=c.getBlue())colors++;}
    for(int c=0;c<t.getColumnCount();c++)if(t.getCellRenderer(r,c).getClass().getName().startsWith("twstapecolor."))foreignWrappers++;
   }
   nativeColoredRows+=colors;paired++;if(different>0)changed++;if(t.getRowSorter()!=sorters.get(t))sorterChanges++;
   details.append(samples+","+depth.indexOf(t)+","+different+","+colors+"\n");
   if(samples==1||samples%10==0){ImageIO.write(after,"png",root.resolve("level2-"+samples+"-"+depth.indexOf(t)+".png").toFile());Point at=t.getLocationOnScreen();ImageIO.write(new Robot().createScreenCapture(new Rectangle(at.x,at.y,t.getWidth(),t.getHeight())),"png",root.resolve("screen-"+samples+"-"+depth.indexOf(t)+".png").toFile());}
  }
 }catch(Throwable e){errors++;try{Files.writeString(root.resolve("errors.txt"),e+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}}
 static void finish(){active=false;try{
  Files.writeString(root.resolve("pairs.csv"),"sample,table,differentPixels,coloredRows\n"+details);
  Files.writeString(root.resolve("summary.txt"),"samples="+samples+"\ndepthTables="+depth.size()+"\ntapeTables="+tapes.size()+"\npairedRenders="+paired+"\nchangedRenders="+changed+"\nnativeColoredRows="+nativeColoredRows+"\nforeignWrappers="+foreignWrappers+"\nsorterChanges="+sorterChanges+"\nerrors="+errors+"\n");
 }catch(Exception ignored){}finally{depth.clear();tapes.clear();sorters.clear();details=null;}}
}
