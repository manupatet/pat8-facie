package com.pat8.opencv.app;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class FaceDetector extends Canvas implements Runnable, ComponentListener, ActionListener {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static final long serialVersionUID = 1L;
    private final WindowAdapter adaptor = new WindowAdapter() {
        @Override
        public void windowClosing(final WindowEvent e) {
            FaceDetector.this.destroy();
            System.exit(0);
        }
    };

    private final String caption1 = "Detect faces", caption2 = "undetect faces";
    private boolean go;
    private BufferStrategy bufferStrategy;
    private final BufferedImage drawing;
    private Thread gameThread;
    private final JFrame frame;
    private long oldTime;
    private long timeSinceLastFPSCalculation;
    private int frames;
    private int updates;
    private int fps;
    private int ups;
    private final Rectangle drawAreaBounds;
    private final JPanel panelOfButtons;
    private final VideoCapture webcam = new VideoCapture();
    private final JCheckBox detectFacesSwitch;
    private final int cvType;
    private final CascadeClassifier faceDetector = new CascadeClassifier(getClass().getResource("/lbpcascade_frontalface.xml").getPath());

    public FaceDetector() {
        final Toolkit kit = this.getToolkit();
        final Dimension wndsize = kit.getScreenSize();
        this.frame = new JFrame();
        this.frame.setSize(wndsize.width / 2, wndsize.height / 2);
        this.frame.setLocationRelativeTo(null);
        this.setSize(this.frame.getWidth(), this.frame.getHeight() - 10);
        this.panelOfButtons = new JPanel();
        this.panelOfButtons.setSize(this.frame.getWidth() - 4, 30);
        this.detectFacesSwitch = new JCheckBox(caption1);
        this.detectFacesSwitch.addActionListener(this);
        this.panelOfButtons.add(detectFacesSwitch);
        this.drawAreaBounds = new Rectangle(this.getBounds());

        this.frame.addWindowListener(this.adaptor);
        this.frame.setTitle("Jadoo wala yantr");
        this.frame.setLayout(new BorderLayout(2, 2));
        this.frame.add(this, BorderLayout.CENTER);
        this.frame.add(this.panelOfButtons, BorderLayout.SOUTH);
        this.frame.pack();

        this.createBufferStrategy(2);
        this.addComponentListener(this);
        this.drawing = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(this.drawAreaBounds.width, this.drawAreaBounds.height);

        // Type of image
        this.cvType = drawing.getType() == BufferedImage.TYPE_INT_RGB ? CvType.CV_8UC3 : CvType.CV_8UC1;
    }

    public void destroy() {
        stop();

        try {
            this.gameThread.join(1000);
            webcam.release();
        } catch (final InterruptedException x) {
            System.err.println("Thread interrrupted while stopping yantr: " + x.getMessage());
        }
        this.frame.dispose();
    }

    public void statGame() {
        this.frame.setIgnoreRepaint(true);
        this.frame.setEnabled(true);
        this.frame.setVisible(true);
        this.go = true;
        this.gameThread = new Thread(this);
        this.gameThread.start();
        webcam.open(1);
    }

    public void stop() {
        this.go = false;
    }

    public void run() {
        this.bufferStrategy = this.getBufferStrategy();
        this.oldTime = System.nanoTime();

        while (this.go) {
            final long nanoTimeAtStartOfUpdate = System.nanoTime();

            this.tick();
            try {
                final Graphics2D g = (Graphics2D) this.bufferStrategy.getDrawGraphics();
                render(g);
                g.dispose();
                if (!this.bufferStrategy.contentsLost()) {
                    this.bufferStrategy.show();
                }
            } catch (final IllegalStateException e) {
                e.printStackTrace();
            }

            waitUntilNextUpdate(nanoTimeAtStartOfUpdate);
        }
    }

    private void tick() {
        if (this.timeSinceLastFPSCalculation >= 1000000000) {
            this.fps = this.frames;
            this.ups = this.updates;
            this.timeSinceLastFPSCalculation = this.timeSinceLastFPSCalculation - 1000000000;
            this.frames = 0;
            this.updates = 0;
        }

        final long elapsedTime = System.nanoTime() - this.oldTime;
        this.oldTime = this.oldTime + elapsedTime;
        this.timeSinceLastFPSCalculation = this.timeSinceLastFPSCalculation + elapsedTime;
        this.updates++;
    }

    private void waitUntilNextUpdate(final long nanoTimeCurrentUpdateStartedOn) {
        final long currentUpdateSpeed = 10;
        if (currentUpdateSpeed > 0) {
            long timeToSleep = currentUpdateSpeed - (System.nanoTime() - nanoTimeCurrentUpdateStartedOn) / 10000000;
            timeToSleep = Math.max(timeToSleep, 0);
            if (timeToSleep > 0) {
                try {
                    Thread.sleep(timeToSleep);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void render(final Graphics g) {

        final Graphics gameGraphics = g.create();
        gameGraphics.setColor(Color.DARK_GRAY);
        gameGraphics.fillRect(0, 0, this.drawAreaBounds.width, this.drawAreaBounds.height);
        final int fontHeight = g.getFontMetrics(g.getFont()).getHeight();

        if (webcam.isOpened()) {
            Mat imageFrame = new Mat();
            webcam.read(imageFrame);//Reads a frame off the camera


            if(detectFacesSwitch.isSelected()){//Faces detect block
                MatOfRect faceDetections = new MatOfRect();
                try{
                    faceDetector.detectMultiScale(imageFrame, faceDetections);
                    for (Rect rect : faceDetections.toArray()) {
                        Point point = new Point(Double.valueOf(rect.x + rect.width / 2), Double.valueOf(rect.y + rect.height / 2));
                        Size size = new Size(rect.width* 0.50, rect.height * 0.75);
                        Imgproc.ellipse(imageFrame, point, size, 0, 0, 360, new Scalar(10, 210, 255), 3, 0, 0);
                    }
                }catch(Exception x){
                    System.out.println("Uknown error while detecting faces: "+x.getMessage());
                    detectFacesSwitch.setSelected(false);
                }
            }

            BufferedImage img = new BufferedImage(this.drawAreaBounds.width, this.drawAreaBounds.height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            imageFrame.get(0, 0, data);
            gameGraphics.drawImage(img, this.drawAreaBounds.x+10, this.drawAreaBounds.y+10, this.drawAreaBounds.width-20, this.drawAreaBounds.height-75, Color.DARK_GRAY, null);

            final Graphics swingAndOtherGuiGraphics = g.create();
            swingAndOtherGuiGraphics.translate(this.drawAreaBounds.x, this.drawAreaBounds.y);
            swingAndOtherGuiGraphics.setColor(Color.cyan);
            swingAndOtherGuiGraphics.drawString("FPS: " + this.fps, 0, fontHeight);
            swingAndOtherGuiGraphics.drawString("UPS: " + this.ups, 0, fontHeight * 2);
            swingAndOtherGuiGraphics.dispose();

        }else{
            gameGraphics.setColor(Color.LIGHT_GRAY);
            gameGraphics.drawString("Initializing ...", 0, fontHeight);
        }
        gameGraphics.dispose();

        this.frames++;
    }

    public void componentMoved(final ComponentEvent e) {
    }

    public void componentShown(final ComponentEvent e) {
    }

    public void componentHidden(final ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        this.drawAreaBounds.setSize(this.frame.getWidth(), this.frame.getHeight());
        this.webcam.set(Videoio.CAP_PROP_FRAME_HEIGHT, this.frame.getHeight());
        this.webcam.set(Videoio.CAP_PROP_FRAME_WIDTH, this.frame.getWidth());
    }

    public void actionPerformed(ActionEvent e) {
        if (caption1 == e.getActionCommand()) {
            System.out.println("detect pressed");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                FaceDetector xyz = new FaceDetector();
                xyz.statGame();
            }
        });
    }
}
