package com.share;

import java.io.Serializable;

public class Message implements Serializable {
    private String command;
    private String content;
    private int sequenceNumber;
    private String ID;

    public Message(String command, String content){
        this.command = command;
        this.content = content;
    }

    public Message(String command, String content, String ID){
        this.command = command;
        this.content = content;
        this.ID = ID;
    }

    public void setCommand(String command){
        this.command = command;
    }

    public String getCommand(){
        return this.command;
    }

    public void setContent(String content){
        this.content = content;
    }

    public String getContent(){
        return this.content;
    }

    public void setID(String ID){
        this.ID = ID;
    }

    public String getID(){
        return this.ID;
    }

    public void setSequenceNumber(int sequenceNumber){
        this.sequenceNumber = sequenceNumber;
    }
    
    public int getSequenceNumber(){
        return this.sequenceNumber;
    }    
}
