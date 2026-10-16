package twstapecolor;
import com.sun.tools.attach.VirtualMachine;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.Set;
public final class Main {
  static final String VERSION="v0.3.20";
  public static void main(String[] a) throws Exception {
    if(a.length==1 && (a[0].equals("--version")||a[0].equals("version"))){System.out.println("twstapecolor "+VERSION+" (experimental bytecode hook)");return;}
    if(a.length<1 || !Set.of("test-paint","start","status","stop","dump","probe","report").contains(a[0])) {
      usage();return;
    }
    String ask="#16834A", bid="#B83B4B", pid=null;
    boolean debug=false;
    java.util.Set<String> seen=new java.util.HashSet<>();
    for(int i=1;i<a.length;){
      String flag=a[i++];
      if(flag.equals("--debug")) {
        if(debug){usage();return;}
        debug=true;
        continue;
      }
      if(flag.equals("--pid")) {
        if(!seen.add(flag)||i>=a.length){usage();return;}
        pid=a[i++];
        if(!pid.matches("[1-9][0-9]*")){System.err.println("Invalid PID: "+pid);System.exit(2);return;}
        continue;
      }
      if(!a[0].equals("start") || !Set.of("--ask-color","--bid-color").contains(flag) || !seen.add(flag) || i>=a.length){usage();return;}
      String value=a[i++];
      if(!value.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Color must be #RRGGBB: "+flag);
      if(flag.equals("--ask-color"))ask=value;else bid=value;
    }
    if(pid==null) {
      try {
        pid=selectTws(VirtualMachine.list().stream().map(vm->new Candidate(vm.id(),vm.displayName())).toList());
      }catch(IllegalStateException failure){System.err.println(failure.getMessage());System.exit(1);return;}
    }
    Path jar=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path logDir=jar.getParent().getParent().resolve("logs");
    if(debug)Files.createDirectories(logDir);
    Path out= debug
      ? logDir.resolve("twstapecolor-"+a[0]+"-"+pid+"-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"))+".txt")
      : Files.createTempFile(jar.getParent(),"twstapecolor-ipc-", ".tmp");
    try {
      Path agentJar=jar.resolveSibling("twstapecolor-"+VERSION+".jar");
      if(!jar.equals(agentJar))Files.copy(jar,agentJar,StandardCopyOption.REPLACE_EXISTING);
      System.out.println("twstapecolor "+VERSION+" | "+a[0]+" | PID="+pid);
      VirtualMachine vm=VirtualMachine.attach(pid);
      try{vm.loadAgent(agentJar.toString(),a[0]+(a[0].equals("start")?"|"+ask+"|"+bid+(debug?"|debug":""):"")+":"+out);}finally{vm.detach();}
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
  record Candidate(String pid,String displayName){}
  static String selectTws(java.util.List<Candidate> vms){
    var matches=vms.stream().filter(vm->{
      String name=vm.displayName()==null?"":vm.displayName().strip();
      return name.equals("install4j.jclient.Launcher")||name.startsWith("install4j.jclient.Launcher ")||name.startsWith("install4j.jclient.Launcher\t");
    }).toList();
    if(matches.isEmpty())throw new IllegalStateException("No running TWS instance found. Start TWS and try again, or specify --pid PID.");
    if(matches.size()>1)throw new IllegalStateException("Multiple running TWS instances found (PIDs: "+String.join(", ",matches.stream().map(Candidate::pid).toList())+"). Specify --pid PID.");
    return matches.get(0).pid();
  }
  private static void usage(){
    System.err.println("Usage: ./twstapecolor start [--pid PID] [--ask-color '#RRGGBB'] [--bid-color '#RRGGBB'] [--debug]");
    System.err.println("       ./twstapecolor status|stop|test-paint|probe|report|dump [--pid PID] [--debug]");
    System.exit(2);
  }
}
