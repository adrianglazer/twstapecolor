package twstapecolor;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Observable shade contrast and native graphics preservation. */
public final class SizeAndPaintTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static BufferedImage image(Component c){var im=new BufferedImage(160,24,BufferedImage.TYPE_INT_RGB);c.setSize(160,24);Graphics2D g=im.createGraphics();try{c.paint(g);}finally{g.dispose();}return im;}
 public static void main(String[] args)throws Exception{
  SwingUtilities.invokeAndWait(()->{
   double small=TapeViewV0318.intensity(100,10000),medium=TapeViewV0318.intensity(1000,10000),large=TapeViewV0318.intensity(10000,10000);
   check(small<.28&&medium>.43&&medium<.45&&large==1,"100/1000/10000-share shades have a wide range");
   check(large-small>.7,"large prints remain clearly brighter than small prints");
   double previous=0;for(int size:new int[]{1,10,100,1000,10000}){double a=TapeViewV0318.intensity(size,10000);check(a>previous&&a<=1,"shade increases monotonically");previous=a;}
   for(Color tint:new Color[]{new Color(22,131,74),new Color(184,59,75)}){
    JLabel text=new JLabel("Native white text");text.setForeground(Color.WHITE);text.setOpaque(true);text.setBackground(Color.BLACK);
    var nativeImage=image(text);var smallImage=image(new TintRendererV0318(text,tint,small));var largeImage=image(new TintRendererV0318(text,tint,large));
    check(largeImage.getRGB(159,1)==tint.getRGB(),"large trade uses configured RGB");
    Color low=new Color(smallImage.getRGB(159,1));check(Math.max(Math.abs(low.getGreen()-tint.getGreen()),Math.abs(low.getRed()-tint.getRed()))>90,"small and large backgrounds visibly differ");
    int glyphs=0;boolean preserved=true;for(int y=0;y<24;y++)for(int x=0;x<160;x++)if(nativeImage.getRGB(x,y)==Color.WHITE.getRGB()){glyphs++;if(largeImage.getRGB(x,y)!=Color.WHITE.getRGB())preserved=false;}
    check(glyphs>0&&preserved,"tint preserves native white glyph pixels");
   }
   JLabel shapes=new JLabel(){@Override protected void paintComponent(Graphics graphics){
    Graphics2D g=(Graphics2D)graphics.create();try{g.translate(2,2);g.setColor(Color.BLACK);g.fill(new java.awt.geom.Rectangle2D.Double(0,0,40,18));g.setColor(Color.BLUE);g.fillRect(42,0,10,10);}finally{g.dispose();}
   }};shapes.setBackground(Color.BLACK);
   var shaped=image(new TintRendererV0318(shapes,Color.GREEN,1));
   check(shaped.getRGB(5,5)==Color.GREEN.getRGB(),"Graphics2D create/transform/fill respects tint");
   check(shaped.getRGB(46,5)==Color.BLUE.getRGB(),"native icon-colored shapes remain unchanged");
  });
  System.out.println("Size and graphics tests passed ("+checks+" checks)");
 }
}
