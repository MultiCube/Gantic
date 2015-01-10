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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.simonschlueter.gantic.event.PreSaveEvent;
import com.simonschlueter.gantic.event.PreSerializeEvent;
import com.simonschlueter.gantic.event.listener.PreSaveEventListener;
import com.simonschlueter.gantic.event.listener.PreSerializeEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author simon
 */
public class GanticAsync extends Gantic {

    private LinkedBlockingQueue<GanticQuery> queue = new LinkedBlockingQueue<>();
    private CallbackHandler callbackHandler = new UnsafeCallbackHandler();
    private ArrayList<Thread> slaves = new ArrayList<>();
    private volatile boolean alive = true;
    
    public GanticAsync(MongoClient client, String db, int poolSize) {
        super(client, db);
        
        createPool(poolSize);
    }
    
    public GanticAsync(String host, int port, String db, int poolSize) {
        super(host, port, db);
        
        createPool(poolSize);
    }
    
    private void createPool(int size) {
        client.getMongoOptions().connectionsPerHost = size;
        
        for (int i = 1; i <= size; i++) {
            Thread thread = new Thread(new GanticSlave(i));
            thread.start();
            
            slaves.add(thread);
        }
    }
    
    /**
     * With this method a custom callback handler can
     * be set.
     * 
     * @param callbackHandler the callback handler to use
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    /**
     * Finds all objects of a certain type asynchronously.
     * 
     * @param <T> the class of the object to find
     * @param type the class of the object to find
     * @param callback a callback that will be called when finished
     */
    public <T> void findAsync(Class<T> type, Result<List<T>> callback) {
        findAsync(type, new BasicDBObject(), callback);
    }
    
    /**
     * Finds all objects of a certain type and matching a query asynchronously.
     * 
     * @param <T> the class of the object to find
     * @param type the class of the object to find
     * @param query the query to find the objects
     * @param callback a callback that will be called when finished
     */
    public <T> void findAsync(final Class<T> type, final DBObject query, Result<List<T>> callback) {
        execute(new GanticQuery<List<T>>() {

            @Override
            public List<T> execute() {
                List<T> list = new ArrayList<>();
        
                try (DBCursor cursor = getCollection(type).find(query)) {
                    while (cursor.hasNext()) {
                        list.add(ObjectParser.parseDbObject(type, cursor.next()));
                    }
                }
                
                return list;
            }
        
        }, callback);
    }
    
    /**
     * Finds the first object matching a certain query asynchronously.
     * 
     * @param <T> the class of the object to find
     * @param type the class of the object to find
     * @param query the query to find the objects
     * @param callback a callback that will be called when finished
     */
    public <T> void findOneAsync(final Class<T> type, final DBObject query, Result<T> callback) {
        execute(new GanticQuery<T>() {

            @Override
            public T execute() {
                DBObject result = getCollection(type).findOne(query);
                
                return result == null ? null : ObjectParser.parseDbObject(type, result);
            }
        
        }, callback);
    }
    
    /**
     * Inserts a new object into the database asynchronously
     * 
     * @param object the object to insert
     * @param callback an callback to be called when finished
     */
    public void insertAsync(final Object object, Result<WriteResult> callback) {
        if (object instanceof PreSerializeEventListener) {
            ((PreSerializeEventListener) object).onPreSerialize(new PreSerializeEvent(true));
        }
        
        final DBObject dbObject = ObjectParser.parseObject(object, true);
        
        if (object instanceof PreSaveEventListener) {
            ((PreSaveEventListener) object).onPreSave(new PreSaveEvent(dbObject, true));
        }
        
        execute(new GanticQuery<WriteResult>() {

            @Override
            public WriteResult execute() {
                ObjectParser.setId(object);
        
                return getCollection(object).insert(dbObject);
            }
        
        }, callback);
    }
    
    /**
     * Updates an object in the database asynchronously.
     * 
     * @param object the object to update
     * @param callback an callback to be called when finished
     * @param fields certain fields to be only updated
     */
    public void updateAsync(final Object object, Result<WriteResult> callback, String... fields) {
        if (object instanceof PreSerializeEventListener) {
            ((PreSerializeEventListener) object).onPreSerialize(new PreSerializeEvent(false));
        }
        
        final DBObject dbObject = ObjectParser.parseObject(object, true, fields);
        
        if (object instanceof PreSaveEventListener) {
            ((PreSaveEventListener) object).onPreSave(new PreSaveEvent(dbObject, false));
        }
        
        execute(new GanticQuery<WriteResult>() {

            @Override
            public WriteResult execute() {
                ObjectParser.setId(object);
        
                return getCollection(object).update(new BasicDBObject("_id", dbObject.get("_id")), new BasicDBObject("$set", dbObject));
            }
        
        }, callback);
    }
    
    private <T> void execute(GanticQuery<T> query, Result<T> callback) {
        queue.add(query.setCallback(callback));
    }
    
    /**
     * Shuts down all slaves and the database connection. This
     * method does not execute all queries left in the queue.
     */
    @Override
    public void shutdown() {
        shutdown(false);
    }
    
    /**
     * Shuts down all slaves and the database connection.
     * 
     * @param safe wether to execute all queries left synchronously or not
     */
    public void shutdown(boolean safe) {
        alive = false;
        
        if (safe) {
            for (GanticQuery query : queue) {
                try {
                    handleQuery(query);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        super.shutdown();
    }
    
    /**
     * Returns wether this client is still alive or has
     * been shut down.
     * 
     * @return wether the client is alive or not
     */
    public boolean isAlive() {
        return alive;
    }
    
    private void handleQuery(GanticQuery query) {
        Object value = query.handle();
        
        if (query.getCallback() != null) {
            callbackHandler.handleCallback(query.getCallback(), value);
        }
    }
    
    private class GanticSlave implements Runnable {

        private int id;
        
        public GanticSlave(int id) {
            this.id = id;
        }
        
        @Override
        public void run() {
            while (alive) {
                GanticQuery query = null;
                try {
                    query = queue.take();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                if (query != null) {
                    try {
                        handleQuery(query);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        
        @Override
        public String toString(){
            return "Gantic Slave #" + id;
        }
    }
    
    private class UnsafeCallbackHandler implements CallbackHandler {

        @Override
        public void handleCallback(Result result, Object object) {
            result.onFinish(object);
        }
        
    }
}
