package com.server;

import com.share.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

public class ServerController {
    @FXML private ListView<String> currentBid;
    @FXML private ListView<String> bidProcess;
    @FXML private ListView<String> bidddingResult;
    @FXML private ListView<String> users;
    @FXML private Label currentUser;
    @FXML private ChoiceBox<String> choice;
    @FXML private Label algorithm;

    private MulticastServer server;
    private ObservableList<String> userList;
    private ObservableList<String> processList;
    private ObservableList<String> resultList;

    public void runServer(String auctionName) {
        // bind lists to listviews
        userList = FXCollections.observableArrayList();
        processList = FXCollections.observableArrayList();
        resultList = FXCollections.observableArrayList();
        users.setItems(userList);
        bidProcess.setItems(processList);
        bidddingResult.setItems(resultList);

        // launch server
        currentUser.setText(auctionName);
        server = new MulticastServer(processList, userList);
        new Thread(() -> {
            server.launchMulticast("228.5.6.7", 1235);
        }).start();
    }

    // publish a new product to clients
    @FXML
    private void publishProduct() {
        String product = choice.getValue();
        if (product == null) {
            showDialog("Please select a product!");
        } else {
            // clear old product
            clearBoard();
            server.reset();
            // publish new product to clients
            server.multicastMessage(new Message("product", product));
            // display on bid board
            currentBid.getItems().add(product);
        }

    }

    // publish the bid result of previous product
    @FXML
    private void publishResult() {
        if (processList.size() < 1) {
            showDialog("No results to be published!");
        } else {
            String result = currentBid.getItems().get(0) + ": " + processList.get(0);
            resultList.add(result);
            server.publishResult(result);
        }
    }

    // switch between ISIS and Sequencer algorithm
    @FXML
    private void switchAlgorithm() {
        if (algorithm.getText().equals("Sequencer-based")) {
            algorithm.setText("ISIS-based");
        } else {
            algorithm.setText("Sequencer-based");
        }
        server.switchAlgorithm();
    }

    // show an alert dialog when something go wrong
    private void showDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Alert Dialog");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Button button = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        button.setText("OK");
        alert.showAndWait();
    }

    // clear bid board and process board
    private void clearBoard() {
        currentBid.getItems().clear();
        processList.clear();
        Message message = new Message("clear", "");
        server.multicastMessage(message);
    }

}
