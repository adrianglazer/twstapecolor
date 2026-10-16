package twstapecolor;
import java.lang.instrument.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;
public final class AgentV0318 {
  public static void agentmain(String arguments, Instrumentation inst) {
    String[] p=arguments==null?new String[0]:arguments.split(":",2);if(p.length!=2)return;
    Path file=Path.of(p[1]);
    try{
      Files.writeString(file,"twstapecolor v0.3.18\nSTATUS: entered\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
      String result=HookV0318.command(p[0],inst);
      Files.writeString(file,"twstapecolor v0.3.18\n"+result+"\nSTATUS: COMPLETE\n",StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);
    }catch(Throwable e){
      java.io.StringWriter w=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(w));
      try{Files.writeString(file,"twstapecolor v0.3.18\nERROR: "+w+"\nSTATUS: COMPLETE\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);}catch(Throwable no){no.printStackTrace(System.err);}
    }
  }
}
