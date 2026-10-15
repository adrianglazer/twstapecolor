package twstapecolor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** Changes shared renderer state only during painting, restoring it in finally. */
final class IsolatedTintRenderer extends JComponent {
  private final Component delegate;
  private final Color tint;
  private final double intensity;
  private final CellRendererPane pane=new CellRendererPane();
  IsolatedTintRenderer(Component delegate,Color tint,double intensity){
    this.delegate=delegate;this.tint=tint;this.intensity=intensity;
    add(pane);
  }
  @Override public Dimension getPreferredSize(){return delegate.getPreferredSize();}
  private record State(Component component,Color background,boolean opaque){}
  private void tint(Component component,List<State> saved){
    Color base=component.getBackground();
    saved.add(new State(component,base,component instanceof JComponent jc&&jc.isOpaque()));
    if(base==null)base=Color.BLACK;
    component.setBackground(new Color(blend(base.getRed(),tint.getRed()),blend(base.getGreen(),tint.getGreen()),blend(base.getBlue(),tint.getBlue())));
    if(component instanceof JComponent jc)jc.setOpaque(true);
    if(component instanceof Container container)for(Component child:container.getComponents())tint(child,saved);
  }
  private int blend(int base,int target){return (int)Math.round(base+(target-base)*Math.clamp(intensity,0,1));}
  @Override protected void paintComponent(Graphics graphics){
    List<State> saved=new ArrayList<>();
    Container parent=delegate.getParent();
    Rectangle bounds=delegate.getBounds();
    int index=parent==null?-1:parent.getComponentZOrder(delegate);
    try{
      tint(delegate,saved);
      graphics.setColor(delegate.getBackground());
      graphics.fillRect(0,0,getWidth(),getHeight());
      pane.paintComponent(graphics,delegate,this,0,0,getWidth(),getHeight(),true);
    }finally{
      for(int i=saved.size()-1;i>=0;i--){
        State state=saved.get(i);
        state.component().setBackground(state.background());
        if(state.component() instanceof JComponent jc)jc.setOpaque(state.opaque());
      }
      pane.remove(delegate);
      if(parent!=null)parent.add(delegate,Math.min(index,parent.getComponentCount()));
      delegate.setBounds(bounds);
    }
  }
}
