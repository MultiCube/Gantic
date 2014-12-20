/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic.example;

import com.simonschlueter.gantic.annotation.MongoCollection;
import com.simonschlueter.gantic.annotation.MongoSync;
import com.simonschlueter.gantic.event.PreSaveEvent;
import com.simonschlueter.gantic.event.listener.PreSaveEventListener;
import java.util.HashMap;
import java.util.List;
import org.bson.types.ObjectId;

/**
 *
 * @author simon
 */
@MongoCollection
public class User implements PreSaveEventListener {
    
    @MongoSync(field = "_id")
    private ObjectId id;
    @MongoSync
    private String username;
    @MongoSync
    private Role role;
    @MongoSync
    private List<ObjectId> followers;
    @MongoSync
    private Auth auth;
    @MongoSync(field = "social.twitter")
    private String twitter;
    @MongoSync(field = "social.facebook")
    private String facebook;
    @MongoSync
    private Integer age;
    @MongoSync
    private HashMap<String, Integer> records = new HashMap<>();
    
    // Constructor for Gantic
    public User() {
        
    }
    
    public User(String username) {
        this.username = username;
        this.role = Role.USER;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public void onPreSave(PreSaveEvent event) {
        if (event.isInsertOperation() && (!event.getDBObject().containsField("age") || event.getDBObject().get("age") == null)) {
            event.getDBObject().put("age", 6);
        }
    }
}
