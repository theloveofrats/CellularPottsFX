package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import potts.CellularPotts;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root  = FXMLLoader.load(getClass().getResource("CellularPottsFX.fxml"));
        primaryStage.setTitle("Cellular Potts Panel");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}
