package com.pat8.opencv.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class ImageViewer extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    private final JPanel imageView;
    private final JButton startButton;
    private final VideoCapture vid = new VideoCapture(1);

    private CaptureTask task;

    public ImageViewer() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException {
        super("Face Detector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        // Components
        imageView = new JPanel();
        final JScrollPane imageScrollPane = new JScrollPane(this.imageView);
        imageScrollPane.setPreferredSize(new Dimension(800, 600));
        startButton = makeButton("Detect Faces");

        // Load Image
        // image = toBufferedImage(Imgcodecs.imread(getClass().getResource("/photo.jpg").getPath()));
        // Graphics2D g2d = (Graphics2D)imageView.getGraphics();
        // g2d.drawImage(image, 0, 0, null);

        (task = new CaptureTask()).execute();

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                task.cancel(true);
                task.clean();
            }
        });

        getContentPane().add(imageScrollPane, BorderLayout.CENTER);
        getContentPane().add(startButton, BorderLayout.SOUTH);

        // Display the window.
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

    }

    private JButton makeButton(String caption) {
        JButton b = new JButton(caption);
        b.setActionCommand(caption);
        b.addActionListener(this);
        b.setPreferredSize(new Dimension(10, 10));
        return b;
    }

    /*
     * public void show(final Mat image) { show(image, ""); }
     * 
     * public void show(final Mat image, final String windowName) { setSystemLookAndFeel(); final JFrame frame =
     * createJFrame(windowName); final Image loadedImage = toBufferedImage(image); this.imageView.setIcon(new
     * ImageIcon(loadedImage)); frame.pack(); frame.setVisible(true); }
     * 
     * private JFrame createJFrame(final String windowName) { final JFrame frame = new JFrame(windowName); final
     * JScrollPane imageScrollPane = new JScrollPane(this.imageView); this.imageView = imageScrollPane.getGraphics();
     * imageScrollPane.setPreferredSize(new Dimension(800, 600)); frame.add(imageScrollPane, BorderLayout.CENTER);
     * 
     * frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); return frame; }
     * 
     * private void setSystemLookAndFeel() { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
     * } catch (final ClassNotFoundException e) { e.printStackTrace(); } catch (final InstantiationException e) {
     * e.printStackTrace(); } catch (final IllegalAccessException e) { e.printStackTrace(); } catch (final
     * UnsupportedLookAndFeelException e) { e.printStackTrace(); } }
     */

    public Image toBufferedImage(final Mat matrix) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (matrix.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        final int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        final byte[] buffer = new byte[bufferSize];
        matrix.get(0, 0, buffer); // get all the pixels
        final BufferedImage image = new BufferedImage(matrix.cols(), matrix.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        return image;
    }

    public Image toBufferedImage2(final Mat matrix) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (matrix.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        // Create an empty image in matching format
        BufferedImage gray = new BufferedImage(matrix.width(), matrix.height(), type);

        // Get the BufferedImage's backing array and copy the pixels directly into it
        byte[] data = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();
        matrix.get(0, 0, data);
        return gray;
    }

    public boolean playOn() {
        return false;
    }

    private class CaptureTask extends SwingWorker<Void, Mat> {

        @Override
        protected Void doInBackground() {
            try {
                vid.open(1);
                if (!vid.isOpened()) {
                    throw new RuntimeException("Could not open device");
                }
                Mat frame = new Mat();
                while (!isCancelled()) {
                    // CascadeClassifier faceDetector = new
                    // CascadeClassifier(getClass().getResource("/lbpcascade_frontalface.xml").getPath());
                    if (frame.isContinuous()) {
                        vid.retrieve(frame);
                    }
                }
                publish(frame);
            } catch (Exception x) {
                x.printStackTrace();
            }
            return null;
        }

        public void clean() {
            vid.release();
        }

        @Override
        protected void process(List<Mat> listOfImages) {
            for (Mat img : listOfImages) {
                Image image = toBufferedImage2(img);
                Graphics2D g2d = (Graphics2D) imageView.getGraphics();
                g2d.drawImage(image, 0, 0, 800, 600, null);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if ("Detect Faces" == e.getActionCommand()) {
            startButton.setActionCommand("Un-detect Faces");
            (task = new CaptureTask()).execute();
        } else if ("Un-detect Faces" == e.getActionCommand()) {
            startButton.setActionCommand("Detect Faces");
            task.cancel(true);
            task = null;
        }
    }
}
