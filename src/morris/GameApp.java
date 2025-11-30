package morris;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import morris.controller.GameController;

public class GameApp extends Application {
    @Override
    public void start(Stage stage) {
        GameController controller = new GameController();
        Scene scene = new Scene(controller.getRoot(), 900, 700);
        stage.setTitle("Nine Men's Morris - Human vs CPU");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
