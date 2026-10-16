package twstapecolor;
import java.nio.file.*;
import java.util.*;
public final class DiscoveryTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static Main.Candidate vm(String pid,String name){return new Main.Candidate(pid,name);}
 static void fails(List<Main.Candidate> vms,String message){try{Main.selectTws(vms);throw new AssertionError("expected refusal");}catch(IllegalStateException e){check(e.getMessage().contains(message),e.getMessage());}}
 static void cli(int code,String expected,String... args)throws Exception{
  var command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"--add-modules","jdk.attach","-cp",System.getProperty("java.class.path"),"twstapecolor.Main"));Collections.addAll(command,args);
  var process=new ProcessBuilder(command).redirectErrorStream(true).start();String out=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
  check(process.waitFor()==code,"unexpected exit: "+out);check(out.contains(expected),"missing expected output: "+out);
 }
 public static void main(String[] args)throws Exception{
  if(args.length==1&&args[0].equals("live")){
   System.out.println(Main.selectTws(com.sun.tools.attach.VirtualMachine.list().stream().map(v->vm(v.id(),v.displayName())).toList()));return;
  }
  fails(List.of(),"No running TWS instance found");
  fails(List.of(vm("1","install4j.ibgateway.GWClient"),vm("2","unrelated.Main")),"No running TWS instance found");
  check(Main.selectTws(List.of(vm("3","install4j.jclient.Launcher"))).equals("3"),"plain launcher");
  check(Main.selectTws(List.of(vm("4","install4j.jclient.Launcher ${installer:cmdLineArgs}"))).equals("4"),"launcher arguments");
  check(Main.selectTws(List.of(vm("5","  install4j.jclient.Launcher\targuments "))).equals("5"),"whitespace and tab arguments");
  check(Main.selectTws(List.of(vm("6","unrelated.Main"),vm("7","install4j.jclient.Launcher"))).equals("7"),"unrelated JVM excluded");
  fails(List.of(vm("1","install4j.jclient.LauncherFake"),vm("2","other.Main install4j.jclient.Launcher"),vm("3",null),vm("4","")),"No running TWS instance found");
  fails(List.of(vm("10","install4j.jclient.Launcher"),vm("11","install4j.jclient.Launcher args")),"PIDs: 10, 11");
  cli(2,"Usage:");cli(2,"Usage:","unknown");cli(2,"Usage:","start","--pid");
  cli(2,"Invalid PID:","start","--pid","0");cli(2,"Usage:","status","--debug","--debug");
  cli(2,"Usage:","start","--pid","1","--pid","2");cli(2,"Usage:","status","--ask-color","#123456");
  cli(0,Main.VERSION,"--version");
  System.out.println("Discovery tests passed ("+checks+" checks)");
 }
}
