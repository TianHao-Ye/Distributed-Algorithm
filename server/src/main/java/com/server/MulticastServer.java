package com.server;

import com.share.Message;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.*;
import java.util.Comparator;
import java.util.PriorityQueue;

public class MulticastServer {
    //UI elements
    private final ObservableList<String> userList;
    private final ObservableList<String> processList;

    //multicast connection
    private InetSocketAddress address;
    private MulticastSocket multicastSocket;

    private boolean runningSequencer;

    //sequencer algorithm variables
    private int sequenceNumber;

    //ISIS algorithm variables
    private int largestAgreedSN;
    private int largestProposedSN;

    //to store received message for total ordering
    private PriorityQueue<Message> holdBackQueue;

    public MulticastServer(ObservableList<String> processList, ObservableList<String> userList) {
        this.userList = userList;
        this.processList = processList;
        this.runningSequencer = true;
        //Initialize Sequencer variables
        this.sequenceNumber = 0;
        //Initialize ISIS variables
        this.largestAgreedSN = 0;
        this.largestProposedSN = 0;
        holdBackQueue = new PriorityQueue<>(Comparator.comparing(Message::getSequenceNumber));
    }

    // launch multicast socket
    public void launchMulticast(String host, int port) {
        try {
            multicastSocket = new MulticastSocket(port);

            // join the multicast group
            InetAddress group = InetAddress.getByName(host);
            address = new InetSocketAddress(group, port);
            NetworkInterface networkInterface = NetworkInterface.getByName("bge0");
            multicastSocket.joinGroup(address, networkInterface);

            // receive message in sequencer-based algorithm by default;
            receive();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // server multicast messages to the group
    public void multicastMessage(Message message) {
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

    // server receive multicast message from the group, the receiving rule depends on the running algorithm
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

    // switch between Sequencer and ISIS, notify the group
    public void switchAlgorithm() {
        runningSequencer = !runningSequencer;
        multicastMessage(new Message("switch", ""));
    }

    // server receive multicast message from the group
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

    // the sequencer(server) assign a sequence number to "bid" message
    private void sequencerAlgorithm(Message message) {
        switch (message.getCommand()) {
            case "add":
                addUser(message.getContent());
                break;
            case "quit":
                removeUser(message.getContent());
                break;
            // receive a message from the group, assign a seeuqnce number and multicast to the group
            case "bid":
                totalOrderDeliver(message);
                orderMessage(message);
                break;
            // do nothing, sequence number assigned by myself, to-delivered already
            case "order":
                break;
        }
    }

    // the server only propose sequence number in ISIS
    private void ISISAlgorithm(Message message) {
        switch (message.getCommand()) {
            case "add":
                addUser(message.getContent());
                break;
            case "quit":
                removeUser(message.getContent());
                break;
            // propose sequence number to the message, keep it in hold-back queue and send it back to sender
            case "bid":
                int proposedSN = Math.max(largestAgreedSN, largestProposedSN) + 1;
                largestProposedSN = proposedSN;
                message.setSequenceNumber(proposedSN);
                message.setCommand("propose");
                holdBackQueue.add(message);
                multicastMessage(message);
                break;
            // server do not bid, do nothing on receiving "propose" message 
            case "propose":
                break;
            // receive the agreed sequence number 
            case "order":
                //update largest observed agreed SN
                largestAgreedSN = Math.max(largestAgreedSN, message.getSequenceNumber());
                // update the proposed sequence number with agreed one
                for (Message m : holdBackQueue) {
                    if (m.getID() == message.getID()) {
                        m.setSequenceNumber(message.getSequenceNumber());
                        break;
                    }
                }
                // holdBackQueue.add(message);
                //if the head of hold back queue (smallest sequence number) is the one just added in, put it into deliverQueue
                if (holdBackQueue.peek().getID().equals(message.getID())) {
                    Message messageDelivered = holdBackQueue.poll();
                    totalOrderDeliver(messageDelivered);
                }
                break;
        }
    }

    //reset variables of two algorithms once new product released
    public void reset(){
        sequenceNumber = 0;
        //Initialize ISIS variables
        largestAgreedSN = 0;
        largestProposedSN = 0;
        holdBackQueue = new PriorityQueue<>(Comparator.comparing(Message::getSequenceNumber));
    }  


    // multicast bid result to clients
    public void publishResult(String result) {
        multicastMessage(new Message("result", result));
    }

    // add username to user board
    private void addUser(String username) {
        Platform.runLater(() -> {
            userList.add(username);
        });
    }

    // remove username from user board
    private void removeUser(String username) {
        Platform.runLater(() -> {
            userList.remove(username);
        });
    }

    // deliver to application layer
    public void totalOrderDeliver(Message message) {
        Platform.runLater(() -> {
            processList.add(message.getContent());
        });
    }

    // send 'order' message to the group to TO-deliver
    public void orderMessage(Message message) {
        message.setCommand("order");
        message.setSequenceNumber(sequenceNumber++);
        multicastMessage(message);
    }
}
