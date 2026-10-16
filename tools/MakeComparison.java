import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Publication layout around unchanged actual tape screenshots; integer pixel scaling only. */
public final class MakeComparison {
  static final Color BG=new Color(11,16,23),TEXT=new Color(227,234,242),MUTED=new Color(166,183,203);
  static void text(Graphics2D g,String value,int x,int y,int size,boolean bold){g.setFont(new Font("SansSerif",bold?Font.BOLD:Font.PLAIN,size));g.setColor(TEXT);g.drawString(value,x,y);}
  public static void main(String[] args)throws Exception{
    if(args.length!=3)throw new IllegalArgumentException("before.png after.png output.png");
    var before=ImageIO.read(Path.of(args[0]).toFile());var after=ImageIO.read(Path.of(args[1]).toFile());
    if(before.getWidth()!=293||after.getWidth()!=293||before.getHeight()!=317||after.getHeight()!=317)throw new IllegalArgumentException("Expected the verified 293x317 tape-only crops");
    var image=new BufferedImage(1360,960,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
    try{
      g.setColor(BG);g.fillRect(0,0,1360,960);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      text(g,"twstapecolor",48,52,34,true);text(g,"Actual TCRT tape backgrounds  |  v0.3.19",49,84,20,false);
      for(int i=0;i<2;i++){
        int x=i==0?46:702;g.setColor(new Color(23,30,40));g.fillRoundRect(x,108,614,730,24,24);g.setColor(new Color(49,64,82));g.setStroke(new BasicStroke(2));g.drawRoundRect(x,108,614,730,24,24);
        text(g,i==0?"BEFORE  |  Native TWS (stopped)":"AFTER  |  twstapecolor",x+18,140,23,true);
        text(g,"Time",x+80,174,16,false);text(g,"Last",x+360,174,16,false);text(g,"Size",x+538,174,16,false);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(i==0?before:after,x+14,188,586,634,null);
      }
      g.setColor(new Color(22,131,74));g.fillRoundRect(50,867,23,23,6,6);text(g,"At / above ask",86,886,19,false);
      g.setColor(new Color(184,59,75));g.fillRoundRect(367,867,23,23,6,6);text(g,"At / below bid",403,886,19,false);
      g.setColor(new Color(49,64,82));g.fillRoundRect(694,867,23,23,6,6);text(g,"Inside spread / unknown: native background",730,886,19,false);
      text(g,"Time, Last and Size share the row tint; larger visible trades receive stronger shading.",49,921,18,false);
      text(g,"2026-10-16  |  Actual screen crops enlarged 2x; live trades differ between captures.",49,948,16,false);
    }finally{g.dispose();}
    ImageIO.write(image,"png",Path.of(args[2]).toFile());
  }
}
