package com.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ClientController {

    @FXML private ListView<String> currentBid;
    @FXML private ListView<String> bidProcess;
    @FXML private ListView<String> bidddingResult;
    @FXML private ListView<String> users;
    @FXML private Label currentUser;
    @FXML private Label algorithm;

    private MultiClient client;

    private ObservableList<String> productList;
    private ObservableList<String> processList;
    private ObservableList<String> resultList;
    private ObservableList<String> userList;

    public void runClient(String username) {
        // bind list to listviews
        productList = FXCollections.observableArrayList();
        processList = FXCollections.observableArrayList();
        resultList = FXCollections.observableArrayList();
        userList = FXCollections.observableArrayList();
        currentBid.setItems(productList);
        bidProcess.setItems(processList);
        bidddingResult.setItems(resultList);
        users.setItems(userList);

        currentUser.setText(username);
        client = new MultiClient(productList, processList, resultList, userList, username, algorithm);
        new Thread(() -> {
            client.connectMulticast("228.5.6.7", 1235);
        }).start(); 
    }

    @FXML 
    private void bid() {
        if (productList.size() < 1) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Alert Dialog");
            alert.setHeaderText(null);
            alert.setContentText("No product to bid!");
            Button button = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            button.setText("OK");
            alert.showAndWait();
        } else {
            client.bid();
        }
    }

    @FXML
    private void quit() {
        client.quit();
    }


}
