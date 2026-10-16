package twstapecolor;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Opt-in bounded, asynchronous market-event trace. No disk I/O on feed/EDT callbacks. */
final class DebugLogV0320 {
  static final int CAPACITY=4096, FILES=4, LIMIT=4*1024*1024;
  static final String HEADER="event,epochMillis,monotonicNanos,type,record,eventObject,last,size,bid,ask,bidSize,askSize,quoteSequence,direction\n";
  private static volatile Session session;
  private static volatile String lastStatus="liveDebug=off";
  static final class Session {
    final Path prefix; final int limit;
    final ArrayBlockingQueue<String> queue=new ArrayBlockingQueue<>(CAPACITY);
    final AtomicLong events=new AtomicLong(),dropped=new AtomicLong();
    volatile boolean closing;volatile String error="none";final Thread worker;
    Session(Path prefix,int limit){this.prefix=prefix;this.limit=limit;worker=new Thread(this::write,"twstapecolor-debug-v0320");worker.setDaemon(true);}
    Path file(int slot){return Path.of(prefix+"-"+slot+".csv");}
    void write(){
      int slot=0,bytes=0;BufferedWriter out=null;
      try{
        while(!closing||!queue.isEmpty()){
          String line=queue.poll(250,TimeUnit.MILLISECONDS);
          if(line==null){if(out!=null)out.flush();continue;}
          if(out==null||bytes+line.length()>limit){
            if(out!=null){out.close();slot=(slot+1)%FILES;}
            out=Files.newBufferedWriter(file(slot));out.write(HEADER);bytes=HEADER.length();
          }
          out.write(line);bytes+=line.length();
        }
      }catch(Exception e){error=e.toString();}
      finally{if(out!=null)try{out.close();}catch(IOException e){error=e.toString();}queue.clear();}
    }
    String status(){return "liveDebug="+(closing?"stopped":"on")+" prefix="+prefix+" events="+events+" dropped="+dropped+" queued="+queue.size()+" error="+error;}
  }
  static synchronized void start(Path prefix)throws IOException{start(prefix,LIMIT);}
  static synchronized void start(Path prefix,int limit)throws IOException{
    stop();Files.createDirectories(prefix.toAbsolutePath().getParent());
    // Validate destination before enabling; actual event writes happen on the worker.
    try(var out=Files.newBufferedWriter(Path.of(prefix+"-0.csv"))){out.write(HEADER);}
    Session next=new Session(prefix,limit);session=next;next.worker.start();
  }
  static void event(String kind,Object record,Object event,double last,double size,LiveTapeDataV0320.Quote q,int direction){
    Session s=session;if(s==null)return;
    long seq=s.events.incrementAndGet();
    String line=seq+","+System.currentTimeMillis()+","+System.nanoTime()+","+kind+","+Integer.toHexString(System.identityHashCode(record))+","+(event==null?"":Integer.toHexString(System.identityHashCode(event)))+","+last+","+size+","+(q==null?"NaN,NaN,NaN,NaN,0":q.bid()+","+q.ask()+","+q.bidSize()+","+q.askSize()+","+q.sequence())+","+direction+"\n";
    if(!s.queue.offer(line))s.dropped.incrementAndGet();
  }
  static synchronized void stop(){
    Session old=session;session=null;if(old==null)return;old.closing=true;
    // Never wait indefinitely for a stalled filesystem, and never join on the EDT.
    if(!javax.swing.SwingUtilities.isEventDispatchThread())try{old.worker.join(2000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
    lastStatus=old.status()+(old.worker.isAlive()?" writerDraining=true":"");
  }
  static String status(){Session s=session;return s==null?lastStatus:s.status();}
}
