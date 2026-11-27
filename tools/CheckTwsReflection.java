package twstapecolor;
import java.nio.file.*;
import java.net.*;
import java.util.*;
/** Uses installed class metadata in an isolated loader; never calls TWS methods or constructors. */
public final class CheckTwsReflection {
 public static void main(String[] args)throws Exception{
  if(args.length==0)throw new IllegalArgumentException("Supply installed TWS archive paths");
  for(String archive:args){
   Path jar=Path.of(archive).toAbsolutePath();List<URL> urls=new ArrayList<>();urls.add(jar.toUri().toURL());
   String build=jar.getFileName().toString().split("-")[1];
   Path launcher=jar.getParent().resolve("twslaunch-"+build+".jar");
   if(Files.exists(launcher))urls.add(launcher.toUri().toURL());
   try(var files=Files.list(jar.getParent())){
    for(Path p:files.filter(p->!p.getFileName().toString().startsWith("jts4launch")&&!p.getFileName().toString().matches("twslaunch-[0-9]+[.]jar")&&(p.toString().endsWith(".jar")||p.toString().endsWith(".dat"))).sorted().toList())urls.add(p.toUri().toURL());
   }
   try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){
    Class<?> event=Class.forName("feature.tape.data.a",false,loader);
    var instrument=NativeResolverV0324.instrument(event);
    var record=NativeResolverV0324.record(instrument.getReturnType());
    var top=NativeResolverV0324.top(record.getReturnType());
    var book=NativeResolverV0324.book(top.getType());
    // p is the independently inspected baseline, not used by the automatic resolver.
    if(!top.getName().equals("p"))throw new AssertionError("Wrong primary field: "+top);
    var wrapper=record.getReturnType().getDeclaredField("q");
    var inherited=NativeResolverV0324.book(wrapper.getType());
    if(NativeResolverV0324.ownsBook(wrapper,inherited))throw new AssertionError("Wrapper must not count as primary top");
    for(String name:List.of("com.ib.chart.timesales.Y","com.ib.chart.timesales.Z"))NativeResolverV0324.instrument(Class.forName(name,false,loader));
    for(String name:List.of("feature.tape.ui.aT","feature.tape.data.k"))NativeResolverV0324.row(Class.forName(name,false,loader));
    System.out.println(jar.getFileName()+": REFLECTION PASS instrument="+instrument.getName()+" record="+record.getName()+" top="+top.getName()+" book="+book.getName()+" inheritedWrapperExcluded=true");
   }
  }
 }
}
