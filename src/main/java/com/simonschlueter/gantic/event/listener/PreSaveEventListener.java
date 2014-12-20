/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic.event.listener;

import com.simonschlueter.gantic.event.PreSaveEvent;

/**
 *
 * @author simon
 */
public interface PreSaveEventListener {
    
    public void onPreSave(PreSaveEvent event);
}
