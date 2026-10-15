package twstapecolor;
import java.lang.instrument.*;
import java.lang.reflect.*;
public final class FailedStartTest {
 public static void main(String[] args)throws Exception{
  int[] discovery={0},installed={0},removed={0},retransforms={0};
  Instrumentation inst=(Instrumentation)Proxy.newProxyInstance(FailedStartTest.class.getClassLoader(),new Class[]{Instrumentation.class},(p,m,a)->switch(m.getName()){
   case "isRetransformClassesSupported","isModifiableClass" -> true;
   case "getAllLoadedClasses" -> {if(++discovery[0]==2)throw new IllegalStateException("synthetic feed discovery failure");yield new Class[]{jtable.basic.by.class};}
   case "addTransformer" -> {installed[0]++;yield null;}
   case "removeTransformer" -> {removed[0]++;yield true;}
   case "retransformClasses" -> {retransforms[0]++;yield null;}
   default -> throw new UnsupportedOperationException(m.getName());
  });
  try{HookV0315.command("start|#184F31|#66262A",inst);throw new AssertionError("expected failed start");}catch(IllegalStateException expected){}
  if(installed[0]!=1||removed[0]!=1||retransforms[0]!=2)throw new AssertionError("new renderer hook not rolled back");
  String status=HookV0315.command("status",inst);
  if(!status.contains("injected=false")||!status.contains("enabled=false"))throw new AssertionError("failed start left active state");
  System.out.println("Failed-start rollback passed (3 checks)");
 }
}
