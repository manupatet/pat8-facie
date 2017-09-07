package com.pat8.opencv.app;

import javax.swing.UnsupportedLookAndFeelException;

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

public class App {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws InterruptedException {
        Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
        Thread t = new Thread(new DetectFaceDemo());
        t.start();
        System.out.println("Meanwhile, let me print out a matrix = " + mat.dump());
        t.join();
    }
}

class DetectFaceDemo implements Runnable {
    public void run() {
        System.out.println("\nRunning DetectFaceDemo");
        ImageViewer imageViewer = null;
        try {
            imageViewer = new ImageViewer();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Create a face detector from the cascade file in the resources
        // directory.
        CascadeClassifier faceDetector = new CascadeClassifier(getClass().getResource("/lbpcascade_frontalface.xml").getPath());
        VideoCapture vid = new VideoCapture(1);
        vid.release();
        vid.open(1);
        if(!vid.isOpened()){
            throw new RuntimeException("Could not open device");
        }
        while(imageViewer.playOn()){
            Mat image = null;
            vid.retrieve(image);
//            imageViewer.show(image);
//        Mat image = Imgcodecs.imread(getClass().getResource("/photo.jpg").getPath());
            
            // Detect faces in the image.
            // MatOfRect is a special container class for Rect.
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(image, faceDetections);
            System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
            
            // Draw a bounding box around each face.
            for (Rect rect : faceDetections.toArray()) {
                // Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                // new Scalar(0, 255, 255));
                Imgproc.ellipse(image, new Point(Double.valueOf(rect.x + rect.width / 2), Double.valueOf(rect.y
                                                                                                         + rect.height / 2)), new Size(rect.width
                                                                                                                                       * 0.75, rect.height * 0.75), 0, 0, 360, new Scalar(0, 255, 255), 4, 0, 0);
            }
        }
        vid.release();
    }
}
