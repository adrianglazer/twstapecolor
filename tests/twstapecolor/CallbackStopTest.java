package twstapecolor;
import java.util.*;
import java.util.concurrent.*;
public final class CallbackStopTest {
 static final CountDownLatch inside=new CountDownLatch(1),release=new CountDownLatch(1);
 public static final class BlockingRecord {private final BlockingTop p=new BlockingTop();}
 public static final class BlockingTop {
  public LiveDataTest.Book cO()throws Exception{inside.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("test timeout");return new LiveDataTest.Book(100,101,1,1);}
 }
 public static final class Instrument {private final BlockingRecord r=new BlockingRecord();public BlockingRecord rk(){return r;}}
 public static final class Event {
  private final Instrument instrument=new Instrument();
  public Instrument ar(){return instrument;}public double D(){return 101;}public double F(){return 1;}
 }
 public static void main(String[] args)throws Exception{
  LiveTapeDataV0319.clear();var active=FeedHookV0319.class.getDeclaredField("active");active.setAccessible(true);active.setBoolean(null,true);
  Thread callback=new Thread(()->FeedHookV0319.trade(new Event()),"test-active-callback");callback.start();
  if(!inside.await(5,TimeUnit.SECONDS))throw new AssertionError("callback not entered");
  CountDownLatch stopEntered=new CountDownLatch(1),stopDone=new CountDownLatch(1);
  Thread stop=new Thread(()->{stopEntered.countDown();FeedHookV0319.stop();LiveTapeDataV0319.clear();stopDone.countDown();},"test-stop");stop.start();stopEntered.await();
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
  while(stop.getState()!=Thread.State.BLOCKED&&stopDone.getCount()!=0&&System.nanoTime()<end)Thread.onSpinWait();
  boolean blocked=stop.getState()==Thread.State.BLOCKED;
  release.countDown();callback.join(5000);stop.join(5000);
  if(!blocked)throw new AssertionError("stop did not wait for in-flight callback");
  if(callback.isAlive()||stop.isAlive())throw new AssertionError("callback/stop deadlock");
  for(String name:new String[]{"quotes","trades","current","watched"}){var f=LiveTapeDataV0319.class.getDeclaredField(name);f.setAccessible(true);if(!((Map<?,?>)f.get(null)).isEmpty())throw new AssertionError("state resurrected after stop: "+name);}
  for(String name:new String[]{"order","watchOrder"}){var f=LiveTapeDataV0319.class.getDeclaredField(name);f.setAccessible(true);if(!((Collection<?>)f.get(null)).isEmpty())throw new AssertionError("queue resurrected after stop: "+name);}
  if(FeedHookV0319.installed())throw new AssertionError("feed still active");
  System.out.println("Concurrent callback/stop passed (9 checks)");
 }
}
