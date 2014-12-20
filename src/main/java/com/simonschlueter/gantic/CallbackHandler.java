/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic;

/**
 *
 * @author simon
 */
public interface CallbackHandler {
    
    public void handleCallback(Result result, Object value);
}
