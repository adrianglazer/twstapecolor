package twstapecolor;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;
import java.util.*;
import javax.swing.*;

/** Paint-only tint: never changes a shared native renderer's colors or parent. */
final class TintRendererV0319 extends JComponent {
  private Component delegate;
  private Color tint;
  private double intensity;
  TintRendererV0319(){}
  TintRendererV0319(Component delegate,Color tint,double intensity){configure(delegate,tint,intensity);}
  TintRendererV0319 configure(Component delegate,Color tint,double intensity){
    this.delegate=delegate;this.tint=tint;this.intensity=Math.clamp(intensity,0,1);return this;
  }
  @Override public Dimension getPreferredSize(){return delegate.getPreferredSize();}
  private Color blend(Color base){
    if(base==null)base=Color.BLACK;
    return new Color((int)Math.round(base.getRed()+(tint.getRed()-base.getRed())*intensity),
      (int)Math.round(base.getGreen()+(tint.getGreen()-base.getGreen())*intensity),
      (int)Math.round(base.getBlue()+(tint.getBlue()-base.getBlue())*intensity));
  }
  private void backgrounds(Component component,Map<Color,Color> colors){
    Color base=component.getBackground();colors.put(base==null?Color.BLACK:base,blend(base));
    if(component instanceof Container container)for(Component child:container.getComponents())backgrounds(child,colors);
  }
  @Override protected void paintComponent(Graphics graphics){
    Map<Color,Color> colors=new HashMap<>();backgrounds(delegate,colors);
    Rectangle bounds=delegate.getBounds();
    Graphics copy=graphics.create();
    try{
      copy.setColor(blend(delegate.getBackground()));copy.fillRect(0,0,getWidth(),getHeight());
      delegate.setBounds(0,0,getWidth(),getHeight());
      // Renderer validation/layout and parent ownership remain TWS's concern.
      // Intercept background fills in this paint's graphics only. In particular,
      // do not call native setBackground/setOpaque: their listeners can affect L2.
      delegate.paint(new BackgroundGraphics(copy,colors));
    }finally{copy.dispose();delegate.setBounds(bounds);}
  }

  /** Graphics2D keeps native text/icon rendering intact; only background fills change. */
  private static final class BackgroundGraphics extends Graphics2D {
    private final Graphics2D g;
    private final Map<Color,Color> colors;
    BackgroundGraphics(Graphics graphics,Map<Color,Color> colors){this.g=(Graphics2D)graphics;this.colors=colors;}
    private void fill(Runnable draw){
      Paint original=g.getPaint();Color replacement=original instanceof Color c?colors.get(c):null;
      if(replacement==null){draw.run();return;}
      try{g.setColor(replacement);draw.run();}finally{g.setPaint(original);}
    }
    @Override public Graphics create(){return new BackgroundGraphics(g.create(),colors);}
    @Override public void dispose(){g.dispose();}
    @Override public void fillRect(int x,int y,int w,int h){fill(()->g.fillRect(x,y,w,h));}
    @Override public void fill(Shape s){fill(()->g.fill(s));}
    @Override public void clearRect(int x,int y,int w,int h){Color c=g.getBackground();Color r=colors.get(c);if(r==null)g.clearRect(x,y,w,h);else{Paint p=g.getPaint();try{g.setColor(r);g.fillRect(x,y,w,h);}finally{g.setPaint(p);}}}
    @Override public void fillRoundRect(int x,int y,int w,int h,int a,int b){fill(()->g.fillRoundRect(x,y,w,h,a,b));}
    @Override public void fillOval(int x,int y,int w,int h){fill(()->g.fillOval(x,y,w,h));}
    @Override public void fillArc(int x,int y,int w,int h,int a,int b){fill(()->g.fillArc(x,y,w,h,a,b));}
    @Override public void fillPolygon(int[] x,int[] y,int n){fill(()->g.fillPolygon(x,y,n));}
    @Override public Color getColor(){return g.getColor();}
    @Override public void setColor(Color c){g.setColor(c);}
    @Override public void setPaintMode(){g.setPaintMode();}
    @Override public void setXORMode(Color c){g.setXORMode(c);}
    @Override public Font getFont(){return g.getFont();}
    @Override public void setFont(Font f){g.setFont(f);}
    @Override public FontMetrics getFontMetrics(Font f){return g.getFontMetrics(f);}
    @Override public Rectangle getClipBounds(){return g.getClipBounds();}
    @Override public void clipRect(int x,int y,int w,int h){g.clipRect(x,y,w,h);}
    @Override public void setClip(int x,int y,int w,int h){g.setClip(x,y,w,h);}
    @Override public Shape getClip(){return g.getClip();}
    @Override public void setClip(Shape s){g.setClip(s);}
    @Override public void copyArea(int x,int y,int w,int h,int dx,int dy){g.copyArea(x,y,w,h,dx,dy);}
    @Override public void drawLine(int x,int y,int a,int b){g.drawLine(x,y,a,b);}
    @Override public void drawRoundRect(int x,int y,int w,int h,int a,int b){g.drawRoundRect(x,y,w,h,a,b);}
    @Override public void drawOval(int x,int y,int w,int h){g.drawOval(x,y,w,h);}
    @Override public void drawArc(int x,int y,int w,int h,int a,int b){g.drawArc(x,y,w,h,a,b);}
    @Override public void drawPolyline(int[] x,int[] y,int n){g.drawPolyline(x,y,n);}
    @Override public void drawPolygon(int[] x,int[] y,int n){g.drawPolygon(x,y,n);}
    @Override public void drawString(String s,int x,int y){g.drawString(s,x,y);}
    @Override public void drawString(String s,float x,float y){g.drawString(s,x,y);}
    @Override public void drawString(AttributedCharacterIterator s,int x,int y){g.drawString(s,x,y);}
    @Override public void drawString(AttributedCharacterIterator s,float x,float y){g.drawString(s,x,y);}
    @Override public boolean drawImage(Image i,int x,int y,ImageObserver o){return g.drawImage(i,x,y,o);}
    @Override public boolean drawImage(Image i,int x,int y,int w,int h,ImageObserver o){return g.drawImage(i,x,y,w,h,o);}
    @Override public boolean drawImage(Image i,int x,int y,Color c,ImageObserver o){return g.drawImage(i,x,y,c,o);}
    @Override public boolean drawImage(Image i,int x,int y,int w,int h,Color c,ImageObserver o){return g.drawImage(i,x,y,w,h,c,o);}
    @Override public boolean drawImage(Image i,int x,int y,int a,int b,int c,int d,int e,int f,ImageObserver o){return g.drawImage(i,x,y,a,b,c,d,e,f,o);}
    @Override public boolean drawImage(Image i,int x,int y,int a,int b,int c,int d,int e,int f,Color color,ImageObserver o){return g.drawImage(i,x,y,a,b,c,d,e,f,color,o);}
    @Override public void draw(Shape s){g.draw(s);}
    @Override public boolean drawImage(Image i,java.awt.geom.AffineTransform t,ImageObserver o){return g.drawImage(i,t,o);}
    @Override public void drawImage(java.awt.image.BufferedImage i,java.awt.image.BufferedImageOp op,int x,int y){g.drawImage(i,op,x,y);}
    @Override public void drawRenderedImage(java.awt.image.RenderedImage i,java.awt.geom.AffineTransform t){g.drawRenderedImage(i,t);}
    @Override public void drawRenderableImage(java.awt.image.renderable.RenderableImage i,java.awt.geom.AffineTransform t){g.drawRenderableImage(i,t);}
    @Override public void drawGlyphVector(java.awt.font.GlyphVector v,float x,float y){g.drawGlyphVector(v,x,y);}
    @Override public boolean hit(Rectangle r,Shape s,boolean onStroke){return g.hit(r,s,onStroke);}
    @Override public GraphicsConfiguration getDeviceConfiguration(){return g.getDeviceConfiguration();}
    @Override public void setComposite(Composite c){g.setComposite(c);}
    @Override public void setPaint(Paint p){g.setPaint(p);}
    @Override public void setStroke(Stroke s){g.setStroke(s);}
    @Override public void setRenderingHint(RenderingHints.Key k,Object v){g.setRenderingHint(k,v);}
    @Override public Object getRenderingHint(RenderingHints.Key k){return g.getRenderingHint(k);}
    @Override public void setRenderingHints(Map<?,?> h){g.setRenderingHints(h);}
    @Override public void addRenderingHints(Map<?,?> h){g.addRenderingHints(h);}
    @Override public RenderingHints getRenderingHints(){return g.getRenderingHints();}
    @Override public void translate(int x,int y){g.translate(x,y);}
    @Override public void translate(double x,double y){g.translate(x,y);}
    @Override public void rotate(double a){g.rotate(a);}
    @Override public void rotate(double a,double x,double y){g.rotate(a,x,y);}
    @Override public void scale(double x,double y){g.scale(x,y);}
    @Override public void shear(double x,double y){g.shear(x,y);}
    @Override public void transform(java.awt.geom.AffineTransform t){g.transform(t);}
    @Override public void setTransform(java.awt.geom.AffineTransform t){g.setTransform(t);}
    @Override public java.awt.geom.AffineTransform getTransform(){return g.getTransform();}
    @Override public Paint getPaint(){return g.getPaint();}
    @Override public Composite getComposite(){return g.getComposite();}
    @Override public void setBackground(Color c){g.setBackground(c);}
    @Override public Color getBackground(){return g.getBackground();}
    @Override public Stroke getStroke(){return g.getStroke();}
    @Override public void clip(Shape s){g.clip(s);}
    @Override public java.awt.font.FontRenderContext getFontRenderContext(){return g.getFontRenderContext();}
  }
}
