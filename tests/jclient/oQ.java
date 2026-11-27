package jclient;
/** TWS 10.53 moves the checked book getter to cO().
 * Fixture shares a top base for the record field; calling the old getter still fails. */
public final class oQ extends oR {
  private final jclient.record.a0 book;
  public oQ(jclient.record.a0 book){super(book);this.book=book;}
  public jclient.record.a0 cN(){throw new AssertionError("Wrong cN accessor");}
  public jclient.record.a0 cO(){return book;}
}
