package twstapecolor;
import java.nio.file.*;
import java.util.*;
public final class DebugLogTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  Path root=Path.of("work/debug-test");Files.createDirectories(root);
  Path prefix=root.resolve("events");for(int i=0;i<4;i++)Files.deleteIfExists(Path.of(prefix+"-"+i+".csv"));
  LiveTapeDataV0319.clear();DebugLogV0319.start(prefix);
  var book=new LiveDataTest.Book(10,11,100,200);var record=new LiveDataTest.Record(book);var instrument=new LiveDataTest.Instrument(record);
  LiveTapeDataV0319.trade(new LiveDataTest.Event(instrument,11,50));
  book.bid=11;book.ask=12;LiveTapeDataV0319.quote(record,new Object[0]);
  LiveTapeDataV0319.trade(new LiveDataTest.Event(instrument,11,10));
  DebugLogV0319.stop();var lines=Files.readAllLines(Path.of(prefix+"-0.csv"));
  check(lines.size()==5,"quote and trade events flushed on stop");
  check(lines.get(1).split(",")[3].equals("Q"),"seed quote precedes trade");
  check(lines.get(2).endsWith(",1"),"ask direction saved");
  check(lines.get(4).endsWith(",-1"),"bid direction saved");
  check(lines.get(2).split(",")[12].equals(lines.get(1).split(",")[12]),"frozen quote sequence saved");
  check(DebugLogV0319.status().contains("dropped=0"),"loss counters accurate");
  long bytes=Files.size(Path.of(prefix+"-0.csv"));LiveTapeDataV0319.trade(new LiveDataTest.Event(instrument,12,1));
  check(Files.size(Path.of(prefix+"-0.csv"))==bytes,"disabled logging produces no event writes");
  // Force frequent rotation and queue pressure under a 64 MiB heap.
  DebugLogV0319.start(prefix,1024);
  for(int i=0;i<100000;i++)DebugLogV0319.event("T",record,null,11,100,new LiveTapeDataV0319.Quote(10,11,1,2,1),1);
  DebugLogV0319.stop();
  for(int i=0;i<4;i++){Path file=Path.of(prefix+"-"+i+".csv");check(Files.exists(file)&&Files.size(file)<=1024,"rotated file exceeds cap");check(Files.readString(file).startsWith(DebugLogV0319.HEADER),"rotation missing schema");}
  check(Thread.getAllStackTraces().keySet().stream().noneMatch(t->t.isAlive()&&t.getName().equals("twstapecolor-debug-v0319")),"writer leaked after stop");
  Path blocker=root.resolve("blocker");Files.writeString(blocker,"file");boolean failed=false;try{DebugLogV0319.start(blocker.resolve("bad"));}catch(java.io.IOException expected){failed=true;}
  check(failed,"invalid destination must report failure");
  check(!DebugLogV0319.status().startsWith("liveDebug=on"),"failed start enabled logger");
  System.out.println("Debug log tests passed ("+checks+" checks)");
 }
}
