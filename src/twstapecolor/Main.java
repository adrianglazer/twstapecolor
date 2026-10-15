package twstapecolor;
import com.sun.tools.attach.VirtualMachine;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.Set;
public final class Main {
  static final String VERSION="v0.3.14";
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
    Path jar=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path logDir=jar.getParent().getParent().resolve("logs");
    if(debug)Files.createDirectories(logDir);
    Path out= debug
      ? logDir.resolve("twstapecolor-"+a[0]+"-"+a[2]+"-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))+".txt")
      : Files.createTempFile(jar.getParent(),"twstapecolor-ipc-", ".tmp");
    try {
      Path agentJar=jar.resolveSibling("twstapecolor-"+VERSION+".jar");
      if(!jar.equals(agentJar))Files.copy(jar,agentJar,StandardCopyOption.REPLACE_EXISTING);
      System.out.println("twstapecolor "+VERSION+" | "+a[0]+" | PID="+a[2]);
      VirtualMachine vm=VirtualMachine.attach(a[2]);
      try{vm.loadAgent(agentJar.toString(),a[0]+(a[0].equals("start")?"|"+ask+"|"+bid:"")+":"+out);}finally{vm.detach();}
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
