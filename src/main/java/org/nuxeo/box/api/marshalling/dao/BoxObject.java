/*
 * Copyright 2013 Box, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.box.api.marshalling.dao;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.nuxeo.box.api.marshalling.jsonentities.DefaultJSONStringEntity;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BoxObject extends DefaultJSONStringEntity {

    private static final Log log = LogFactory.getLog(BoxObject.class);

    private final Map<String, Object> extraMap = new HashMap<String, Object>();

    private final Map<String, Object> map = new HashMap<String, Object>();

    public BoxObject() {
    }

    /**
     * Instantiate the object from a map. Each entry in the map reflects to a field.
     *
     * @param map
     */
    public BoxObject(Map<String, Object> map) {
        cloneMap(this.map, map);
    }

    /**
     * Copy constructor, this does deep copy for all the fields.
     *
     * @param obj
     */
    public BoxObject(BoxObject obj) {
        cloneMap(map, obj.map);
        cloneMap(extraMap, obj.extraMap);
    }

    /**
     * Clone a map to another
     *
     * @param destination
     * @param source
     */
    @SuppressWarnings("unchecked")
    private static void cloneMap(Map<String, Object> destination, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof BoxObject) {
                try {
                    destination.put(entry.getKey(),
                            value.getClass().getConstructor(value.getClass()).newInstance(value));
                } catch (ReflectiveOperationException e) {
                    log.error(e, e);
                }
            } else if (value instanceof ArrayList<?>) {
                ArrayList<Object> list = new ArrayList<Object>();
                cloneArrayList(list, (ArrayList<Object>) value);
                destination.put(entry.getKey(), list);
            } else {
                destination.put(entry.getKey(), value);
            }
        }
    }

    /**
     * Clone an arraylist.
     *
     * @param destination
     * @param source
     */
    private static void cloneArrayList(ArrayList<Object> destination, ArrayList<Object> source) {
        for (Object obj : source) {
            if (obj instanceof BoxObject) {
                try {
                    destination.add(obj.getClass().getConstructor(obj.getClass()).newInstance(obj));
                } catch (ReflectiveOperationException e) {
                    log.error(e, e);
                }
            } else {
                destination.add(obj);
            }
        }
    }

    /**
     * Whether the two objects are equal. This strictly compares all the fields in the two objects, if any fields are
     * different this returns false.
     *
     * @param obj
     * @return Whether the two objects are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BoxObject)) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        BoxObject bObj = (BoxObject) obj;
        return map.equals(bObj.map) && extraMap.equals(bObj.extraMap);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(map).append(extraMap).toHashCode();
    }

    public void put(String key, Object value) {
        map.put(key, value);
    }

    public void putAll(Map<String, Object> newMap) {
        map.putAll(newMap);
    }

    public Object getValue(String key) {
        return map.get(key);
    }

    /**
     * Get extra data. This could be extra unknown data passed back from api responses, the data is put in a Map.
     *
     * @param key
     * @return extra object
     */
    public Object getExtraData(String key) {
        return extraMap.get(key);
    }

    @JsonAnyGetter
    public Map<String, Object> properties() {
        return extraMap;
    }

    /**
     * Use this method to check whether the object contains certain field at all. This helps differentiate the case when
     * the field is not returned from server at all, or is returned from server but value is null. For the first case,
     * this method returns false, the later case returns true.
     *
     * @return whether the field exists
     */
    public boolean contains(String key) {
        return map.containsKey(key) || extraMap.containsKey(key);
    }

    @JsonAnySetter
    public void handleUnknown(String key, Object value) {
        if (value instanceof String) {
            extraMap.put(key, value);
        }
    }

    /**
     * Added to introspect the map to update document
     *
     * @since 5.9.2
     */
    @JsonIgnore
    public Set<String> getKeySet() {
        return map.keySet();
    }
}
