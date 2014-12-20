/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic.example;

import com.simonschlueter.gantic.annotation.MongoObject;
import com.simonschlueter.gantic.annotation.MongoSync;

/**
 *
 * @author simon
 */
@MongoObject
public class Auth {
    
    @MongoSync
    private String username;
    @MongoSync
    private String password;
}
