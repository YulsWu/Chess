import javafx.application.Application;
import javafx.stage.Stage;

public class FXTestApp extends Application {
    Stage stage;
    
    @Override
    public void start(Stage stage){
        FXTest testObj = new FXTest();
        stage.setScene(testObj.browserTest());

        stage.show();
    }
}
