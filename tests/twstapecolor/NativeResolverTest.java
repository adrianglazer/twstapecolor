package twstapecolor;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.objectweb.asm.*;

/** Generated fixtures rename members without shipping any proprietary TWS bytecode. */
public final class NativeResolverTest {
  static int checks;
  static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  static final class Loader extends ClassLoader {
    final Map<String,byte[]> bytes=new HashMap<>();boolean hide;
    Loader(){super(NativeResolverTest.class.getClassLoader());}
    Class<?> define(String name,byte[] code){bytes.put(name.replace('.','/')+".class",code);return defineClass(name,code,0,code.length);}
    public InputStream getResourceAsStream(String name){if(bytes.containsKey(name))return hide?null:new ByteArrayInputStream(bytes.get(name));return super.getResourceAsStream(name);}
  }
  static byte[] getterClass(String name,String fieldType,boolean duplicate,boolean impure,String fieldName){
    ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,name,null,"java/lang/Object",null);
    MethodVisitor ctor=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(0,0);ctor.visitEnd();
    w.visitField(Opcodes.ACC_PRIVATE,fieldName,fieldType,null,null).visitEnd();
    for(String method:duplicate?List.of("renamed","second"):List.of("renamed")){
      MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,method,"()"+fieldType,null,null);m.visitCode();
      if(impure){m.visitTypeInsn(Opcodes.NEW,"java/lang/AssertionError");m.visitInsn(Opcodes.DUP);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/AssertionError","<init>","()V",false);m.visitInsn(Opcodes.ATHROW);}
      else{m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,name,fieldName,fieldType);m.visitInsn(Opcodes.ARETURN);}
      m.visitMaxs(0,0);m.visitEnd();
    }
    w.visitEnd();return w.toByteArray();
  }
  static void rejects(Runnable action,String message){try{action.run();throw new AssertionError(message);}catch(IllegalStateException expected){checks++;}}
  static byte[] hook(boolean duplicate,boolean factory){
    ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,"test/Hook",null,"java/lang/Object",null);
    for(String name:duplicate?List.of("renamed","other"):List.of("renamed")){
      MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|(factory?Opcodes.ACC_STATIC:0),name,"()Ljava/lang/Object;",null,null);
      m.visitCode();m.visitInsn(Opcodes.ACONST_NULL);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(1,factory?0:1);m.visitEnd();
    }
    w.visitEnd();return w.toByteArray();
  }
  public static class PrimaryBook {public Object book(){return null;}}
  public static class InheritedBook extends PrimaryBook {}
  public static class RecordWithWrapper {PrimaryBook primary;InheritedBook wrapper;}
  public static final class Event {
    private final Object instrument;
    Event(Object instrument){this.instrument=instrument;}
    public Object ar(){return instrument;}
    public double D(){return 101;}
    public double F(){return 20;}
  }
  public static void main(String[] args)throws Exception{
    check(NativeResolverV0324.ownsBook(RecordWithWrapper.class.getDeclaredField("primary"),PrimaryBook.class.getMethod("book")),"primary owns the book getter");
    check(!NativeResolverV0324.ownsBook(RecordWithWrapper.class.getDeclaredField("wrapper"),InheritedBook.class.getMethod("book")),"inherited wrapper getter cannot identify primary top");
    Loader loader=new Loader();
    Class<?> renamed=loader.define("jclient.Renamed",getterClass("jclient/Renamed","Ljclient/record/o;",false,false,"changedField"));
    Class<?> renamedEvent=loader.define("feature.tape.FutureEvent",getterClass("feature/tape/FutureEvent","Ljclient/Renamed;",false,false,"instrument"));
    check(NativeResolverV0324.instrument(renamedEvent).getName().equals("renamed"),"renamed native instrument getter follows verified record path");
    Class<?> ambiguousEvent=loader.define("feature.tape.AmbiguousEvent",getterClass("feature/tape/AmbiguousEvent","Ljclient/Renamed;",true,false,"instrument"));
    rejects(()->NativeResolverV0324.instrument(ambiguousEvent),"ambiguous instrument getter rejected");
    Method selected=NativeResolverV0324.record(renamed);
    check(selected.getName().equals("renamed"),"renamed existing-record getter found");
    check(NativeResolverV0324.record(renamed)==selected,"successful resolution cached");
    Class<?> ambiguous=loader.define("jclient.Ambiguous",getterClass("jclient/Ambiguous","Ljclient/record/o;",true,false,"record"));
    rejects(()->NativeResolverV0324.record(ambiguous),"two pure getters must be rejected");
    String before=NativeResolverV0324.status();
    rejects(()->NativeResolverV0324.record(ambiguous),"cached failure must remain rejected");
    check(before.equals(NativeResolverV0324.status()),"failure cached without rescan");
    Class<?> dangerous=loader.define("jclient.Dangerous",getterClass("jclient/Dangerous","Ljclient/record/o;",false,true,"record"));
    rejects(()->NativeResolverV0324.record(dangerous),"impure getter cannot be invoked");
    var neutral=LiveTapeDataV0324.read(new feature.tape.data.k(new Event(dangerous.getConstructor().newInstance())),0);
    check(neutral!=null&&neutral.last()==101&&Double.isNaN(neutral.bid()),"unsupported record preserves visible neutral trade");
    Class<?> top=loader.define("jclient.NewRecord",getterClass("jclient/NewRecord","Ljclient/oQ;",false,false,"renamedTop"));
    check(NativeResolverV0324.top(top).getName().equals("renamedTop"),"top field found by verified book path");
    ClassWriter extra=new ClassWriter(0);
    new ClassReader(getterClass("jclient/AmbiguousTop","Ljclient/oQ;",false,false,"first")).accept(new ClassVisitor(Opcodes.ASM9,extra){
      public void visitEnd(){super.visitField(Opcodes.ACC_PRIVATE,"second","Ljclient/oQ;",null,null).visitEnd();super.visitEnd();}
    },0);
    Class<?> ambiguousTop=loader.define("jclient.AmbiguousTop",extra.toByteArray());
    rejects(()->NativeResolverV0324.top(ambiguousTop),"two top fields rejected without reading values");
    Class<?> unknownBook=loader.define("jclient.UnknownBook",getterClass("jclient/UnknownBook","Ljclient/record/a0;",false,false,"book"));
    rejects(()->NativeResolverV0324.book(unknownBook),"quote-shaped return alone is insufficient");
    Loader missing=new Loader();Class<?> unavailable=missing.define("jclient.Missing",getterClass("jclient/Missing","Ljclient/record/o;",false,false,"r"));missing.hide=true;
    rejects(()->NativeResolverV0324.record(unavailable),"missing bytecode fails closed");
    Loader second=new Loader();Class<?> sameName=second.define("jclient.Renamed",getterClass("jclient/Renamed","Ljclient/record/o;",false,true,"r"));
    rejects(()->NativeResolverV0324.record(sameName),"same class name in another loader is not cached success");
    var original=MethodShapeV0324.read(getterClass("jclient/A","Ljclient/record/o;",false,false,"old"));
    var changed=MethodShapeV0324.read(getterClass("jclient/B","Ljclient/record/o;",false,false,"new"));
    check(original.get(1).hash().equals(changed.get(1).hash()),"symbol renames preserve fingerprint");
    check(!original.get(1).hash().equals(MethodShapeV0324.read(getterClass("jclient/A","Ljclient/record/o;",false,true,"old")).get(1).hash()),"body changes alter fingerprint");
    check(FeedHookV0324.resolveHook(hook(false,true),"()Ljava/lang/Object;",true).equals("renamed"),"renamed factory matched");
    check(FeedHookV0324.resolveHook(hook(false,false),"()Ljava/lang/Object;",false).equals("renamed"),"renamed instance hook matched");
    rejects(()->FeedHookV0324.resolveHook(hook(true,true),"()Ljava/lang/Object;",true),"ambiguous hooks rejected");
    rejects(()->FeedHookV0324.resolveHook(hook(false,true),"()Ljava/lang/Object;",false),"wrong hook staticness rejected");
    System.out.println("Native resolver tests passed ("+checks+" checks)");
  }
}
