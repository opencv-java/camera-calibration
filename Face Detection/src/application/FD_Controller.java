package application;

import java.io.ByteArrayInputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FD_Controller {
	// FXML buttons
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;
	// checkbox for selecting the Haar Classifier
	@FXML
	private CheckBox haarClassifier;
	// checkbox for selecting the LBP Classifier
	@FXML
	private CheckBox lbpClassifier;
	
	// a timer for acquiring the video stream
	private Timer timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;
	// the face cascade classifier object
	private CascadeClassifier faceCascade;
	// minimum face size
	private int absoluteFaceSize;
	private Image CamStream;
	
	/**
	 * Init the controller variables
	 */
	protected void init()
	{
		this.capture = new VideoCapture();
		this.faceCascade = new CascadeClassifier();
		this.absoluteFaceSize = 0;
	}
	
	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	protected void startCamera()
	{
		if (!this.cameraActive)
		{
			// disable setting checkboxes
			this.haarClassifier.setDisable(true);
			this.lbpClassifier.setDisable(true);
			
			// start the video capture
			this.capture.open(0);
			
			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				TimerTask frameGrabber = new TimerTask() {
					@Override
					public void run()
					{
						CamStream = grabFrame();
						Platform.runLater(new Runnable() {
							@Override
				            public void run() {
								
								// show the original frames
								originalFrame.setImage(CamStream);
								// set fixed width
								originalFrame.setFitWidth(600);
								// preserve image ratio
								originalFrame.setPreserveRatio(true);
								
				            	}
							});
					}
				};
				this.timer = new Timer();
				this.timer.schedule(frameGrabber, 0, 33);
				
				// update the button content
				this.cameraButton.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");
			// enable setting checkboxes
			this.haarClassifier.setDisable(false);
			this.lbpClassifier.setDisable(false);
			
			// stop the timer
			if (this.timer != null)
			{
				this.timer.cancel();
				this.timer = null;
			}
			// release the camera
			this.capture.release();
			// clean the image area
			originalFrame.setImage(null);
		}
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Image grabFrame()
	{
		// init everything
		Image imageToShow = null;
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					// face detection
					this.detectAndDisplay(frame);
					
					// convert the Mat object (OpenCV) to Image (JavaFX)
					imageToShow = mat2Image(frame);
				}
				
			}
			catch (Exception e)
			{
				// log the (full) error
				System.err.print("ERROR");
				e.printStackTrace();
			}
		}
		
		return imageToShow;
	}
	
	/**
	 * Perform face detection and show a rectangle around the detected face.
	 * 
	 * @param frame
	 *            the current frame
	 */
	private void detectAndDisplay(Mat frame)
	{
		// init
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();
		
		// convert the frame in gray scale
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		Imgproc.equalizeHist(grayFrame, grayFrame);
		
		// compute minimum face size (20% of the frame height)
		if (this.absoluteFaceSize == 0)
		{
			int height = grayFrame.rows();
			if (Math.round(height * 0.2f) > 0)
			{
				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}
		
		// detect faces
		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE, new Size(
				this.absoluteFaceSize, this.absoluteFaceSize), new Size());
		
		// each rectangle in faces is a face
		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++)
			Core.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
		
	}
	
	/**
	 * When the Haar checkbox is selected, deselect the other one and load the
	 * proper XML classifier
	 * 
	 */
	@FXML
	protected void haarSelected()
	{
		// check whether the lpb checkbox is selected and deselect it
		if (this.lbpClassifier.isSelected())
			this.lbpClassifier.setSelected(false);
		
		this.checkboxSelection("resources/haarcascades/haarcascade_frontalface_alt.xml");
	}
	
	/**
	 * 
	 When the LBP checkbox is selected, deselect the other one and load the
	 * proper XML classifier
	 */
	@FXML
	protected void lbpSelected()
	{
		// check whether the haar checkbox is selected and deselect it
		if (this.haarClassifier.isSelected())
			this.haarClassifier.setSelected(false);
		
		this.checkboxSelection("resources/lbpcascades/lbpcascade_frontalface.xml");
	}
	
	/**
	 * Common operation for both checkbox selections
	 * 
	 * @param classifierPath
	 *            the absolute path where the XML file representing a training
	 *            set for a classifier is present
	 */
	private void checkboxSelection(String... classifierPath)
	{
		// load the classifier(s)
		for (String xmlClassifier : classifierPath)
		{
			this.faceCascade.load(xmlClassifier);
		}
		
		// now the capture can start
		this.cameraButton.setDisable(false);
	}
	
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame)
	{
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer, according to the PNG format
		Highgui.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}
}
