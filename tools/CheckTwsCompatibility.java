package twstapecolor;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Offline inspection only: never loads, initializes or invokes installed TWS classes. */
public final class CheckTwsCompatibility {
  static byte[] bytes(ZipFile zip,String name)throws Exception{
    var entry=zip.getEntry(name+".class");if(entry==null)throw new IllegalStateException("Missing "+name);
    try(var in=zip.getInputStream(entry)){return in.readAllBytes();}
  }
  static void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
  static long instruments(ZipFile zip,byte[] event)throws Exception{
    int count=0;
    for(var shape:MethodShapeV0324.read(event)){
      if(!shape.descriptor().startsWith("()Ljclient/")||!NativeResolverV0324.INSTRUMENT_SHAPES.contains(shape.hash()))continue;
      String type=Type.getReturnType(shape.descriptor()).getInternalName();
      long records=MethodShapeV0324.read(bytes(zip,type)).stream().filter(s->s.descriptor().equals("()Ljclient/record/o;")&&s.fieldRead()&&(s.access()&Opcodes.ACC_PUBLIC)!=0).count();
      if(records==1)count++;
    }
    return count;
  }
  public static void main(String[] args)throws Exception{
    if(args.length==0)throw new IllegalArgumentException("Supply one or more installed TWS archive paths");
    for(String path:args)try(ZipFile zip=new ZipFile(path)){
      byte[] record=bytes(zip,"jclient/record/o");String[] top={null};
      new ClassReader(record).accept(new ClassVisitor(Opcodes.ASM9){
        public FieldVisitor visitField(int flags,String name,String desc,String signature,Object value){if(name.equals("p"))top[0]=Type.getType(desc).getInternalName();return null;}
      },ClassReader.SKIP_CODE);
      check(top[0]!=null,"Known baseline top field absent");
      List<String> fieldTypes=new ArrayList<>();
      new ClassReader(record).accept(new ClassVisitor(Opcodes.ASM9){
        public FieldVisitor visitField(int flags,String name,String desc,String signature,Object value){
          if((flags&Opcodes.ACC_STATIC)==0&&desc.startsWith("Ljclient/"))fieldTypes.add(Type.getType(desc).getInternalName());return null;
        }
      },ClassReader.SKIP_CODE);
      List<String> verifiedTops=new ArrayList<>();
      for(String type:fieldTypes){
        if(zip.getEntry(type+".class")==null)continue;
        if(MethodShapeV0324.read(bytes(zip,type)).stream().filter(s->NativeResolverV0324.BOOK_SHAPES.contains(s.hash())).count()==1)verifiedTops.add(type);
      }
      check(verifiedTops.size()==1&&verifiedTops.get(0).equals(top[0]),"Top-field structural candidates differ from inspected baseline: "+verifiedTops);
      byte[] topBytes=bytes(zip,top[0]);
      var profiles=MethodShapeV0324.read(topBytes).stream().filter(s->NativeResolverV0324.BOOK_SHAPES.contains(s.hash())).toList();
      check(profiles.size()==1,"Expected unique book fingerprint in "+top[0]+", found "+profiles.size());
      var book=profiles.get(0);
      // Rename the getter and all field/method references in-memory. No proprietary bytes are saved.
      ClassWriter changed=new ClassWriter(0);
      new ClassReader(topBytes).accept(new ClassVisitor(Opcodes.ASM9,changed){
        public MethodVisitor visitMethod(int flags,String name,String desc,String signature,String[] exceptions){
          MethodVisitor base=super.visitMethod(flags,"renamed_"+name,desc,signature,exceptions);
          return new MethodVisitor(Opcodes.ASM9,base){
            public void visitFieldInsn(int opcode,String owner,String name,String desc){super.visitFieldInsn(opcode,owner,"field_"+name,desc);}
            public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf){super.visitMethodInsn(opcode,owner,name.startsWith("<")||owner.startsWith("java/")||owner.startsWith("javax/")?name:"method_"+name,desc,itf);}
          };
        }
      },0);
      check(MethodShapeV0324.read(changed.toByteArray()).stream().anyMatch(s->s.name().equals("renamed_"+book.name())&&s.hash().equals(book.hash())),"Book fingerprint did not survive rename");
      var eventShapes=MethodShapeV0324.read(bytes(zip,"com/ib/chart/timesales/Y"));
      check(instruments(zip,bytes(zip,"com/ib/chart/timesales/Y"))==1,"Instrument getter fingerprint ambiguous or missing");
      check(instruments(zip,bytes(zip,"com/ib/chart/timesales/Z"))==1,"Current instrument getter fingerprint ambiguous or missing");
      check(MethodShapeV0324.read(bytes(zip,"jtable/basic/bB")).stream().filter(s->s.descriptor().equals("(II)Ljava/lang/Object;")&&NativeResolverV0324.ROW_SHAPES.contains(s.hash())).count()==1,"Row getter fingerprint ambiguous or missing");
      String instrument=eventShapes.stream().filter(s->s.name().equals("ar")&&s.descriptor().startsWith("()L")).map(s->Type.getReturnType(s.descriptor()).getInternalName()).findFirst().orElseThrow();
      var getters=MethodShapeV0324.read(bytes(zip,instrument)).stream().filter(s->s.descriptor().equals("()Ljclient/record/o;")&&s.fieldRead()&&(s.access()&Opcodes.ACC_PUBLIC)!=0).toList();
      check(getters.size()==1,"Existing-record getter ambiguous or missing");
      String notification=FeedHookV0324.resolveHook(record,"(Ljclient/IRecObservable$Scope;Ljclient/IRecObservable$ObserverType;[Ljclient/record/RecordChangeEnum;)V",false);
      String factory=FeedHookV0324.resolveHook(bytes(zip,"feature/tape/data/a"),"(Lfeature/timesales/l;)Lfeature/tape/data/a;",true);
      System.out.println(Path.of(path).getFileName()+": PASS record="+instrument+"."+getters.get(0).name()+" book="+top[0]+"."+book.name()+" notification="+notification+" factory="+factory+" renamedBook=PASS");
    }
  }
}
