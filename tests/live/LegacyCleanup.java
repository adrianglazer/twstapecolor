import java.lang.instrument.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;import org.objectweb.asm.*;
/** One-off cleanup of disabled transformers left by pre-v0.3.14 attach ownership bug. */
public final class LegacyCleanup {
 public static void agentmain(String path,Instrumentation inst)throws Exception{
  List<ClassDefinition> definitions=new ArrayList<>();List<String> patched=new ArrayList<>();
  for(Class<?> c:inst.getAllLoadedClasses()){
   if(!Set.of("twstapecolor.HookV0311$2","twstapecolor.HookV0312$2","twstapecolor.HookV0313$2").contains(c.getName()))continue;
   Class<?> hook=Class.forName(c.getName().substring(0,c.getName().indexOf('$')),false,c.getClassLoader());
   Field enabled=hook.getDeclaredField("enabled");enabled.setAccessible(true);if(enabled.getBoolean(null))throw new IllegalStateException("Refusing to alter an active legacy hook: "+hook.getName());
   var bytes=new java.util.concurrent.atomic.AtomicReference<byte[]>();
   ClassFileTransformer capture=new ClassFileTransformer(){public byte[] transform(ClassLoader l,String n,Class<?> t,java.security.ProtectionDomain d,byte[] b){if(t==c)bytes.set(b.clone());return null;}};
   inst.addTransformer(capture,true);try{inst.retransformClasses(c);}finally{inst.removeTransformer(capture);}
   ClassReader reader=new ClassReader(bytes.get());ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
   reader.accept(new ClassVisitor(Opcodes.ASM9,writer){public MethodVisitor visitMethod(int flags,String n,String desc,String sig,String[] ex){
    MethodVisitor mv=super.visitMethod(flags,n,desc,sig,ex);
    if(n.equals("transform")&&desc.equals("(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B")){
     mv.visitCode();mv.visitInsn(Opcodes.ACONST_NULL);mv.visitInsn(Opcodes.ARETURN);mv.visitMaxs(1,6);mv.visitEnd();return null;
    }return mv;
   }},0);
   definitions.add(new ClassDefinition(c,writer.toByteArray()));patched.add(c.getName());
  }
  if(!definitions.isEmpty())inst.redefineClasses(definitions.toArray(ClassDefinition[]::new));
  for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("jtable.basic.by"))inst.retransformClasses(c);
  Files.writeString(Path.of(path),"Disabled stale legacy transformer bodies: "+patched+"\nRetransformed renderer selector without legacy hooks.\n");
 }
}
