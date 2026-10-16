package twstapecolor;
import java.lang.instrument.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;
public final class AgentV0319 {
  public static void agentmain(String arguments, Instrumentation inst) {
    String[] p=arguments==null?new String[0]:arguments.split(":",2);if(p.length!=2)return;
    Path file=Path.of(p[1]);
    try{
      Files.writeString(file,"twstapecolor v0.3.19\nSTATUS: entered\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
      boolean live=p[0].endsWith("|debug");
      String command=live?p[0].substring(0,p[0].length()-6):p[0];
      if(command.startsWith("start")) {
        DebugLogV0319.stop();
        if(live)DebugLogV0319.start(file.resolveSibling(file.getFileName()+"-live"));
        }
      String result=HookV0319.command(command,inst);
      if(command.startsWith("start"))result+="\n"+DebugLogV0319.status();
      Files.writeString(file,"twstapecolor v0.3.19\n"+result+"\nSTATUS: COMPLETE\n",StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);
    }catch(Throwable e){
      DebugLogV0319.stop();
      java.io.StringWriter w=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(w));
      try{Files.writeString(file,"twstapecolor v0.3.19\nERROR: "+w+"\nSTATUS: COMPLETE\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);}catch(Throwable no){no.printStackTrace(System.err);}
    }
  }
}
