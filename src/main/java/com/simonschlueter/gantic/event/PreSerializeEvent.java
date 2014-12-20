/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic.event;

/**
 *
 * @author simon
 */
public class PreSerializeEvent extends Event {
    
    private boolean insert;
    
    public PreSerializeEvent(boolean insert) {
        this.insert = insert;
    }
    
    public boolean isInsertOperation() {
        return insert;
    }
}
