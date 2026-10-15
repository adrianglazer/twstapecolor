package twstapecolor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.instrument.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import org.objectweb.asm.*;

public final class TapeTest {
  private static int checks;
  private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  private static DefaultTableModel model(Object[][] rows){return new DefaultTableModel(rows,new Object[]{"Last","Last Size","Bid","Ask"});}
  private static void classification(){
    // Newest first: a newer quote must never classify the older trade below it.
    var m=model(new Object[][]{
      {null,null,110,111},{102,20,null,null},{null,null,101,102},
      {100,2,null,null},{null,null,100,101},{100.5,5,null,null},
      {null,null,100,101},{101,1,null,null}
    });
    var p=TapeViewV0314.snapshot(m);
    check(p.size()==4,"quote rows hidden");
    check(p.get(1).direction()==1,"nearest preceding quote, not future quote");
    check(p.get(3).direction()==-1,"bid classification");
    check(p.get(5).direction()==0,"inside spread neutral");
    check(p.get(7).direction()==0,"missing preceding quote neutral");
    check(p.get(1).intensity()==1,"largest trade full intensity");
    check(p.get(3).intensity()<p.get(1).intensity(),"size adaptive intensity");
    check(TapeViewV0314.classify(100,100,100)==0,"locked market neutral");
    check(TapeViewV0314.classify(100,102,101)==0,"crossed market neutral");
    check(TapeViewV0314.classify(103,100,101)==1,"above ask");
    check(TapeViewV0314.classify(99,100,101)==-1,"below bid");
    for(Object quote:new Object[]{"bad", "100 x 100", "102 x 101", "100 x NaN"}){
      var pair=new DefaultTableModel(new Object[][]{{101,1,null},{null,null,quote},{null,null,"100 x 101"}},new Object[]{"Last","Size","NBBO"});
      check(TapeViewV0314.snapshot(pair).get(0).direction()==0,"invalid quote does not reuse stale NBBO");
    }
    for(String quote:new String[]{"100 x 101","100/101","100 × 101","100 - 101"}){
      var pair=new DefaultTableModel(new Object[][]{{101,1,null},{null,null,quote}},new Object[]{"Last","Size","NBBO"});
      check(TapeViewV0314.snapshot(pair).get(0).direction()==1,"NBBO pair parsing");
    }
    var invalid=model(new Object[][]{{101,null,null,null},{null,1,null,null},{101,0,null,null},{Double.NaN,1,null,null},{101,1,null,null},{null,null,100,null},{null,null,100,101}});
    check(TapeViewV0314.snapshot(invalid).size()==1,"require valid Last and Last Size");
    check(TapeViewV0314.snapshot(invalid).get(4).direction()==0,"partial quote invalidates older complete quote");
    var simultaneous=model(new Object[][]{{101,1,200,201},{null,null,100,101}});
    check(TapeViewV0314.snapshot(simultaneous).get(0).direction()==1,"same event quote cannot classify own trade");
    Object[][] many=new Object[2000][4];
    for(int i=0;i<60;i++){many[2*i]=new Object[]{101,i+1,null,null};many[2*i+1]=new Object[]{null,null,100,101};}
    var bounded=new DefaultTableModel(many,new Object[]{"Last","Size","Bid","Ask"}){
      @Override public Object getValueAt(int r,int c){if(r>100)throw new AssertionError("unneeded older history read");return super.getValueAt(r,c);}
    };
    var limit=TapeViewV0314.snapshot(bounded);
    check(limit.size()==50&&!limit.containsKey(100),"newest 50 trades only");
    check(limit.values().stream().allMatch(x->x.direction()==1),"quotes outside view still classify");
    var reordered=new DefaultTableModel(new Object[][]{{101,null,1},{null,"100 x 101",null}},new Object[]{"LAST","NBBO","Last Size"});
    check(TapeViewV0314.snapshot(reordered).get(0).direction()==1,"model column order independent");
  }
  private static void paint(){
    JLabel label=new JLabel("print");label.setOpaque(false);label.setBackground(Color.BLACK);label.setForeground(Color.WHITE);
    JPanel nested=new JPanel();nested.setBackground(Color.BLUE);nested.add(label);
    CellRendererPane originalParent=new CellRendererPane();originalParent.add(nested);
    var tint=new IsolatedTintRenderer(nested,Color.GREEN,1);
    tint.setSize(120,25);
    BufferedImage image=new BufferedImage(120,25,BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics=image.createGraphics();
    try{tint.paint(graphics);}finally{graphics.dispose();}
    check(image.getRGB(1,1)==Color.GREEN.getRGB(),"full cell background painted");
    check(nested.getBackground().equals(Color.BLUE)&&label.getBackground().equals(Color.BLACK),"nested shared backgrounds restored");
    check(!label.isOpaque()&&label.getForeground().equals(Color.WHITE),"opacity and text restored");
    check(nested.getParent()==originalParent,"shared renderer parent restored");
    JLabel broken=new JLabel(){@Override protected void paintComponent(Graphics g){throw new IllegalStateException("paint failure");}};
    broken.setBackground(Color.BLACK);broken.setOpaque(false);
    var failure=new IsolatedTintRenderer(broken,Color.RED,1);failure.setSize(10,10);
    Graphics2D g=image.createGraphics();
    try{failure.paint(g);throw new AssertionError("expected paint failure");}catch(IllegalStateException expected){}finally{g.dispose();}
    check(broken.getBackground().equals(Color.BLACK)&&!broken.isOpaque(),"restored on paint exception");
  }
  private static feature.tape.ui.aN table,hookTable;
  private static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
  private static void filtering()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      Object[][] rows=new Object[120][4];
      for(int i=0;i<60;i++){rows[2*i]=new Object[]{101,i+1,null,null};rows[2*i+1]=new Object[]{null,null,100,101};}
      table=new feature.tape.ui.aN(new feature.tape.ui.aT(rows));
      // Hide quote columns; classification uses model data, not visible renderers.
      table.removeColumn(table.getColumnModel().getColumn(3));
      table.removeColumn(table.getColumnModel().getColumn(2));
      TapeViewV0314.schedule(table,()->true);
    });flush();
    SwingUtilities.invokeAndWait(()->{
      check(table.getRowCount()==50&&table.getModel().getRowCount()==120,"filter preserves all model events");
      check(TapeViewV0314.print(table,0).direction()==1,"hidden quote columns classify");
      ((DefaultTableModel)table.getModel()).insertRow(0,new Object[]{101,500,null,null});
      check(TapeViewV0314.print(table,0).direction()==0,"invalidates snapshot immediately");
    });flush();
    SwingUtilities.invokeAndWait(()->{
      check(table.getRowCount()==50&&table.getValueAt(0,1).equals(500),"insert updates newest 50");
      ((DefaultTableModel)table.getModel()).setValueAt(null,0,1);
    });flush();
    SwingUtilities.invokeAndWait(()->{
      check(table.getRowCount()==50&&!Objects.equals(table.getValueAt(0,1),500),"updated incomplete row hidden");
      TapeViewV0314.restoreAll();
      check(table.getRowSorter()==null&&table.getRowCount()==121,"stop restores unfiltered view");
      TapeViewV0314.schedule(table,()->true);
    });flush();
    SwingUtilities.invokeAndWait(()->{
      check(table.getRowCount()==50,"restart reinstalls filter");
      table.setModel(new feature.tape.ui.aT(new Object[][]{{101,1,null,null},{null,null,100,101}}));
      check(table.getRowSorter()==null,"model replacement removes old view");
      TapeViewV0314.schedule(table,()->true);
    });flush();
    SwingUtilities.invokeAndWait(()->{
      check(table.getRowCount()==1,"replacement model filtered");
      TapeViewV0314.restoreAll();
      var sorter=new TableRowSorter<>(table.getModel());table.setRowSorter(sorter);
      TapeViewV0314.schedule(table,()->true);
    });flush();
    SwingUtilities.invokeAndWait(()->{
      check(table.getRowCount()==2,"application sorter preserved");
      table.setRowSorter(null);
      TapeViewV0314.schedule(table,()->false);
    });flush();
    SwingUtilities.invokeAndWait(()->check(table.getRowSorter()==null,"pending task respects stop"));
  }
  private static void verifier()throws Exception{
    final ClassFileTransformer[] transformer={null};
    final byte[][] transformed={null};
    final int[] installs={0},removals={0};
    Instrumentation inst=(Instrumentation)Proxy.newProxyInstance(TapeTest.class.getClassLoader(),new Class[]{Instrumentation.class},(proxy,method,args)->switch(method.getName()){
      case "isRetransformClassesSupported","isModifiableClass" -> true;
      case "getAllLoadedClasses" -> new Class[]{jtable.basic.by.class};
      case "addTransformer" -> {transformer[0]=(ClassFileTransformer)args[0];installs[0]++;yield null;}
      case "removeTransformer" -> {transformer[0]=null;removals[0]++;yield true;}
      case "retransformClasses" -> {
        if(transformer[0]!=null)transformed[0]=transformer[0].transform(TapeTest.class.getClassLoader(),"jtable/basic/by",jtable.basic.by.class,null,deadArgumentFixture());
        yield null;
      }
      default -> throw new UnsupportedOperationException(method.getName());
    });
    HookV0314.command("start|#184F31|#66262A",inst);
    byte[] bytes=transformed[0];check(bytes!=null,"transformation emits bytecode");
    // Define and execute with -Xverify:all. The ARETURN frame deliberately has TOP
    // in slots 1 and 2, reproducing the original VerifyError trigger.
    Class<?> verified=new ClassLoader(TapeTest.class.getClassLoader()){
      Class<?> loadFixture(){return defineClass("jtable.basic.by",bytes,0,bytes.length);}
    }.loadFixture();
    Object receiver=verified.getConstructor().newInstance();
    check(verified.getMethod("getCellRenderer",int.class,int.class).invoke(receiver,0,0)==null,"dead argument frame verifies and executes");
    SwingUtilities.invokeAndWait(()->{
      var target=new feature.tape.ui.aN(new feature.tape.ui.aT(new Object[][]{{101,1,null,null},{null,null,100,101}}));
      hookTable=target;
      var shared=new DefaultTableCellRenderer();
      var first=HookV0314.wrap(shared,target);
      var second=HookV0314.wrap(first,target);
      check(first!=second,"unwrap existing renderer before wrapping");
      check(HookV0314.wrap(shared,new JTable())==shared,"non tape tables untouched");
      check(second.getTableCellRendererComponent(target,101,true,false,0,0)==shared,"selection unchanged");
    });flush();
    SwingUtilities.invokeAndWait(()->{
      check(hookTable.getRowCount()==1,"hook schedules trade filtering");
      var shared=new DefaultTableCellRenderer();
      shared.setBackground(Color.BLACK);
      for(int col=0;col<hookTable.getColumnCount();col++){
        var wrapped=HookV0314.wrap(shared,hookTable);
        Component rendered=wrapped.getTableCellRendererComponent(hookTable,hookTable.getValueAt(0,col),false,false,0,col);
        check(rendered instanceof IsolatedTintRenderer,"NBBO tint wraps every column");
        check(shared.getBackground().equals(Color.BLACK),"renderer lookup leaves shared colors untouched");
      }
    });
    // Each real attach supplies a different Instrumentation instance. A status
    // command must never replace the instance that owns the registered transformer.
    Instrumentation otherInst=(Instrumentation)Proxy.newProxyInstance(TapeTest.class.getClassLoader(),new Class[]{Instrumentation.class},(p,m,a)->{
      if(m.getName().equals("removeTransformer")||m.getName().equals("retransformClasses"))throw new AssertionError("used a later attach's instrumentation instead of the transformer owner");
      return null;
    });
    HookV0314.command("status",otherInst);
    HookV0314.command("stop",otherInst);
    SwingUtilities.invokeAndWait(()->check(hookTable.getRowSorter()==null&&hookTable.getRowCount()==2,"stop cleans hook view"));
    HookV0314.command("start|#184F31|#66262A",inst);
    check(installs[0]==2&&removals[0]==1,"restart has one transformer");
    HookV0314.command("stop",inst);
    check(removals[0]==2,"second stop removes transformer");
  }
  private static byte[] deadArgumentFixture(){
    ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,"jtable/basic/by",null,"javax/swing/JTable",null);
    MethodVisitor init=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(Opcodes.ALOAD,0);init.visitMethodInsn(Opcodes.INVOKESPECIAL,"javax/swing/JTable","<init>","()V",false);init.visitInsn(Opcodes.RETURN);init.visitMaxs(1,1);init.visitEnd();
    MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"getCellRenderer","(II)Ljavax/swing/table/TableCellRenderer;",null,null);
    m.visitCode();org.objectweb.asm.Label end=new org.objectweb.asm.Label();m.visitJumpInsn(Opcodes.GOTO,end);m.visitLabel(end);
    m.visitFrame(Opcodes.F_FULL,1,new Object[]{"jtable/basic/by"},0,null);
    m.visitInsn(Opcodes.ACONST_NULL);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(1,3);m.visitEnd();w.visitEnd();return w.toByteArray();
  }
  public static void main(String[] args)throws Exception{
    classification();SwingUtilities.invokeAndWait(TapeTest::paint);filtering();verifier();
    System.out.println("Tape tests passed ("+checks+" checks)");
  }
}
