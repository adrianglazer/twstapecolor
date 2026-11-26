package jclient;
/** TWS 10.53 name collisions must never invoke unrelated or subscription getters. */
public final class dz {
  private final jclient.record.o record;
  public dz(jclient.record.o record){this.record=record;}
  public Object rk(){throw new AssertionError("Wrong rk accessor");}
  public String q6(){throw new AssertionError("Wrong q6 accessor");}
  public jclient.record.o q8(){return record;}
  public jclient.record.o st(){throw new AssertionError("Must not request a subscription");}
}
