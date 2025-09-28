import com.YCorp.chessApp.client.javafx.control.BrowserSceneController;
import com.YCorp.chessApp.server.db.BrowserEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

public class FXTest {

    

    public Scene browserTest(){
        ObservableList<BrowserEntry> testData = FXCollections.observableArrayList(
            new BrowserEntry(null, "whitePlayer1", "blackPlayer1", "here", "life", "now", "1", "0-0"),
            new BrowserEntry(null, "whitePlayer2", "blackPlayer2", "there", "death", "inevitable", "0", "0-1")
        );


        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/browserScene.fxml"));
        Parent root = new Pane();

        try {
            root = loader.load();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        BrowserSceneController controller = loader.getController();
        controller.init();


        return new Scene(root);
    }
}
