/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic;

/**
 *
 * @author simon
 * @param <T>
 */
public abstract class GanticQuery<T> {
    
    private Result<T> callback;
    
    public abstract T execute();
    
    public GanticQuery<T> setCallback(Result<T> callback) {
        this.callback = callback;
        
        return this;
    }
    
    public Result<T> getCallback() {
        return callback;
    }
    
    public T handle() {
        return execute();
    }
}
