package application;
	
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

/**
 * The main class for a JavaFX application. It creates and handle the main
 * window with its resources (style, graphics, etc.).
 * 
 * This application handles a video stream and perform all the needed steps for
 * calibrating a camera.
 * 
 */
public class CameraCalibration extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			// load the FXML resource
			FXMLLoader loader = new FXMLLoader(getClass().getResource("CC_FX.fxml"));
			// store the root element so that the controllers can use it
			BorderPane rootElement = (BorderPane) loader.load();
			// set a whitesmoke background
			rootElement.setStyle("-fx-background-color: whitesmoke;");
			// create and style a scene
			Scene scene = new Scene(rootElement, 800, 600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			primaryStage.setTitle("Camera Calibration");
			primaryStage.setScene(scene);
			// init the controller variables
			CC_Controller controller = loader.getController();
			controller.init();
			// show the GUI
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		launch(args);
	}
}
