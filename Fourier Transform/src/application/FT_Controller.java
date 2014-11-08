package application;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

/**
 * The controller associated to the only view of our application. The
 * application logic is implemented here. It handles the button for opening an
 * image and perform all the operation related to the Fourier transformation and
 * antitransformation.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @since 2013-12-11
 * 
 */

public class FT_Controller {
	// images to show in the view
		@FXML
		private ImageView originalImage;
		@FXML
		private ImageView transformedImage;
		@FXML
		private ImageView antitransformedImage;
		// a FXML button for performing the transformation
		@FXML
		private Button transformButton;
		// a FXML button for performing the antitransformation
		@FXML
		private Button antitransformButton;
		
		// the main app
		private FourierTransform main;
		// the JavaFX file chooser
		private FileChooser fileChooser;
		// support variables
		private Mat image;
		private List<Mat> planes;
		// the final complex image
		private Mat complexImage;
		
		/**
		 * Init the needed variables
		 */
		protected void init()
		{
			this.fileChooser = new FileChooser();
			this.image = new Mat();
			this.planes = new ArrayList<>();
			this.complexImage = new Mat();
		}
		
		/**
		 * Load an image from disk
		 */
		@FXML
		protected void loadImage()
		{
			// show the open dialog window
			File file = new File("./resources/");
			this.fileChooser.setInitialDirectory(file);
			file = this.fileChooser.showOpenDialog(this.main.getStage());
			if (file != null)
			{
				// read the image in gray scale
				this.image = Highgui.imread(file.getAbsolutePath(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
				// show the image
				this.originalImage.setImage(this.mat2Image(this.image));
				// set a fixed width
				this.originalImage.setFitWidth(250);
				// preserve image ratio
				this.originalImage.setPreserveRatio(true);
				// update the UI
				this.transformButton.setDisable(false);
				// empty the image planes if it is not the first image to be loaded
				if (!this.planes.isEmpty())
					this.planes.clear();
			}
		}
		
		/**
		 * The action triggered by pushing the button for apply the dft to the
		 * loaded image
		 */
		@FXML
		protected void transformImage()
		{
			// optimize the dimension of the loaded image
			Mat padded = this.optimizeImageDim(this.image);
			padded.convertTo(padded, CvType.CV_32F);
			// prepare the image planes to obtain the complex image
			this.planes.add(padded);
			this.planes.add(Mat.zeros(padded.size(), CvType.CV_32F));
			// prepare a complex image for performing the dft
			Core.merge(this.planes, this.complexImage);
			
			// dft
			Core.dft(this.complexImage, this.complexImage);
			
			// optimize the image resulting from the dft operation
			Mat magnitude = this.createOptimizedMagnitude(this.complexImage);
			
			// show the result of the transformation as an image
			this.transformedImage.setImage(this.mat2Image(magnitude));
			// set a fixed width
			this.transformedImage.setFitWidth(250);
			// preserve image ratio
			this.transformedImage.setPreserveRatio(true);
			
			// enable the button for perform the antitransformation
			this.antitransformButton.setDisable(false);
		}
		
		/**
		 * Optimize the image dimensions
		 * 
		 * @param image
		 *            the {@link Mat} to optimize
		 * @return the image whose dimensions have been optimized
		 */
		private Mat optimizeImageDim(Mat image)
		{
			// init
			Mat padded = new Mat();
			// get the optimal rows size for dft
			int addPixelRows = Core.getOptimalDFTSize(image.rows());
			// get the optimal cols size for dft
			int addPixelCols = Core.getOptimalDFTSize(image.cols());
			// apply the optimal cols and rows size to the image
			Imgproc.copyMakeBorder(image, padded, 0, addPixelRows - image.rows(), 0, addPixelCols - image.cols(),
					Imgproc.BORDER_CONSTANT, Scalar.all(0));
			
			return padded;
		}
		
		/**
		 * Optimize the magnitude of the complex image obtained from the DFT, to
		 * improve its visualization
		 * 
		 * @param complexImage
		 *            the complex image obtained from the DFT
		 * @return the optimized image
		 */
		private Mat createOptimizedMagnitude(Mat complexImage)
		{
			// init
			List<Mat> newPlanes = new ArrayList<>();
			Mat mag = new Mat();
			// split the comples image in two planes
			Core.split(complexImage, newPlanes);
			// compute the magnitude
			Core.magnitude(newPlanes.get(0), newPlanes.get(1), mag);
			
			// move to a logarithmic scale
			Core.add(mag, Scalar.all(1), mag);
			Core.log(mag, mag);
			// optionally reorder the 4 quadrants of the magnitude image
			this.shiftDFT(mag);
			// normalize the magnitude image for the visualization since both JavaFX
			// and OpenCV need images with value between 0 and 255
			Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX);
			
			// you can also write on disk the resulting image...
			// Highgui.imwrite("../magnitude.png", mag);
			
			return mag;
		}
		
		/**
		 * Reorder the 4 quadrants of the image representing the magnitude, after
		 * the DFT
		 * 
		 * @param image
		 *            the {@link Mat} object whose quadrants are to reorder
		 */
		private void shiftDFT(Mat image)
		{
			image = image.submat(new Rect(0, 0, image.cols() & -2, image.rows() & -2));
			int cx = image.cols() / 2;
			int cy = image.rows() / 2;
			
			Mat q0 = new Mat(image, new Rect(0, 0, cx, cy));
			Mat q1 = new Mat(image, new Rect(cx, 0, cx, cy));
			Mat q2 = new Mat(image, new Rect(0, cy, cx, cy));
			Mat q3 = new Mat(image, new Rect(cx, cy, cx, cy));
			
			Mat tmp = new Mat();
			q0.copyTo(tmp);
			q3.copyTo(q0);
			tmp.copyTo(q3);
			
			q1.copyTo(tmp);
			q2.copyTo(q1);
			tmp.copyTo(q2);
		}
		
		/**
		 * The action triggered by pushing the button for apply the inverse dft to
		 * the loaded image
		 */
		@FXML
		protected void antitransformImage()
		{
			Core.idft(this.complexImage, this.complexImage);
			
			Mat restoredImage = new Mat();
			Core.split(this.complexImage, this.planes);
			Core.normalize(this.planes.get(0), restoredImage, 0, 255, Core.NORM_MINMAX);
			
			this.antitransformedImage.setImage(this.mat2Image(restoredImage));
			// set a fixed width
			this.antitransformedImage.setFitWidth(250);
			// preserve image ratio
			this.antitransformedImage.setPreserveRatio(true);
		}
		
		/**
		 * Set the main app (needed for the FileChooser modal window)
		 * 
		 * @param mainApp
		 *            the main app
		 */
		public void setMainApp(FourierTransform mainApp)
		{
			this.main = mainApp;
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
