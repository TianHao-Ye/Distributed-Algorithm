package com.server;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter the name of this auction.");
            System.exit(0);
        }
        App.main(args);
    }

    
}
