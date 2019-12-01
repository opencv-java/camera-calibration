package application;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.videoio.VideoCapture;

/**
 * The controller associated to the only view of our application. The
 * application logic is implemented here. It handles the button for
 * starting/stopping the camera, the acquired video stream, the relative
 * controls and the overall calibration process.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @since 2013-11-20
 * 
 */

public class CC_Controller {
	// FXML buttons
		@FXML
		private Button cameraButton;
		@FXML
		private Button applyButton;
		@FXML
		private Button snapshotButton;
		// the FXML area for showing the current frame (before calibration)
		@FXML
		private ImageView originalFrame;
		// the FXML area for showing the current frame (after calibration)
		@FXML
		private ImageView calibratedFrame;
		// info related to the calibration process
		@FXML
		private TextField numBoards;
		@FXML
		private TextField numHorCorners;
		@FXML
		private TextField numVertCorners;
		
		// a timer for acquiring the video stream
		private Timer timer;
		// the OpenCV object that performs the video capture
		private VideoCapture capture;
		// a flag to change the button behavior
		private boolean cameraActive;
		// the saved chessboard image
		private Mat savedImage;
		// the calibrated camera frame
		private Image undistoredImage,CamStream;
		// various variables needed for the calibration
		private List<Mat> imagePoints;
		private List<Mat> objectPoints;
		private MatOfPoint3f obj;
		private MatOfPoint2f imageCorners;
		private int boardsNumber;
		private int numCornersHor;
		private int numCornersVer;
		private int successes;
		private Mat intrinsic;
		private Mat distCoeffs;
		private boolean isCalibrated;
		
		/**
		 * Init all the (global) variables needed in the controller
		 */
		protected void init()
		{
			this.capture = new VideoCapture();
			this.cameraActive = false;
			this.obj = new MatOfPoint3f();
			this.imageCorners = new MatOfPoint2f();
			this.savedImage = new Mat();
			this.undistoredImage = null;
			this.imagePoints = new ArrayList<>();
			this.objectPoints = new ArrayList<>();
			this.intrinsic = new Mat(3, 3, CvType.CV_32FC1);
			this.distCoeffs = new Mat();
			this.successes = 0;
			this.isCalibrated = false;
		}
		
		/**
		 * Store all the chessboard properties, update the UI and prepare other
		 * needed variables
		 */
		@FXML
		protected void updateSettings()
		{
			this.boardsNumber = Integer.parseInt(this.numBoards.getText());
			this.numCornersHor = Integer.parseInt(this.numHorCorners.getText());
			this.numCornersVer = Integer.parseInt(this.numVertCorners.getText());
			int numSquares = this.numCornersHor * this.numCornersVer;
			for (int j = 0; j < numSquares; j++)
				obj.push_back(new MatOfPoint3f(new Point3(j / this.numCornersHor, j % this.numCornersVer, 0.0f)));
			this.cameraButton.setDisable(false);
		}
		
		/**
		 * The action triggered by pushing the button on the GUI
		 */
		@FXML
		protected void startCamera()
		{
			if (!this.cameraActive)
			{
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
							CamStream=grabFrame();
							// show the original frames
							Platform.runLater(new Runnable() {
								@Override
					            public void run() {
									originalFrame.setImage(CamStream);
									// set fixed width
									originalFrame.setFitWidth(380);
									// preserve image ratio
									originalFrame.setPreserveRatio(true);
									// show the original frames
									calibratedFrame.setImage(undistoredImage);
									// set fixed width
									calibratedFrame.setFitWidth(380);
									// preserve image ratio
									calibratedFrame.setPreserveRatio(true);
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
					System.err.println("Impossible to open the camera connection...");
				}
			}
			else
			{
				// the camera is not active at this point
				this.cameraActive = false;
				// update again the button content
				this.cameraButton.setText("Start Camera");
				// stop the timer
				if (this.timer != null)
				{
					this.timer.cancel();
					this.timer = null;
				}
				// release the camera
				this.capture.release();
				// clean the image areas
				originalFrame.setImage(null);
				calibratedFrame.setImage(null);
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
						// show the chessboard pattern
						this.findAndDrawPoints(frame);
						
						if (this.isCalibrated)
						{
							// prepare the undistored image
							Mat undistored = new Mat();
							Calib3d.undistort(frame, undistored, intrinsic, distCoeffs);
							undistoredImage = mat2Image(undistored);
						}
						
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
		 * Take a snapshot to be used for the calibration process
		 */
		@FXML
		protected void takeSnapshot()
		{
			if (this.successes < this.boardsNumber)
			{
				// save all the needed values
				this.imagePoints.add(imageCorners);
				imageCorners = new MatOfPoint2f();
				this.objectPoints.add(obj);
				this.successes++;
			}
			
			// reach the correct number of images needed for the calibration
			if (this.successes == this.boardsNumber)
			{
				this.calibrateCamera();
			}
		}
		
		/**
		 * Find and draws the points needed for the calibration on the chessboard
		 * 
		 * @param frame
		 *            the current frame
		 * @return the current number of successfully identified chessboards as an
		 *         int
		 */
		private void findAndDrawPoints(Mat frame)
		{
			// init
			Mat grayImage = new Mat();
			
			// I would perform this operation only before starting the calibration
			// process
			if (this.successes < this.boardsNumber)
			{
				// convert the frame in gray scale
				Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
				// the size of the chessboard
				Size boardSize = new Size(this.numCornersVer, this.numCornersHor);
				// look for the inner chessboard corners
				boolean found = Calib3d.findChessboardCorners(grayImage, boardSize, imageCorners,
						Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK);
				// all the required corners have been found...
				if (found)
				{
					// optimization
					TermCriteria term = new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 30, 0.1);
					Imgproc.cornerSubPix(grayImage, imageCorners, new Size(11, 11), new Size(-1, -1), term);
					// save the current frame for further elaborations
					grayImage.copyTo(this.savedImage);
					// show the chessboard inner corners on screen
					Calib3d.drawChessboardCorners(frame, boardSize, imageCorners, found);
					
					// enable the option for taking a snapshot
					this.snapshotButton.setDisable(false);
				}
				else
				{
					this.snapshotButton.setDisable(true);
				}
			}
		}
		
		/**
		 * The effective camera calibration, to be performed once in the program
		 * execution
		 */
		private void calibrateCamera()
		{
			// init needed variables according to OpenCV docs
			List<Mat> rvecs = new ArrayList<>();
			List<Mat> tvecs = new ArrayList<>();
			intrinsic.put(0, 0, 1);
			intrinsic.put(1, 1, 1);
			// calibrate!
			Calib3d.calibrateCamera(objectPoints, imagePoints, savedImage.size(), intrinsic, distCoeffs, rvecs, tvecs);
			this.isCalibrated = true;
			
			// you cannot take other snapshot, at this point...
			this.snapshotButton.setDisable(true);
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
			Imgcodecs.imencode(".png", frame, buffer);
			// build and return an Image created from the image encoded in the
			// buffer
			return new Image(new ByteArrayInputStream(buffer.toArray()));
		}
}
