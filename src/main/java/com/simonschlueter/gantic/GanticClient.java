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
package com.simonschlueter.gantic;

import com.simonschlueter.gantic.annotation.MongoCollection;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author simon
 */
public abstract class GanticClient {
    
    protected MongoClient client;
    protected String db;
    
    public GanticClient(MongoClient client, String db) {
        this.client = client;
        this.db = db;
    }
    
    public GanticClient(String host, int port, String db) {
        this(null, db);
        
        try {
            client = new MongoClient(host, port);
            client.getMongoOptions().autoConnectRetry = true;
            client.getMongoOptions().connectionsPerHost = 3;
            
        } catch (UnknownHostException | MongoException ex) {
            Logger.getLogger(Gantic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public DBCollection getCollection(Class<?> objectClass) {
        MongoCollection objectAnnotation = objectClass.getAnnotation(MongoCollection.class);
        
        String objectDb = objectAnnotation.db();
        if (objectDb.isEmpty()) {
            objectDb = db;
        }
        
        String collectionName = objectAnnotation.name();
        if (collectionName.isEmpty()) {
            collectionName = objectClass.getSimpleName().replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
        }
        
        return client.getDB(objectDb).getCollection(collectionName);
    }
    
    public DBCollection getCollection(Object object) {
        return getCollection(object.getClass());
    }
}
