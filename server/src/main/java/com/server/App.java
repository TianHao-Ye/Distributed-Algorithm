package com.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("server.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);

        // pass username to server
        String auctionName = getParameters().getRaw().get(0);
        ServerController controller = fxmlLoader.getController();
        controller.runServer(auctionName);

        stage.setScene(scene);
        stage.setTitle("Server");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
