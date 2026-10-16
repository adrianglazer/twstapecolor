package twstapecolor;
import java.util.*;
public final class SymbolQuoteTest {
 static int checks;
 static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
 static int direction(LiveDataTest.Event event){return TapeViewV0320.snapshot(new feature.tape.data.k(event)).get(0).direction();}
 public static void main(String[] args)throws Exception{
  LiveTapeDataV0320.clear();LiveDataTest.Event first=null,last=null;
  for(int i=0;i<256;i++){
   boolean low=i%2==0;double bid=low?10:10000;
   var record=new LiveDataTest.Record(new LiveDataTest.Book(bid,bid+1,100,200));
   var event=new LiveDataTest.Event(new LiveDataTest.Instrument(record),low?bid+1:bid,100);
   LiveTapeDataV0320.trade(event);
   check(direction(event)==(low?1:-1),"new instrument lost quotes or reused the prior symbol's book after switch "+i);
   check(LiveTapeDataV0320.watching(record),"latest symbol was not watched after cache turnover");
   if(i==0)first=event;last=event;
  }
  check(direction(first)==1,"record eviction recolored an older frozen trade");
  var replacement=new LiveDataTest.Instrument(new LiveDataTest.Record(new LiveDataTest.Book(10000,10001,1,1)));
  first.instrument=replacement;first.last=10000;
  check(direction(first)==0,"reused event inherited a quote from another instrument");
  LiveTapeDataV0320.trade(first);check(direction(first)==-1,"reused event could not capture its new instrument");
  for(String name:new String[]{"quotes","current","watched"}){var field=LiveTapeDataV0320.class.getDeclaredField(name);field.setAccessible(true);check(((Map<?,?>)field.get(null)).size()<=64,"unbounded record cache: "+name);}
  var field=LiveTapeDataV0320.class.getDeclaredField("watchOrder");field.setAccessible(true);check(((Collection<?>)field.get(null)).size()<=64,"unbounded record eviction queue");
  LiveTapeDataV0320.clear();check(!LiveTapeDataV0320.watching(last.instrument.record),"stop left a watched symbol");
  check(((Collection<?>)field.get(null)).isEmpty(),"stop retained the watched-record eviction queue");
  System.out.println("Symbol quote tests passed ("+checks+" checks)");
 }
}
