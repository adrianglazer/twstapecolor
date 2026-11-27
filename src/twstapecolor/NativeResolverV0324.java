package twstapecolor;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import org.objectweb.asm.Type;

/** Conservative, class-loader-scoped discovery. Never invokes a candidate during resolution. */
final class NativeResolverV0324 {
  static final Set<String> BOOK_SHAPES=Set.of(
    "a45cd5c37a4af37970b6ba36ac73684fec701cd5c10521b9961e9fd7c580e329", // 10.51
    "add79fa15e91151bb4ee446193b6f5ba9d1c1214fee13fd3200f7de41327f9e7", // 10.52
    "7fcf91c591b22e78770523e56f452e73125bbd845ec1ea63acbb03af8f44fabb"  // 10.53
  );
  static final Set<String> INSTRUMENT_SHAPES=Set.of(
    "36ab41b5cbb3b85e16ce7b5d9bca4d71cef848c3ec5caa0911c66179bd8b7e59",
    "8714efbefe1001e0534e2b850f8470f248c5ff4d05f550efab5666de80e88dfa",
    "431b823a317dfb8ddaeb22cf462aac1eb687523a323d8eb7ee44cb2652d02645",
    "93e0d27668d7e0be58531092373de1a5b4d2509dba93900fe4372657050c52b0");
  static final Set<String> ROW_SHAPES=Set.of("98d064336b120f43140fdd426c9c5429a5d62dc4bdfd2d7eb977668c00f03402");
  private static final class Resolution<T> {
    private final T value;private final String error;private final AtomicBoolean reported=new AtomicBoolean();
    Resolution(T value,String error){this.value=value;this.error=error;}
    T value(){return value;}
    T require(){
      if(error!=null){if(reported.compareAndSet(false,true)){failures.incrementAndGet();last=error;}throw new IllegalStateException(error);}
      return value;
    }
  }
  private static final AtomicLong scans=new AtomicLong(),failures=new AtomicLong();
  private static volatile String last="none";
  private interface Find<T>{T find(Class<?> type)throws ReflectiveOperationException;}
  private static <T> ClassValue<Resolution<T>> cache(String role,Find<T> find){
    return new ClassValue<>(){
      protected Resolution<T> computeValue(Class<?> type){
        scans.incrementAndGet();
        try{T value=find.find(type);last=role+"="+value;return new Resolution<>(value,null);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError e){
          String error="Native resolver "+role+" for "+type.getName()+": "+e;
          return new Resolution<>(null,error);
        }
      }
    };
  }
  private static final ClassValue<Resolution<Method>> RECORD=cache("record",NativeResolverV0324::findRecord);
  private static final ClassValue<Resolution<Method>> BOOK=cache("book",NativeResolverV0324::findBook);
  private static final ClassValue<Resolution<Field>> TOP=cache("top",NativeResolverV0324::findTop);
  private static final ClassValue<Resolution<Method>> INSTRUMENT=cache("instrument",NativeResolverV0324::findInstrument);
  private static final ClassValue<Resolution<Method>> ROW=cache("row",NativeResolverV0324::findRow);
  static Method instrument(Class<?> type){return INSTRUMENT.get(type).require();}
  static Method row(Class<?> type){return ROW.get(type).require();}
  static Method record(Class<?> type){return RECORD.get(type).require();}
  static Method book(Class<?> type){return BOOK.get(type).require();}
  static Field top(Class<?> type){return TOP.get(type).require();}
  static String status(){return "resolverScans="+scans+" resolverFailures="+failures+" resolverLast=["+last+"]";}
  private static <T> T unique(List<T> candidates,String role){
    if(candidates.size()!=1)throw new IllegalStateException(role+": expected one verified candidate, found "+candidates.size()+" "+candidates);
    return candidates.get(0);
  }
  private static boolean instanceGetter(Method m){return Modifier.isPublic(m.getModifiers())&&!Modifier.isStatic(m.getModifiers())&&!m.isBridge()&&m.getParameterCount()==0;}
  private static Method accessible(Method m){m.setAccessible(true);return m;}
  private static boolean matches(Method m,Set<String> profiles){
    return MethodShapeV0324.read(m.getDeclaringClass()).stream().anyMatch(s->s.name().equals(m.getName())&&s.descriptor().equals(Type.getMethodDescriptor(m))&&profiles.contains(s.hash()));
  }
  private static Method findInstrument(Class<?> type)throws ReflectiveOperationException{
    if(!type.getName().startsWith("com.ib.chart.timesales.")&&!type.getName().startsWith("feature.tape."))return accessible(type.getMethod("ar"));
    List<Method> candidates=new ArrayList<>();
    for(Method m:type.getMethods()){
      if(instanceGetter(m)&&m.getReturnType().getName().startsWith("jclient.")&&matches(m,INSTRUMENT_SHAPES)&&RECORD.get(m.getReturnType()).value()!=null)candidates.add(m);
    }
    return accessible(unique(candidates,"instrument getter fingerprint and record path"));
  }
  private static Method findRow(Class<?> type)throws ReflectiveOperationException{
    try{
      Method known=type.getMethod("e",int.class,int.class);
      if(!Modifier.isStatic(known.getModifiers())&&known.getReturnType()==Object.class)return accessible(known);
    }catch(NoSuchMethodException ignored){}
    List<Method> candidates=new ArrayList<>();
    for(Method m:type.getMethods()){
      if(!Modifier.isStatic(m.getModifiers())&&Type.getMethodDescriptor(m).equals("(II)Ljava/lang/Object;")&&matches(m,ROW_SHAPES))candidates.add(m);
    }
    return accessible(unique(candidates,"native row getter fingerprint"));
  }
  private static Method findRecord(Class<?> type)throws ReflectiveOperationException{
    if(!type.getName().startsWith("jclient.")){
      Method m=type.getMethod("rk");m.getReturnType().getDeclaredField("p");return accessible(m); // ordinary synthetic adapter
    }
    List<Method> candidates=new ArrayList<>();
    Map<Class<?>,List<MethodShapeV0324.Shape>> shapes=new HashMap<>();
    for(Method m:type.getMethods()){
      if(!instanceGetter(m)||!m.getReturnType().getName().equals("jclient.record.o"))continue;
      for(var shape:shapes.computeIfAbsent(m.getDeclaringClass(),MethodShapeV0324::read)){
        if(shape.name().equals(m.getName())&&shape.descriptor().equals(Type.getMethodDescriptor(m))&&shape.fieldRead())candidates.add(m);
      }
    }
    return accessible(unique(candidates,"existing-record field getter"));
  }
  private static boolean quoteType(Class<?> type){
    try{
      for(String name:List.of("G","F")){
        Method m=type.getMethod(name);if(!instanceGetter(m)||m.getReturnType()!=double.class)return false;
      }
      for(String name:List.of("aa","Z")){
        Method m=type.getMethod(name);if(!instanceGetter(m)||!(m.getReturnType()==double.class||Number.class.isAssignableFrom(m.getReturnType())||m.getReturnType().getName().equals("jutils.a")))return false;
      }
      return true;
    }catch(NoSuchMethodException|LinkageError e){return false;}
  }
  private static Method findBook(Class<?> type)throws ReflectiveOperationException{
    if(!type.getName().startsWith("jclient."))return accessible(type.getMethod("cO"));
    // Known signatures retain support for builds already inspected. No trial calls.
    String[][] known=switch(type.getName()){
      case "jclient.oR" -> new String[][]{{"cO","jclient.record.aZ"},{"cN","jclient.record.a0"}};
      case "jclient.oQ" -> new String[][]{{"cO","jclient.record.a0"}};
      default -> new String[0][];
    };
    List<Method> matches=new ArrayList<>();
    for(String[] pair:known){try{
      Method m=type.getMethod(pair[0]);
      if(instanceGetter(m)&&m.getReturnType().getName().equals(pair[1])&&quoteType(m.getReturnType()))matches.add(m);
    }catch(NoSuchMethodException ignored){}}
    if(!matches.isEmpty())return accessible(unique(matches,"known book getter"));
    Map<Class<?>,List<MethodShapeV0324.Shape>> shapes=new HashMap<>();
    for(Method m:type.getMethods()){
      if(!instanceGetter(m)||!quoteType(m.getReturnType()))continue;
      for(var shape:shapes.computeIfAbsent(m.getDeclaringClass(),MethodShapeV0324::read)){
        if(shape.name().equals(m.getName())&&shape.descriptor().equals(Type.getMethodDescriptor(m))&&BOOK_SHAPES.contains(shape.hash()))matches.add(m);
      }
    }
    return accessible(unique(matches,"book instruction fingerprint"));
  }
  static boolean ownsBook(Field field,Method getter){return field.getType()==getter.getDeclaringClass();}
  private static Field findTop(Class<?> type)throws ReflectiveOperationException{
    if(!type.getName().startsWith("jclient.")){
      Field f=type.getDeclaredField("p");f.setAccessible(true);return f;
    }
    List<Field> fields=new ArrayList<>();
    for(Field f:type.getDeclaredFields()){
      if(Modifier.isStatic(f.getModifiers()))continue;
      if(!f.getType().getName().startsWith("jclient."))continue;
      Method getter=BOOK.get(f.getType()).value();
      // A wrapper subclass can inherit the same getter while representing another
      // quote view. Only the type declaring the verified getter identifies the top.
      if(getter!=null&&ownsBook(f,getter))fields.add(f);
    }
    Field field=unique(fields,"existing top-record field");field.setAccessible(true);return field;
  }
}
