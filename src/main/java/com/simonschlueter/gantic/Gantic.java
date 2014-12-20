/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    
    public void update(Object object) {
        if (object instanceof PreSerializeEventListener) {
            ((PreSerializeEventListener) object).onPreSerialize(new PreSerializeEvent(false));
        }
        
        DBObject dbObject = ObjectParser.parseObject(object, false);
        
        if (object instanceof PreSaveEventListener) {
            ((PreSaveEventListener) object).onPreSave(new PreSaveEvent(dbObject, false));
        }
        
        getCollection(object).update(new BasicDBObject("_id", dbObject.get("_id")), new BasicDBObject("$set", dbObject));
    }
    
    public <T> List<T> find(Class<T> type) {
        return find(type, new BasicDBObject());
    }
    
    public <T> List<T> find(Class<T> type, DBObject query) {
        List<T> list = new ArrayList<>();
        
        try (DBCursor cursor = getCollection(type).find(query)) {
            while (cursor.hasNext()) {
                list.add(ObjectParser.parseDbObject(type, cursor.next()));
            }
        }
        
        return list;
    }
    
    public <T> T findOne(Class<T> type, DBObject query) {
        List<T> list = find(type, query);
        
        return list.isEmpty() ? null : list.get(0);
    }
}
