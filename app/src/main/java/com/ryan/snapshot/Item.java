package com.ryan.snapshot;

public class Item {
    public String Id;
    public String Text;

    public Item(String id, String text) {
        Id = id;
        Text = text;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getText() {
        return Text;
    }

    public void setText(String text) {
        Text = text;
    }
}