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
public interface Result<T> {
    
    public void onFinish(T result);
}
