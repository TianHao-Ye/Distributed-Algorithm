package com.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("client.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        
        // pass username to client
        String username = getParameters().getRaw().get(0);
        ClientController controller = fxmlLoader.getController();
        controller.runClient(username);

        stage.setScene(scene);
        stage.setTitle("Client");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
}
