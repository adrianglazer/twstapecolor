import java.lang.instrument.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;

/** Bounded 1 Hz process/heap/EDT sampler. Never requests GC or retransforms code. */
public final class ResourceAudit {
 static volatile boolean active;
 public static synchronized void agentmain(String args,Instrumentation ignored)throws Exception{
  if(active)throw new IllegalStateException("resource audit active");
  String[] p=args.split("\\|",-1);int duration=Integer.parseInt(p[1]);
  if(duration<1||duration>3600)throw new IllegalArgumentException("duration 1..3600 seconds");
  Path root=Path.of(p[0]);Files.createDirectories(root);String version=p[2];
  var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
  var memory=ManagementFactory.getMemoryMXBean();
  AtomicBoolean pending=new AtomicBoolean();AtomicLong lag=new AtomicLong();AtomicInteger viewCount=new AtomicInteger(-1);
  var executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"twstapecolor-resource-audit");t.setDaemon(true);return t;});
  var writer=Files.newBufferedWriter(root.resolve("resources.csv"));writer.write("second,processCpuNanos,heapUsed,heapCommitted,gcCount,gcMillis,lastGcHeapUsed,edtLagNanos,views,tradeCache,quoteCache,currentCache\n");
  long start=System.nanoTime();AtomicInteger samples=new AtomicInteger();active=true;
  executor.scheduleAtFixedRate(()->{
   try{
    if(pending.compareAndSet(false,true)){long sent=System.nanoTime();SwingUtilities.invokeLater(()->{try{lag.set(System.nanoTime()-sent);if(!version.equals("native")){Class<?> c=Class.forName("twstapecolor.TapeView"+version);Field f=c.getDeclaredField("views");f.setAccessible(true);viewCount.set(((Map<?,?>)f.get(null)).size());}}catch(Exception ignored2){}finally{pending.set(false);}});}
    long count=0,millis=0,after=0,lastGcEnd=-1;for(var gc:ManagementFactory.getGarbageCollectorMXBeans()){count+=gc.getCollectionCount();millis+=gc.getCollectionTime();if(gc instanceof com.sun.management.GarbageCollectorMXBean bean&&bean.getLastGcInfo()!=null){var values=bean.getLastGcInfo().getMemoryUsageAfterGc();long bytes=0;for(var pool:ManagementFactory.getMemoryPoolMXBeans())if(pool.getType()==MemoryType.HEAP&&values.containsKey(pool.getName()))bytes+=values.get(pool.getName()).getUsed();if(bean.getLastGcInfo().getEndTime()>lastGcEnd){lastGcEnd=bean.getLastGcInfo().getEndTime();after=bytes;}}}
    int[] caches={-1,-1,-1};if(!version.equals("native")){Class<?> c=Class.forName("twstapecolor.LiveTapeData"+version);int k=0;for(String name:new String[]{"trades","quotes","current"}){Field f=c.getDeclaredField(name);f.setAccessible(true);Map<?,?> map=(Map<?,?>)f.get(null);synchronized(map){caches[k++]=map.size();}}}
    var heap=memory.getHeapMemoryUsage();writer.write((System.nanoTime()-start)/1e9+","+os.getProcessCpuTime()+","+heap.getUsed()+","+heap.getCommitted()+","+count+","+millis+","+after+","+lag.get()+","+viewCount.get()+","+caches[0]+","+caches[1]+","+caches[2]+"\n");writer.flush();
    if(samples.incrementAndGet()>=duration){writer.close();executor.shutdown();active=false;Files.writeString(root.resolve("complete.txt"),"samples="+samples.get()+"\n");}
   }catch(Throwable e){try{writer.close();Files.writeString(root.resolve("error.txt"),e.toString());}catch(Exception ignored2){}executor.shutdown();active=false;}
  },0,1,TimeUnit.SECONDS);
 }
}
