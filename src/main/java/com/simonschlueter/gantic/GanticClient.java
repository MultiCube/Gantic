/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
