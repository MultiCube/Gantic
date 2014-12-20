/*
 * This file is part of Gantic, licensed under the MIT License (MIT).
 *
 * Copyright (c) Simon Schlueter
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
