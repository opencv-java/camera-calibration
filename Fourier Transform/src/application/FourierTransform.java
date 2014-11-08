package application;
	
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;
import org.opencv.core.Core;

/**
 * The main class for a JavaFX application. It creates and handle the main
 * window with its resources (style, graphics, etc.).
 * 
 * This application opens an image stored on disk and perform the Fourier
 * transformation and antitranformation.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @since 2013-12-11
 * 
 */

public class FourierTransform extends Application {
	// the main stage
	private Stage primaryStage;
	
	@Override
	public void start(Stage primaryStage) {
		try
		{
			// load the FXML resource
			FXMLLoader loader = new FXMLLoader(getClass().getResource("FT_FX.fxml"));
			BorderPane root = (BorderPane) loader.load();
			// set a whitesmoke background
			root.setStyle("-fx-background-color: whitesmoke;");
			Scene scene = new Scene(root, 800, 600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			this.primaryStage = primaryStage;
			this.primaryStage.setTitle("Fourier Transform");
			this.primaryStage.setScene(scene);
			this.primaryStage.show();
			
			// init the controller
			FT_Controller controller = loader.getController();
			controller.setMainApp(this);
			controller.init();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the main stage
	 * 
	 * @return the stage
	 */
	protected Stage getStage()
	{
		return this.primaryStage;
	}
	
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		launch(args);
	}
}
