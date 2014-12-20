/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simonschlueter.gantic;

import com.simonschlueter.gantic.annotation.MongoSync;
import com.simonschlueter.gantic.annotation.MongoObject;
import com.simonschlueter.gantic.annotation.MongoCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.types.ObjectId;

/**
 *
 * @author simon
 */
public class ObjectParser {

    public static DBObject parseObject(Object object, boolean ignoreNull, String... fields) {
        BasicDBObject data = new BasicDBObject();

        for (Field field : object.getClass().getDeclaredFields()) {
            MongoSync sync = field.getAnnotation(MongoSync.class);
            if (sync != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                
                String f = field.getName();
                String parent = null;
                if (!sync.field().isEmpty()) {
                    f = sync.field();
                    
                    if (f.contains(".")) {
                        String[] path = f.split("\\.");
                        
                        f = path[1];
                        parent = path[0];
                    }
                }
                
                if (fields != null && fields.length > 0) {
                    if (!arrayContains(fields, f)) {
                        continue;
                    }
                } else if (!sync.save()) {
                    continue;
                }
                
                Object value = null;
                try {
                    value = field.get(object);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(ObjectParser.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                if (value != null && field.getType().getAnnotation(MongoObject.class) != null) {
                    value = parseObject(value, ignoreNull);
                }
                
                if (ignoreNull && !sync.insertNull()) {
                    if (value == null) {
                        continue;
                    }
                }
                
                if (value != null) {
                    if (value instanceof List) {
                        value = new ArrayList(((List) value));
                    } else if (value instanceof HashMap) {
                        BasicDBObject newValue = new BasicDBObject();
                        
                        HashMap<Object, Object> map = (HashMap) value;
                        for (Map.Entry<Object, Object> set : map.entrySet()) {
                            newValue.put(set.getKey().toString(), set.getValue());
                        }
                        
                        value = newValue;
                    } else if (value.getClass().isEnum()) {
                        value = value.toString();
                    }
                }
                
                if (parent != null) {
                    if (!data.containsField(parent)) {
                        data.put(parent, new BasicDBObject());
                    }
                    ((BasicDBObject) data.get(parent)).put(f, value);
                } else {
                    data.put(f, value);
                }
            }
        }

        return data;
    }

    public static void setId(Object object) {
        for (Field field : object.getClass().getDeclaredFields()) {
            MongoSync sync = field.getAnnotation(MongoSync.class);
            if (sync != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                String name = sync.field().isEmpty() ? field.getName() : sync.field();
                if (name.equals("_id") || name.equals("id")) {
                    if (field.getType().equals(ObjectId.class)) {
                        try {
                            if (field.get(object) != null) {
                                return;
                            }
                        } catch (IllegalArgumentException | IllegalAccessException ex) {
                            Logger.getLogger(ObjectParser.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        try {
                            field.set(object, new ObjectId());
                        } catch (IllegalArgumentException | IllegalAccessException ex) {
                            Logger.getLogger(ObjectParser.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
    }
    
    public static <T> T parseDbObject(Class<T> objectClass, DBObject data) {
        try {
            T instance = objectClass.newInstance();
            
            for (Field field : objectClass.getDeclaredFields()) {
                MongoSync sync = field.getAnnotation(MongoSync.class);
                if (sync != null) {
                    
                    String fieldName = field.getName();
                    String children = null;
                    Object value = null;
                    boolean hasData = false;
                    
                    if (!sync.field().isEmpty()) {
                        fieldName = sync.field();

                        if (fieldName.contains(".")) {
                            String[] path = fieldName.split("\\.");

                            fieldName = path[0];
                            children = path[1];
                        }
                    }
                    
                    if (data.containsField(fieldName)) {
                        hasData = true;
                        value = data.get(fieldName);
                    }
                    
                    if (children != null && hasData && value != null) {
                        BasicDBObject bdbo = (BasicDBObject) value;
                        if (bdbo.containsField(children)) {
                            value = bdbo.get(children);
                        } else {
                            hasData = false;
                            value = null;
                        }
                    }
                        
                    if (hasData) {
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        
                        if (value != null) {
                            if (field.getType().isAssignableFrom(int.class)) {
                                value = ((Number) value).intValue();
                            } else if (field.getType().isAssignableFrom(double.class)) {
                                value = ((Number) value).doubleValue();
                            } else if (field.getType().isAssignableFrom(float.class)) {
                                value = ((Number) value).floatValue();
                            } else if (field.getType().isAssignableFrom(long.class)) {
                                value = ((Number) value).longValue();
                            } else if (field.getType().isAssignableFrom(short.class)) {
                                value = ((Number) value).shortValue();
                            } else if (field.getType().isEnum()) {
                                value = field.getType().getMethod("valueOf", String.class).invoke(null, value);
                            } else if (List.class.isAssignableFrom(field.getType())) {
                                value = field.getType().getConstructor(List.class).newInstance(value);
                            } else if (field.getType().getAnnotation(MongoObject.class) != null) {
                                value = parseDbObject(field.getType(), (DBObject) value);
                            } else if (field.getType().equals(HashMap.class)) {
                                value = new HashMap<>((BasicDBObject) value);
                            }
                        }

                        field.set(instance, value);
                    }
                }
            }
            
            return instance;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ObjectParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

    public static MongoCollection getObjectAnnotation(Object object) {
        return object.getClass().getAnnotation(MongoCollection.class);
    }

    private static boolean arrayContains(Object[] obj, Object value) {
        for (Object o : obj) {
            if (o == value) {
                return true;
            }
        }
        return false;
    }
}
