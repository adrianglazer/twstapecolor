package twstapecolor;

import java.io.*;
import java.security.*;
import java.util.*;
import org.objectweb.asm.*;

/** Instruction fingerprints ignore obfuscated symbols, but retain aliasing, control flow and constants. */
final class MethodShapeV0324 {
  record Shape(String name,String descriptor,int access,String hash,boolean fieldRead){}
  static List<Shape> read(Class<?> type){
    String resource="/"+type.getName().replace('.','/')+".class";
    try(InputStream in=type.getResourceAsStream(resource)){
      if(in==null)throw new IOException("Class bytes unavailable: "+type.getName());
      byte[] bytes=in.readNBytes(8*1024*1024+1);
      if(bytes.length>8*1024*1024)throw new IOException("Class too large: "+type.getName());
      if(!new ClassReader(bytes).getClassName().equals(type.getName().replace('.','/')))throw new IOException("Class resource mismatch");
      return read(bytes);
    }catch(IOException|RuntimeException e){throw new IllegalStateException("Cannot inspect "+type.getName(),e);}
  }
  static List<Shape> read(byte[] bytes){
    List<Shape> result=new ArrayList<>();
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
      public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
        return new MethodVisitor(Opcodes.ASM9){
          final StringBuilder code=new StringBuilder();
          final Map<String,Integer> symbols=new LinkedHashMap<>();
          final Map<Label,Integer> labels=new IdentityHashMap<>();
          final List<String> simple=new ArrayList<>();
          String symbol(String s){return "s"+symbols.computeIfAbsent(s,k->symbols.size());}
          String owner(String s){return s.startsWith("java/")||s.startsWith("javax/")?s:symbol("class:"+s);}
          String descriptor(String s){StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++){
            char c=s.charAt(i);b.append(c);if(c=='L'){int end=s.indexOf(';',i);b.append(owner(s.substring(i+1,end))).append(';');i=end;}
          }return b.toString();}
          String member(String owner,String name,String desc){return owner(owner)+":"+(owner.startsWith("java/")||owner.startsWith("javax/")||name.startsWith("<")?name:symbol(owner+"."+name+desc))+descriptor(desc);}
          int label(Label l){return labels.computeIfAbsent(l,k->labels.size());}
          void add(String s){code.append(s).append('\n');}
          public void visitInsn(int op){add("i"+op);simple.add("i"+op);}
          public void visitVarInsn(int op,int var){add("v"+op+":"+var);simple.add("v"+op+":"+var);}
          public void visitFieldInsn(int op,String owner,String name,String desc){add("f"+op+":"+member(owner,name,desc));simple.add(op==Opcodes.GETFIELD&&desc.startsWith("L")?"field":"other");}
          public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){add("m"+op+":"+member(owner,name,desc)+":"+itf);simple.add("other");}
          public void visitTypeInsn(int op,String type){add("t"+op+":"+owner(type));simple.add("other");}
          public void visitIntInsn(int op,int value){add("n"+op+":"+value);simple.add("other");}
          public void visitJumpInsn(int op,Label target){add("j"+op+":"+label(target));simple.add("other");}
          public void visitLabel(Label l){add("l"+label(l));}
          public void visitLdcInsn(Object value){add("c"+value.getClass().getName()+":"+value);simple.add("other");}
          public void visitIincInsn(int var,int increment){add("inc"+var+":"+increment);simple.add("other");}
          public void visitTryCatchBlock(Label start,Label end,Label handler,String type){add("catch"+label(start)+":"+label(end)+":"+label(handler)+":"+(type==null?"*":owner(type)));simple.add("other");}
          public void visitTableSwitchInsn(int min,int max,Label dflt,Label... targets){add("switch"+min+":"+max+":"+label(dflt));for(Label l:targets)add("case"+label(l));simple.add("other");}
          public void visitLookupSwitchInsn(Label dflt,int[] keys,Label[] targets){add("lookup"+label(dflt));for(int i=0;i<keys.length;i++)add(keys[i]+":"+label(targets[i]));simple.add("other");}
          public void visitMultiANewArrayInsn(String desc,int dims){add("array"+descriptor(desc)+":"+dims);simple.add("other");}
          public void visitInvokeDynamicInsn(String name,String desc,Handle bootstrap,Object... args){add("dynamic"+name+desc+bootstrap+Arrays.toString(args));simple.add("other");}
          public void visitEnd(){
            try{String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
              result.add(new Shape(name,desc,access,hash,simple.equals(List.of("v25:0","field","i176"))));
            }catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
          }
        };
      }
    },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
    return List.copyOf(result);
  }
}
