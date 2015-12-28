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
package org.nuxeo.box.api.marshalling.jsonparsing;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import org.nuxeo.box.api.marshalling.exceptions.BoxJSONException;
import org.nuxeo.box.api.marshalling.interfaces.IBoxJSONParser;
import org.nuxeo.box.api.marshalling.interfaces.IBoxResourceHub;
import org.nuxeo.box.api.marshalling.interfaces.IBoxType;

import java.io.IOException;
import java.io.InputStream;

/**
 * The json parser class wrapping Jackson JSON parser. For now, if user wants to remove jackson dependency(jackson
 * jars), all the overriden methods and constructor of this class needs to be rewritten against the cusomized json
 * parser. An alternative approach (not taken yet) requires user to implement a new IBoxJSONParser, in the meantime make
 * all the jackson library related calls in this class reflection calls. However this is error prone if we need to
 * update jackson. Since jackson is still the recommended way. We are not doing the reflection way yet.
 */
public class BoxJSONParser implements IBoxJSONParser {

    private final ObjectMapper mObjectMapper;

    public BoxJSONParser(final IBoxResourceHub hub) {
        mObjectMapper = new ObjectMapper();
        mObjectMapper.setSerializationInclusion(Include.NON_NULL);
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (IBoxType type : hub.getAllTypes()) {
            mObjectMapper.registerSubtypes(new NamedType(hub.getClass(type), type.toString()));
        }
    }

    protected ObjectMapper getObjectMapper() {
        return mObjectMapper;
    }

    @Override
    public String convertBoxObjectToJSONStringQuietly(final Object object) {
        try {
            return convertBoxObjectToJSONString(object);
        } catch (BoxJSONException e) {
            return null;
        }
    }

    @Override
    public <T> T parseIntoBoxObjectQuietly(final InputStream inputStream, final Class<T> theClass) {
        try {
            return parseIntoBoxObject(inputStream, theClass);
        } catch (BoxJSONException e) {
            return null;
        }
    }

    @Override
    public <T> T parseIntoBoxObjectQuietly(final String jsonString, final Class<T> theClass) {
        try {
            return parseIntoBoxObject(jsonString, theClass);
        } catch (BoxJSONException e) {
            return null;
        }
    }

    @Override
    public String convertBoxObjectToJSONString(Object object) throws BoxJSONException {
        try {
            return getObjectMapper().writeValueAsString(object);
        } catch (IOException e) {
            throw new BoxJSONException(e);
        }
    }

    @Override
    public <T> T parseIntoBoxObject(InputStream inputStream, Class<T> theClass) throws BoxJSONException {
        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jp = jsonFactory.createJsonParser(inputStream);
            return getObjectMapper().readValue(jp, theClass);
        } catch (IOException e) {
            throw new BoxJSONException(e);
        }
    }

    @Override
    public <T> T parseIntoBoxObject(String jsonString, Class<T> theClass) throws BoxJSONException {
        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jp = jsonFactory.createJsonParser(jsonString);
            return getObjectMapper().readValue(jp, theClass);
        } catch (IOException e) {
            throw new BoxJSONException(e);
        }
    }
}
