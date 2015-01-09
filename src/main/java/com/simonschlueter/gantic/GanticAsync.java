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
    
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }
    
    public <T> void findAsync(Class<T> type, Result<List<T>> callback) {
        findAsync(type, new BasicDBObject(), callback);
    }
    
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
    
    public <T> void findOneAsync(final Class<T> type, final DBObject query, Result<T> callback) {
        execute(new GanticQuery<T>() {

            @Override
            public T execute() {
                DBObject result = getCollection(type).findOne(query);
                
                return result == null ? null : ObjectParser.parseDbObject(type, result);
            }
        
        }, callback);
    }
    
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
    
    public void updateAsync(final Object object, Result<WriteResult> callback) {
        if (object instanceof PreSerializeEventListener) {
            ((PreSerializeEventListener) object).onPreSerialize(new PreSerializeEvent(false));
        }
        
        final DBObject dbObject = ObjectParser.parseObject(object, true);
        
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
    
    public void shutdown() {
        alive = false;
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
                        Object value = query.handle();
                        if (query.getCallback() != null) {
                            callbackHandler.handleCallback(query.getCallback(), value);
                        }
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
