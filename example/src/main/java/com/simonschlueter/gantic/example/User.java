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
