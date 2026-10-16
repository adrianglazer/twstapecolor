package twstapecolor;
import java.lang.instrument.*;import java.lang.reflect.*;
public final class FailedStopTest {
 public static void main(String[] args)throws Exception{
  int[] retransforms={0};
  Instrumentation inst=(Instrumentation)Proxy.newProxyInstance(FailedStopTest.class.getClassLoader(),new Class[]{Instrumentation.class},(p,m,a)->switch(m.getName()){
   case "isRetransformClassesSupported","isModifiableClass" -> true;
   case "getAllLoadedClasses" -> new Class[]{jtable.basic.by.class};
   case "addTransformer" -> null;
   case "removeTransformer" -> false;
   case "retransformClasses" -> {retransforms[0]++;yield null;}
   default -> throw new UnsupportedOperationException(m.getName());
  });
  HookV0318.command("start|#184F31|#66262A",inst);
  String result=HookV0318.command("stop",inst);
  if(!result.contains("WARNING: renderer transformer could not be removed"))throw new AssertionError("failed removal reported as restored");
  if(result.contains("Original getCellRenderer bytecode restored"))throw new AssertionError("false restoration claim");
  String status=HookV0318.command("status",inst);
  if(!status.contains("enabled=false")||!status.contains("injected=true")||retransforms[0]!=1)throw new AssertionError("failed removal state lost");
  System.out.println("Failed-stop reporting passed (3 checks)");
 }
}
