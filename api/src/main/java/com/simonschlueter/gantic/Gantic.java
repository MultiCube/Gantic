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
import com.simonschlueter.gantic.event.PreSaveEvent;
import com.simonschlueter.gantic.event.PreSerializeEvent;
import com.simonschlueter.gantic.event.listener.PreSaveEventListener;
import com.simonschlueter.gantic.event.listener.PreSerializeEventListener;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author simon
 */
public class Gantic extends GanticClient {
    
    public Gantic(MongoClient client, String db) {
        super(client, db);
    }
    
    public Gantic(String host, int port, String db) {
        super(host, port, db);
    }
    
    /**
     * Inserts a new object into the database.
     * 
     * @param object The object to insert
     */
    public void insert(Object object) {
        ObjectParser.setId(object);
        
        if (object instanceof PreSerializeEventListener) {
            ((PreSerializeEventListener) object).onPreSerialize(new PreSerializeEvent(true));
        }
        
        DBObject dbObject = ObjectParser.parseObject(object, true);
        
        if (object instanceof PreSaveEventListener) {
            ((PreSaveEventListener) object).onPreSave(new PreSaveEvent(dbObject, true));
        }
        
        getCollection(object).insert(dbObject);
    }
    
    /**
     * Updates an object in the database.
     * 
     * @param object The object to update
     * @param fields Certain fields to be only updated
     */
    public void update(Object object, String... fields) {
        if (object instanceof PreSerializeEventListener) {
            ((PreSerializeEventListener) object).onPreSerialize(new PreSerializeEvent(false));
        }
        
        DBObject dbObject = ObjectParser.parseObject(object, false, fields);
        
        if (object instanceof PreSaveEventListener) {
            ((PreSaveEventListener) object).onPreSave(new PreSaveEvent(dbObject, false));
        }
        
        getCollection(object).update(new BasicDBObject("_id", dbObject.get("_id")), new BasicDBObject("$set", dbObject));
    }
    
    /**
     * Finds all objects of a certain type.
     * 
     * @param <T> the class of the object to find
     * @param type the class of the object to find
     * @return a list of the found objects
     */
    public <T> List<T> find(Class<T> type) {
        return find(type, new BasicDBObject());
    }
    
    /**
     * Finds all objects of a certain type and matching a query.
     * 
     * @param <T> the class of the object to find
     * @param type the class of the object to find
     * @param query the query to find the objects
     * @return a list of the found objects
     */
    public <T> List<T> find(Class<T> type, DBObject query) {
        List<T> list = new ArrayList<>();
        
        try (DBCursor cursor = getCollection(type).find(query)) {
            while (cursor.hasNext()) {
                list.add(ObjectParser.parseDbObject(type, cursor.next()));
            }
        }
        
        return list;
    }
    
    /**
     * Finds the first object matching a certain query.
     * 
     * @param <T> the class of the object to find
     * @param type the class of the object to find
     * @param query the query to find this object
     * @return the object
     */
    public <T> T findOne(Class<T> type, DBObject query) {
        List<T> list = find(type, query);
        
        return list.isEmpty() ? null : list.get(0);
    }
}
