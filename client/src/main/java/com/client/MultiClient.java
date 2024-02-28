package com.client;

import com.share.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import java.sql.Timestamp;

public class MultiClient {

    private final String username;
    private final Label algorithm;
    private boolean runningSequencer;
    private final ObservableList<String> productList;
    private final ObservableList<String> processList;
    private final ObservableList<String> resultList;
    private final ObservableList<String> userList;

    //hold-back queue to store received messages waiting to be delivered
    private PriorityQueue<Message> holdBackQueue;
    //delivered queue to store total ordering messages
    private ArrayList<Message> deliveredQueue;
    //next sequence number to be delivered 
    private int localSequenceNumber;

    //ISIS approach variables
    private int largestAgreedSN;
    private int largestProposedSN;
    private PriorityQueue<Message> proposedList;

    private InetSocketAddress address;
    private MulticastSocket multicastSocket;

    public MultiClient(ObservableList<String> productList, ObservableList<String> processList, ObservableList<String> resultList,
                       ObservableList<String> userList, String username, Label algorithm) {
        this.productList = productList;
        this.processList = processList;
        this.resultList = resultList;
        this.userList = userList;
        this.username = username;
        this.algorithm = algorithm;
        this.runningSequencer = true;

        this.localSequenceNumber = 0;
        //initialize hold back queue, sorted acendingly based on sequence number
        holdBackQueue = new PriorityQueue<>(Comparator.comparing(Message::getSequenceNumber));
        deliveredQueue = new ArrayList<>();

        //initialize ISIS
        //initialize proposed sequence queue, sorted decendingly based on sequence number
        this.largestAgreedSN = 0;
        this.largestProposedSN = 0;
        proposedList = new PriorityQueue<>(Comparator.comparing(Message::getSequenceNumber, Comparator.reverseOrder()));

    }

    // connect to multicast socket 
    public void connectMulticast(String host, int port) {
        try {
            multicastSocket = new MulticastSocket(port);

            // join the multicast group
            InetAddress group = InetAddress.getByName(host);
            address = new InetSocketAddress(group, port);
            NetworkInterface networkInterface = NetworkInterface.getByName("bge0");
            multicastSocket.joinGroup(address, networkInterface);

            // notify other users that I join the group
            multicastMessage(new Message("add", username));

            receive();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // receive multicast message from the group, the receiving rule depends on the running algorithm
    private void receive() {
        byte[] buffer = new byte[1024];
        while (true) {
            Message message = recvMulticastMessage(buffer);
            if (runningSequencer) {
                sequencerAlgorithm(message);
            } else {
                ISISAlgorithm(message);
            }
        }
    }

    private Message recvMulticastMessage(byte[] buffer) {
        try {
            DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
            multicastSocket.receive(recvPacket);

            byte[] recvData = recvPacket.getData();
            ByteArrayInputStream byteStream = new ByteArrayInputStream(recvData);
            ObjectInputStream objectStream = new ObjectInputStream(byteStream);
            return (Message) objectStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sequencerAlgorithm(Message message) {
        switch (message.getCommand()) {
            case "add":
                addUser(message.getContent());
                // receive add message from another client, inform it of my name
                if (!message.getContent().equals(username)) {
                    multicastMessage(new Message("current", username));
                }
                break;
            case "quit":
                removeUser(message.getContent());
                break;
            case "current":
                addUser(message.getContent());
                break;
            // put in hold-back queue, waiting to be TO-delievered
            case "bid":
                // holdBackQueue.add(message);
                break;
            // receive order message from server 
            case "order":
                // add to hold-back queue
                holdBackQueue.add(message);
                // to-deliver the head message of hold-back queue if localSN = globalSN
                totalOrderDeliver();
                break;
            case "product":
                updateBidBoard(message);
                reset();
                break;
            case "result":
                updateResultBoard(message);
                break;
            case "clear":
                clearBoard();
                break;
            case "switch":
                runningSequencer = !runningSequencer;
                updateAlgorithm();
                break;
        }
    }

    private void ISISAlgorithm(Message message) {
        switch (message.getCommand()) {
            case "add":
                addUser(message.getContent());
                // receive add message from another client, inform it of my name
                if (!message.getContent().equals(username)) {
                    multicastMessage(new Message("current", username));
                }
                break;
            case "quit":
                removeUser(message.getContent());
                break;
            case "current":
                addUser(message.getContent());
                break;
            //receivers propose sequence number and send it back, also put it into its hold back queue
            case "bid":
                proposeSequenceNumber(message);
                break;
            //sender receive propose message from group, elect the agreed SN and multicast it to the group
            case "propose":
                orderSequenceNumber(message);
                break;
            //update largest observed agreed SN and to-deliver
            case "order":
                updateSequenceNumber(message);
                break;
            case "product":
                updateBidBoard(message);
                reset();
                break;
            case "result":
                updateResultBoard(message);
                break;
            case "clear":
                clearBoard();
                break;
            case "switch":
                runningSequencer = !runningSequencer;
                updateAlgorithm();
                break;
        }
    }

    //reset variables of two algorithms once new product released
    public void reset(){
        localSequenceNumber = 0;
        holdBackQueue = new PriorityQueue<>(Comparator.comparing(Message::getSequenceNumber));
        deliveredQueue = new ArrayList<>();
        largestAgreedSN = 0;
        largestProposedSN = 0;
        proposedList = new PriorityQueue<>(Comparator.comparing(Message::getSequenceNumber, Comparator.reverseOrder()));
    }    

    // the group propose a sequence number and send the message back to sender
    public void proposeSequenceNumber(Message message) {
        int proposedSN = Math.max(largestAgreedSN, largestProposedSN) + 1;
        largestProposedSN = proposedSN;
        message.setSequenceNumber(proposedSN);
        message.setCommand("propose");
        holdBackQueue.add(message);
        multicastMessage(message);
    }

    // sender select the biggest proposed sequence number and order this agreed sequence number to group
    public void orderSequenceNumber(Message message) {
        if (message.getContent().equals(username)) {
            proposedList.add(message);
            // if receive all proposals, get the largest sequence number and send it to group
            if (proposedList.size() == (userList.size()+1)) {
                Message orderedMessage = proposedList.poll();
                orderedMessage.setCommand("order");
                multicastMessage(orderedMessage);
                proposedList.clear();
            }
        }
    }

    // update LASN and deliver the msg if it is the head of priority queue
    public void updateSequenceNumber(Message message) {
        largestAgreedSN = Math.max(largestAgreedSN, message.getSequenceNumber());
        // update the proposed sequence number with agreed one
        for (Message m : holdBackQueue) {
            if (m.getID() == message.getID()) {
                m.setSequenceNumber(message.getSequenceNumber());
                break;
            }
        }
        // holdBackQueue.add(message);
        //if the head of hold back queue (smallest sequence number) is the one just added in, put it into deliverList
        if (holdBackQueue.peek().getID().equals(message.getID())) {
            Message messageDelivered = holdBackQueue.poll();
            updateProcessBoard(messageDelivered);
            deliveredQueue.add(messageDelivered);
        }
    }

    public void totalOrderDeliver() {
        if (holdBackQueue.peek().getSequenceNumber() == localSequenceNumber) {
            Message messageDelivered = holdBackQueue.poll();
            updateProcessBoard(messageDelivered);
            deliveredQueue.add(messageDelivered);
            localSequenceNumber = messageDelivered.getSequenceNumber() + 1;
        }
    }

    private void multicastMessage(Message message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            byte[] msgBytes = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, address);
            multicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateAlgorithm() {
        Platform.runLater(() -> {
            if (algorithm.getText().equals("Sequencer-based")) {
                algorithm.setText("ISIS-based");
            } else {
                algorithm.setText("Sequencer-based");
            }
        });
    }


    // add username to user board
    private void addUser(String username) {
        if (!userList.contains(username)) {
            Platform.runLater(() -> {
                userList.add(username);
            });
        }
    }

    // remove username from user board
    private void removeUser(String username) {
        Platform.runLater(() -> {
            userList.remove(username);
        });
    }

    // ID = UserName_timestamp
    public String createMessageID() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long mms = timestamp.getTime();
        return username + "_" + mms;
    }

    // bid the product, multicast the messages to group and the sequencer(server)
    public void bid() {
        multicastMessage(new Message("bid", username, createMessageID()));
    }

    public void quit() {
        multicastMessage(new Message("quit", username));
        System.exit(0);
    }


    private void updateBidBoard(Message message) {
        Platform.runLater(() -> {
            productList.add(message.getContent());
        });
    }

    private void updateProcessBoard(Message message) {
        Platform.runLater(() -> {
            processList.add(message.getContent());
        });
    }

    private void updateResultBoard(Message message) {
        Platform.runLater(() -> {
            resultList.add(message.getContent());
        });
    }

    private void clearBoard() {
        Platform.runLater(() -> {
            productList.clear();
            processList.clear();
        });
    }

}
