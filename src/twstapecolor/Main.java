package twstapecolor;
import com.sun.tools.attach.VirtualMachine;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.Set;
public final class Main {
  static final String VERSION="v0.3.10";
  public static void main(String[] a) throws Exception {
    if(a.length==1 && (a[0].equals("--version")||a[0].equals("version"))){System.out.println("twstapecolor "+VERSION+" (experimental bytecode hook)");return;}
    if(a.length<3 || !a[1].equals("--pid") || !Set.of("test-paint","start","status","stop","dump","probe","report").contains(a[0])) {
      usage();return;
    }
    String ask="#184F31", bid="#66262A";
    boolean debug=false;
    java.util.Set<String> seen=new java.util.HashSet<>();
    for(int i=3;i<a.length;){
      String flag=a[i++];
      if(flag.equals("--debug")) {
        if(debug){usage();return;}
        debug=true;
        continue;
      }
      if(!a[0].equals("start") || !Set.of("--ask-color","--bid-color").contains(flag) || !seen.add(flag) || i>=a.length){usage();return;}
      String value=a[i++];
      if(!value.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Color must be #RRGGBB: "+flag);
      if(flag.equals("--ask-color"))ask=value;else bid=value;
    }
    if(!a[2].matches("[1-9][0-9]*"))throw new IllegalArgumentException("Invalid PID");
    Path out= debug
      ? Path.of(System.getProperty("user.home"),"twstapecolor-"+a[0]+"-"+a[2]+"-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))+".txt")
      : Files.createTempFile("twstapecolor-ipc-", ".tmp");
    try {
      Path jar=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      System.out.println("twstapecolor "+VERSION+" | "+a[0]+" | PID="+a[2]);
      VirtualMachine vm=VirtualMachine.attach(a[2]);
      try{vm.loadAgent(jar.toString(),a[0]+(a[0].equals("start")?"|"+ask+"|"+bid:"")+":"+out);}finally{vm.detach();}
      long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
      while(System.nanoTime()<until){
        if(Files.exists(out)) {
          String txt=Files.readString(out);
          if(txt.contains("STATUS: COMPLETE")){
            if(debug)System.out.println("Report: "+out);
            System.out.print(txt);
            if(txt.contains("ERROR:"))throw new IllegalStateException("Agent returned an error");
            return;
          }
        }
        Thread.sleep(100);
      }
      throw new IllegalStateException("Agent response timed out"+(debug?": "+out:""));
    } finally {
      if(!debug)Files.deleteIfExists(out);
    }
  }
  private static void usage(){
    System.err.println("Usage: ./twstapecolor start --pid PID [--ask-color '#RRGGBB'] [--bid-color '#RRGGBB'] [--debug]");
    System.err.println("       ./twstapecolor status|stop|test-paint|probe|report|dump --pid PID [--debug]");
    System.exit(2);
  }
}
