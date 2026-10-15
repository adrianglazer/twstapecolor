package twstapecolor;
import javax.swing.*;
import java.awt.*;
import java.util.*;

public final class LiveDataTest {
  private static int checks;
  private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  public static final class Book {
    double bid,ask,bidSize,askSize;
    Book(double bid,double ask,double bs,double as){this.bid=bid;this.ask=ask;bidSize=bs;askSize=as;}
    public double G(){return bid;}public double F(){return ask;}
    public double aa(){return bidSize;}public double Z(){return askSize;}
  }
  public static final class Top {Book book;Top(Book b){book=b;}public Book cO(){return book;}}
  public static final class Record {private Top p;Record(Book b){p=new Top(b);}}
  public static final class Instrument {Record record;Instrument(Record r){record=r;}public Record rk(){return record;}}
  public static final class Event {
    Instrument instrument;double last,size;
    Event(Instrument i,double p,double s){instrument=i;last=p;size=s;}
    public double D(){return last;}public double F(){return size;}public Instrument ar(){return instrument;}
  }
  public static void main(String[] args)throws Exception{
    LiveTapeDataV0314.clear();
    Book book=new Book(100,101,200,300);Record record=new Record(book);Instrument instrument=new Instrument(record);
    Event old=new Event(instrument,101,10);
    var model=new feature.tape.data.k(old);
    check(LiveTapeDataV0314.supports(model),"recognizes null-cell TWS model");
    check(model.getValueAt(0,1)==null,"fixture reproduces null API values");
    var snapshot=TapeViewV0314.snapshot(model);
    check(snapshot.size()==1&&snapshot.get(0).direction()==0,"old trade must not use attach-time quote");
    Event trade=new Event(instrument,101,100);
    LiveTapeDataV0314.trade(trade);model.rows=new Object[]{trade,old};
    check(TapeViewV0314.snapshot(model).get(0).direction()==1,"incoming ask trade gets preceding captured book");
    book.bid=110;book.ask=111;
    LiveTapeDataV0314.quote(record,new Object[0]); // real TWS notification arrays are empty
    check(TapeViewV0314.snapshot(model).get(0).direction()==1,"future quote cannot recolor old trade");
    Event next=new Event(instrument,110,1);LiveTapeDataV0314.trade(next);model.rows=new Object[]{next,trade,old};
    var latest=TapeViewV0314.snapshot(model);
    check(latest.get(0).direction()==-1,"empty-array notification updates preceding bid");
    check(latest.get(0).intensity()<latest.get(1).intensity(),"actual native trade size sets intensity");
    Instrument other=new Instrument(new Record(new Book(200,201,1,2)));
    Event unrelated=new Event(other,201,1);LiveTapeDataV0314.trade(unrelated);
    check(TapeViewV0314.snapshot(model).get(0).direction()==-1,"instruments isolated");
    book.ask=book.bid;
    LiveTapeDataV0314.quote(record,new Object[0]);
    Event locked=new Event(instrument,110,1);LiveTapeDataV0314.trade(locked);model.rows=new Object[]{locked};
    check(TapeViewV0314.snapshot(model).get(0).direction()==0,"locked snapshot neutral");
    check(Double.isNaN(TapeViewV0314.number(Double.MAX_VALUE)),"TWS missing-price sentinel rejected");
    model.rows=new Object[]{new Event(instrument,Double.MAX_VALUE,1),new Event(instrument,100,0),new Event(instrument,100,1)};
    check(TapeViewV0314.snapshot(model).size()==1,"native empty and quote rows hidden");
    Object[] many=new Object[70];for(int i=0;i<many.length;i++)many[i]=new Event(instrument,100,1);model.rows=many;
    check(TapeViewV0314.snapshot(model).size()==50,"native events limited to 50 trades");
    check(TapeViewV0314.classify(2.36,2.35,2.3600000000000003)==1,"decimal quote rounding at ask");
    check(TapeViewV0314.classify(2.355,2.35,2.3600000000000003)==0,"inside spread remains neutral");
    book.bid=109;book.ask=110;LiveTapeDataV0314.quote(record,new Object[0]);
    record.p=null;
    LiveTapeDataV0314.quote(record,new Object[0]);
    Event disconnected=new Event(instrument,110,1);LiveTapeDataV0314.trade(disconnected);model.rows=new Object[]{disconnected};
    check(TapeViewV0314.snapshot(model).get(0).direction()==0,"missing live top invalidates the old quote");
    LiveTapeDataV0314.clear();
    for(int i=0;i<90;i++)LiveTapeDataV0314.trade(new Event(new Instrument(new Record(new Book(10,11,1,1))),11,1));
    var currentField=LiveTapeDataV0314.class.getDeclaredField("current");currentField.setAccessible(true);
    check(((java.util.Map<?,?>)currentField.get(null)).size()<=64,"current-trade cache respects record bound");
    LiveTapeDataV0314.clear();model.rows=new Object[]{trade};
    check(TapeViewV0314.snapshot(model).get(0).direction()==0,"stop clears captured trade history");
    check(LiveTapeDataV0314.status().contains("dataErrors=0"),"native accessors error free");
    System.out.println("Live-data tests passed ("+checks+" checks)");
  }
}
