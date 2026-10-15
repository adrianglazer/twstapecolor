package twstapecolor;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.table.TableModel;

/** Read-only accessors verified against TWS 10.51.1c. No subscription/config writes. */
final class LiveTapeDataV0314 {
  record Quote(double bid,double ask,double bidSize,double askSize,long sequence){}
  record Trade(double last,double size,double bid,double ask){}
  private record Captured(double last,double size,Quote quote){}
  private static final Map<Object,Quote> quotes=new IdentityHashMap<>();
  private static final Map<Object,Captured> trades=new IdentityHashMap<>();
  private static final Map<Object,Captured> current=new IdentityHashMap<>();
  private static final ArrayDeque<Object> order=new ArrayDeque<>();
  private static final AtomicLong quoteEvents=new AtomicLong(),tradeEvents=new AtomicLong(),errors=new AtomicLong();
  private static volatile String lastQuote="none",lastTrade="none",lastError="none";
  private static final ClassValue<Optional<Method>> rowGetter=new ClassValue<>(){
    @Override protected Optional<Method> computeValue(Class<?> type){
      if(!Set.of("feature.tape.ui.aT","feature.tape.data.k").contains(type.getName()))return Optional.empty();
      try{return Optional.of(type.getMethod("e",int.class,int.class));}catch(NoSuchMethodException e){return Optional.empty();}
    }
  };
  private static final ClassValue<Access> access=new ClassValue<>(){
    @Override protected Access computeValue(Class<?> type){return new Access(type);}
  };
  private static final class Access {
    final Class<?> type;
    final Map<String,Method> methods=new java.util.concurrent.ConcurrentHashMap<>();
    Access(Class<?> type){this.type=type;}
    Object get(Object object,String name)throws ReflectiveOperationException{
      Method m=methods.get(name);
      if(m==null){m=type.getMethod(name);if(!Modifier.isPublic(m.getDeclaringClass().getModifiers()))m.setAccessible(true);methods.put(name,m);}
      return m.invoke(object);
    }
  }
  private static Object get(Object object,String name)throws ReflectiveOperationException{return access.get(object.getClass()).get(object,name);}
  static boolean supports(TableModel model){return rowGetter.get(model.getClass()).isPresent();}
  private static Object event(TableModel model,int row)throws ReflectiveOperationException{return rowGetter.get(model.getClass()).orElseThrow().invoke(model,row,-1);}
  static Trade read(TableModel model,int row){
    try{
      Object event=event(model,row);if(event==null)return null;
      double last=TapeViewV0314.number(get(event,"D")),size=TapeViewV0314.number(get(event,"F"));
      Object record=record(event);watch(record);
      Captured captured;
      synchronized(trades){captured=trades.get(event);}
      if(event.getClass().getName().equals("com.ib.chart.timesales.Z")){
        synchronized(quotes){Captured now=current.get(record);if(now!=null&&now.last()==last&&now.size()==size)captured=now;}
      }
      // Never apply today's live quote to an older unobserved event.
      Quote q=captured==null?null:captured.quote();
      return new Trade(last,size,q==null?Double.NaN:q.bid(),q==null?Double.NaN:q.ask());
    }catch(ReflectiveOperationException|RuntimeException e){failure(e);return null;}
  }
  private static Object record(Object event)throws ReflectiveOperationException{return get(get(event,"ar"),"rk");}
  private static boolean watch(Object record)throws ReflectiveOperationException{
    synchronized(quotes){if(quotes.containsKey(record))return true;if(quotes.size()>=64)return false;quotes.put(record,null);}
    // Seed once at attach/symbol discovery. Only future trade events use this seed.
    update(record);
    return true;
  }
  static void quote(Object record,Object[] changes){
    synchronized(quotes){if(!quotes.containsKey(record))return;}
    // TWS usually passes an empty changes array: the actual changed-field bits
    // are accumulated on the top record before this notification boundary.
    try{update(record);}catch(ReflectiveOperationException|RuntimeException e){synchronized(quotes){if(quotes.containsKey(record))quotes.put(record,null);}failure(e);}
  }
  private static synchronized void update(Object record)throws ReflectiveOperationException{
    // Read the existing record; do not ask TWS to allocate a feed or keep it alive.
    Field p=record.getClass().getDeclaredField("p");p.setAccessible(true);Object top=p.get(record);
    if(top==null){synchronized(quotes){if(quotes.containsKey(record))quotes.put(record,null);}lastQuote="unavailable";return;}
    Object book=get(top,"cO");
    if(book==null){synchronized(quotes){if(quotes.containsKey(record))quotes.put(record,null);}lastQuote="unavailable";return;}
    double bid=TapeViewV0314.number(get(book,"G")),ask=TapeViewV0314.number(get(book,"F"));
    double bidSize=TapeViewV0314.number(get(book,"aa")),askSize=TapeViewV0314.number(get(book,"Z"));
    synchronized(quotes){
      Quote prior=quotes.get(record);
      if(prior!=null&&Double.compare(prior.bid(),bid)==0&&Double.compare(prior.ask(),ask)==0&&Double.compare(prior.bidSize(),bidSize)==0&&Double.compare(prior.askSize(),askSize)==0)return;
    }
    Quote quote=new Quote(bid,ask,bidSize,askSize,quoteEvents.incrementAndGet());
    synchronized(quotes){if(quotes.containsKey(record))quotes.put(record,quote);}
    lastQuote="bid="+bid+"x"+bidSize+" ask="+ask+"x"+askSize;
  }
  static void trade(Object event){
    if(event==null)return;
    try{
      Object record=record(event);if(!watch(record))return;
      Quote quote; synchronized(quotes){quote=quotes.get(record);}
      double last=TapeViewV0314.number(get(event,"D")),size=TapeViewV0314.number(get(event,"F"));
      if(!Double.isFinite(last)||!Double.isFinite(size))return;
      Captured captured=new Captured(last,size,quote);
      synchronized(trades){
        if(!trades.containsKey(event))order.add(event);
        trades.put(event,captured);
        while(order.size()>4096)trades.remove(order.remove());
      }
      synchronized(quotes){current.put(record,captured);}
      tradeEvents.incrementAndGet();
      lastTrade="last="+last+" size="+size+" quote="+(quote==null?"none":quote.bid()+"/"+quote.ask()+"#"+quote.sequence())+" direction="+(quote==null?0:TapeViewV0314.classify(last,quote.bid(),quote.ask()));
    }catch(ReflectiveOperationException|RuntimeException e){failure(e);}
  }
  private static void failure(Exception e){errors.incrementAndGet();lastError=e.toString();}
  static String status(){return "quoteEvents="+quoteEvents+" tradeEvents="+tradeEvents+" dataErrors="+errors+" lastQuote=["+lastQuote+"] lastTrade=["+lastTrade+"] lastDataError=["+lastError+"]";}
  static void clear(){synchronized(quotes){quotes.clear();current.clear();}synchronized(trades){trades.clear();order.clear();}}
}
