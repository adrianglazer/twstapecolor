package twstapecolor;
/** Reproduces the accessor name collisions in the installed TWS 10.53 build. */
public final class Tws1053Test {
  public static final class Event {
    final jclient.dz instrument; final double price;
    Event(jclient.dz instrument,double price){this.instrument=instrument;this.price=price;}
    public jclient.dz ar(){return instrument;}
    public double D(){return price;} public double F(){return 10;}
  }
  static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
  public static void main(String[] args){
    LiveTapeDataV0324.clear();
    var book=new jclient.record.a0();
    var record=new jclient.record.o(new jclient.oQ(book));
    var instrument=new jclient.dz(record);
    var old=new Event(instrument,101);
    var model=new feature.tape.data.k(old);
    var rows=TapeViewV0324.snapshot(model);
    check(rows.size()==1&&rows.get(0).direction()==0,"attach preserves historical neutral trade");
    var ask=new Event(instrument,101);LiveTapeDataV0324.trade(ask);model.rows=new Object[]{ask};
    check(TapeViewV0324.snapshot(model).get(0).direction()==1,"10.53 ask trade colored");
    book.bid=110;book.ask=111;LiveTapeDataV0324.quote(record,new Object[0]);
    check(TapeViewV0324.snapshot(model).get(0).direction()==1,"later quote cannot recolor trade");
    var bid=new Event(instrument,110);LiveTapeDataV0324.trade(bid);model.rows=new Object[]{bid};
    check(TapeViewV0324.snapshot(model).get(0).direction()==-1,"10.53 bid trade colored");
    check(LiveTapeDataV0324.status().contains("dataErrors=0"),"no accessor failures");
    LiveTapeDataV0324.clear();
    System.out.println("TWS 10.53 compatibility tests passed");
  }
}
