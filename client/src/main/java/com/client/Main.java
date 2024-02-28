package com.client;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a username to join us.");
            System.exit(0);
        }
        App.main(args);
    }

}
