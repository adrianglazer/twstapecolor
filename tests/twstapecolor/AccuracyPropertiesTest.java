package twstapecolor;
import java.math.*;
import java.util.*;
public final class AccuracyPropertiesTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  Random random=new Random(15102026L);
  for(int i=0;i<1000;i++){
   BigDecimal bid=BigDecimal.valueOf(100+random.nextInt(1000000),4);
   BigDecimal ask=bid.add(BigDecimal.valueOf(1+random.nextInt(100),4));
   BigDecimal[] prices={bid.subtract(new BigDecimal("0.0001")),bid,bid.add(ask).divide(new BigDecimal("2")),ask,ask.add(new BigDecimal("0.0001"))};
   for(BigDecimal last:prices){
    int oracle=last.compareTo(ask)>=0?1:last.compareTo(bid)<=0?-1:0;
    check(TapeViewV0318.classify(last.doubleValue(),bid.doubleValue(),ask.doubleValue())==oracle,"decimal oracle mismatch at "+last+" "+bid+"/"+ask);
   }
   check(TapeViewV0318.classify(ask.doubleValue(),bid.doubleValue(),Math.nextUp(ask.doubleValue()))==1,"binary encoding noise at ask");
  }
  for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.MAX_VALUE,-1,0}){
   check(TapeViewV0318.classify(bad,100,101)==0,"invalid trade neutral");
   check(TapeViewV0318.classify(100,bad,101)==0,"invalid bid neutral");
   check(TapeViewV0318.classify(101,100,bad)==0,"invalid ask neutral");
  }
  LiveTapeDataV0318.clear();
  var book=new LiveDataTest.Book(100,101,1,1);var instrument=new LiveDataTest.Instrument(new LiveDataTest.Record(book));
  var first=new LiveDataTest.Event(instrument,101,1);LiveTapeDataV0318.trade(first);
  LiveDataTest.Event last=null;
  for(int i=0;i<100000;i++){last=new LiveDataTest.Event(instrument,101,i+1);LiveTapeDataV0318.trade(last);}
  var field=LiveTapeDataV0318.class.getDeclaredField("trades");field.setAccessible(true);
  check(((Map<?,?>)field.get(null)).size()==4096,"trade cache bounded at 4096");
  var oldest=new feature.tape.data.k(first);var newest=new feature.tape.data.k(last);
  check(TapeViewV0318.snapshot(oldest).get(0).direction()==0,"evicted event neutral");
  check(TapeViewV0318.snapshot(newest).get(0).direction()==1,"latest event retained");
  System.out.println("Accuracy properties passed ("+checks+" checks)");
 }
}
