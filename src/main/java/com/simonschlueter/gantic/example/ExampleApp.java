/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic.example;

import com.mongodb.BasicDBObject;
import com.simonschlueter.gantic.Gantic;
import com.simonschlueter.gantic.GanticAsync;
import com.simonschlueter.gantic.Result;

/**
 *
 * @author simon
 */
public class ExampleApp {
    
    private static ExampleApp instance;
    private GanticAsync gantic;
    
    public ExampleApp() {
        instance = this;
        
        gantic = new GanticAsync("127.0.0.1", 27017, "gantic_example", 2);
    }
    
    public void createUser(String username) {
        User user = new User(username);
        
        gantic.insert(user);
    }
    
    public void updateUser(String username, int age) {
        User user = gantic.findOne(User.class, new BasicDBObject("username", username));
        if (user != null) {
            user.setAge(age);
            
            gantic.update(user);
            
            System.out.println("Updated: " + user.getUsername());
        } else {
            System.out.println("No user found!");
        }
    }
    
    public void findUserAsync(String username) {
        gantic.findOneAsync(User.class, new BasicDBObject("username", username), new Result<User>() {

            @Override
            public void onFinish(User user) {
                if (user != null) {
                    System.out.println("User found asynchronously: " + user.getUsername());
                } else {
                    System.out.println("User not found..");
                }
            }
        
        });
    }
    
    public Gantic getGantic() {
        return gantic;
    }
    
    public static ExampleApp getInstance() {
        return instance;
    }
    
    public static void main(String[] args) {
       ExampleApp app = new ExampleApp();
        
       switch (args[0].toLowerCase()) {
           case "create": {
               app.createUser(args[1]);
               break;
           }
           case "update": {
               app.updateUser(args[1], Integer.parseInt(args[2]));
               break;
           }
           case "find": {
               app.findUserAsync(args[1]);
               break;
           }
           default: {
               System.out.println("invalid action");
           }
       }
    }
}
