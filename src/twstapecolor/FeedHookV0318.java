package twstapecolor;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;

/** Two narrow hooks: record notifications before observers; completed live trade factory. */
public final class FeedHookV0318 {
  private static Instrumentation instrumentation;
  private static ClassFileTransformer transformer;
  private static Class<?>[] targets=new Class<?>[0];
  private static volatile boolean active;
  private static volatile Throwable failure;
  static boolean installed(){return active;}
  static synchronized void install(Instrumentation inst)throws Exception{
    if(active)return;
    Map<String,Class<?>> found=new HashMap<>();
    for(Class<?> c:inst.getAllLoadedClasses())if(Set.of("jclient.record.o","feature.tape.data.a").contains(c.getName()))found.put(c.getName().replace('.','/'),c);
    if(found.size()!=2)return; // Synthetic/unit-test environments have no TWS feed.
    targets=found.values().toArray(Class<?>[]::new);instrumentation=inst;failure=null;
    transformer=new ClassFileTransformer(){
      @Override public byte[] transform(ClassLoader loader,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
        if(found.get(name)!=type||type==null)return null;
        try{
          ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
          int[] matches={0};
          reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            @Override public MethodVisitor visitMethod(int flags,String method,String desc,String sig,String[] exceptions){
              MethodVisitor base=super.visitMethod(flags,method,desc,sig,exceptions);
              if(name.equals("jclient/record/o")&&method.equals("a")&&desc.equals("(Ljclient/IRecObservable$Scope;Ljclient/IRecObservable$ObserverType;[Ljclient/record/RecordChangeEnum;)V")){
                matches[0]++;
                return new MethodVisitor(Opcodes.ASM9,base){@Override public void visitCode(){super.visitCode();super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ALOAD,3);super.visitMethodInsn(Opcodes.INVOKESTATIC,"twstapecolor/FeedHookV0318","quote","(Ljava/lang/Object;[Ljava/lang/Object;)V",false);}};
              }
              if(name.equals("feature/tape/data/a")&&method.equals("a")&&desc.equals("(Lfeature/timesales/l;)Lfeature/tape/data/a;")){
                matches[0]++;
                return new MethodVisitor(Opcodes.ASM9,base){@Override public void visitInsn(int opcode){if(opcode==Opcodes.ARETURN){super.visitInsn(Opcodes.DUP);super.visitMethodInsn(Opcodes.INVOKESTATIC,"twstapecolor/FeedHookV0318","trade","(Ljava/lang/Object;)V",false);}super.visitInsn(opcode);}};
              }
              return base;
            }
          },0);
          if(matches[0]!=1)throw new IllegalStateException("Expected one data hook in "+name+", found "+matches[0]);
          return writer.toByteArray();
        }catch(Throwable e){failure=e;return null;}
      }
    };
    inst.addTransformer(transformer,true);
    try{inst.retransformClasses(targets);if(failure!=null)throw new IllegalStateException("Feed hook failed",failure);active=true;}
    catch(Throwable e){inst.removeTransformer(transformer);inst.retransformClasses(targets);transformer=null;throw e;}
  }
  public static void quote(Object record,Object[] changes){
    if(!active||!LiveTapeDataV0318.watching(record))return;
    synchronized(FeedHookV0318.class){if(active)try{LiveTapeDataV0318.quote(record,changes);}catch(Throwable ignored){}}
  }
  public static synchronized void trade(Object event){if(active)try{LiveTapeDataV0318.trade(event);}catch(Throwable ignored){}}
  static synchronized String stop(){
    active=false;
    try{
      if(transformer!=null){instrumentation.removeTransformer(transformer);instrumentation.retransformClasses(targets);transformer=null;targets=new Class<?>[0];}
      return "Data hooks restored.";
    }catch(Throwable e){return "WARNING: data-hook restoration failed: "+e+". Restart TWS.";}
  }
}
