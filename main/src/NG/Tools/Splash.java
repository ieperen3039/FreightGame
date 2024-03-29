package NG.Tools;

import NG.AssetHandling.Resource;
import NG.Settings.Settings;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * a splash image that can be shown and disposed.
 * @author Geert van Ieperen created on 4-5-2019.
 */
public class Splash extends Frame implements Runnable {
    private static final Resource.Path path = Resource.Path.get("images", "SplashImage.png");

    public Splash() {
        setTitle("Loading " + Settings.GAME_NAME);

        try {
            BufferedImage splashImage = ImageIO.read(path.asStream());
            setImage(this, splashImage);

        } catch (Exception e) {
            Logger.ERROR.print("Could not load splash image " + path, e);
            setSize(new Dimension(500, 300));
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point centerPoint = ge.getCenterPoint();

        int dx = centerPoint.x - (getWidth() / 2);
        int dy = centerPoint.y - (getHeight() / 2);

        setLocation(dx, dy);
        setUndecorated(true);
        setBackground(Color.WHITE);
    }

    /**
     * makes a frame identical to the image, also adapts size
     * @param target some frame
     * @param image  some image
     */
    private void setImage(Frame target, final BufferedImage image) {
        target.add(new Component() {
            @Override
            public void paint(Graphics g) {
                g.drawImage(image, 0, 0, null);
            }
        });
        target.setSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    @Override
    public void run() {
        setVisible(true);
    }
}
