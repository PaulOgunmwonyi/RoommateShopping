package edu.uga.cs.roommateshopping.models;

public class Roommate {
    private String email;

    public Roommate() {
    }

    public Roommate(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
