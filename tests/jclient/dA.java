package jclient;
/** TWS 10.52 reuses rk() for text; q6() returns the existing record. */
public final class dA {
  private final jclient.record.o record;
  public dA(jclient.record.o record){this.record=record;}
  public String rk(){throw new AssertionError("Old rk accessor must not be invoked");}
  public jclient.record.o q6(){return record;}
  public jclient.record.o ss(){throw new AssertionError("Must not request a subscription");}
}
