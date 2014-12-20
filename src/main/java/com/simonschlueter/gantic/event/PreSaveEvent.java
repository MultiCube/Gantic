/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic.event;

import com.mongodb.DBObject;

/**
 *
 * @author simon
 */
public class PreSaveEvent extends Event {
    
    private DBObject dbObject;
    private boolean insert;
    
    public PreSaveEvent(DBObject dbObject, boolean insert) {
        this.dbObject = dbObject;
        this.insert = insert;
    }
    
    public DBObject getDBObject() {
        return dbObject;
    }
    
    public boolean isInsertOperation() {
        return insert;
    }
}
