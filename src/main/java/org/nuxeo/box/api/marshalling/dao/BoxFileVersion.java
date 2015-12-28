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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Version of a file.
 */
public class BoxFileVersion extends BoxTypedObject {

    public static final String FIELD_MODIFIED_BY = "modified_by";

    public static final String FIELD_NAME = "name";

    /**
     * Constructor.
     */
    public BoxFileVersion() {
        setType(BoxResourceType.FILE_VERSION.toString());
    }

    /**
     * Copy constructor, this does deep copy for all the fields.
     *
     * @param obj
     */
    public BoxFileVersion(BoxFileVersion obj) {
        super(obj);
    }

    /**
     * Instantiate the object from a map. Each entry in the map reflects to a field.
     *
     * @param map
     */
    public BoxFileVersion(Map<String, Object> map) {
        super(map);
    }

    /**
     * Get the user last modified this version.
     *
     * @return the user last modified this version
     */
    @JsonProperty(FIELD_MODIFIED_BY)
    public BoxUser getModifiedBy() {
        return (BoxUser) getValue(FIELD_MODIFIED_BY);
    }

    /**
     * Setter. This is only used by {@see <a href="http://jackson.codehaus .org">Jackson JSON processer</a>}
     *
     * @param modifiedBy user last modified the version
     */
    @JsonProperty(FIELD_MODIFIED_BY)
    private void setModifiedBy(BoxUser modifiedBy) {
        put(FIELD_MODIFIED_BY, modifiedBy);
    }

    /**
     * Get name of this version of file.
     *
     * @return name of this version of file
     */
    @JsonProperty(FIELD_NAME)
    public String getName() {
        return (String) getValue(FIELD_NAME);
    }

    /**
     * Setter. This is only used by {@see <a href="http://jackson.codehaus .org">Jackson JSON processer</a>}
     *
     * @param name name
     */
    @JsonProperty(FIELD_NAME)
    private void setName(String name) {
        put(FIELD_NAME, name);
    }

}
