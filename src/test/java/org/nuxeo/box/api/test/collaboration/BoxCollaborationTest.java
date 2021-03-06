/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.box.api.test.collaboration;

import org.nuxeo.box.api.test.BoxBaseTest;
import org.nuxeo.box.api.test.BoxServerFeature;
import org.nuxeo.box.api.test.BoxServerInit;
import com.sun.jersey.api.client.ClientResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ BoxServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = BoxServerInit.class)
public class BoxCollaborationTest extends BoxBaseTest {

    @Test
    public void collaborationSuite() throws JSONException, IOException {
        String collaborationId = itCanPostCollaboration();
        itCanFetchACollaboration(collaborationId);
        itCanFetchCollaborations();
        itCanUpdateCollaboration(collaborationId);
        itCanDeleteCollaboration(collaborationId);
        checkIfNoCollaboration(collaborationId);
    }

    protected String itCanPostCollaboration() throws IOException, JSONException {
        // Fetching the folder in Nuxeo way
        DocumentModel folder = BoxServerInit.getFolder(1, session);
        // Posting with few properties
        ClientResponse response = service.path("collaborations").post(
                ClientResponse.class,
                "{\"item\": { \"id\": \"" + folder.getId() + "\", "
                        + "\"type\": \"folder\"}, \"accessible_by\": { \"id\": "
                        + "\"members\", \"type\": \"user\" }, " + "\"role\": \"editor\"}");
        // Checking response consistency
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONObject finalResult = getJSONFromResponse(response);
        assertEquals("editor", finalResult.get("role"));
        return finalResult.getString("id");
    }

    protected void itCanFetchACollaboration(String collaborationId) throws IOException, JSONException {
        // Fetching the collaboration through NX Box API
        ClientResponse response = getResponse(BoxBaseTest.RequestType.GET, "collaborations/" + collaborationId);
        // Checking response consistency
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONObject finalResult = getJSONFromResponse(response);
        assertEquals("members", finalResult.getJSONObject("accessible_by").getString("login"));
    }

    protected void itCanFetchCollaborations() throws IOException, JSONException {
        // Fetching the folder in Nuxeo way
        DocumentModel folder = BoxServerInit.getFolder(1, session);
        // Fetching the collaboration through NX Box API
        ClientResponse response = getResponse(BoxBaseTest.RequestType.GET, "folders/" + folder.getId()
                + "/collaborations");
        // Checking response consistency
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONObject finalResult = getJSONFromResponse(response);
        assertEquals("editor", ((JSONObject) finalResult.getJSONArray("entries").get(0)).get("role"));
        assertEquals("1", finalResult.getString("total_count"));
    }

    protected void itCanUpdateCollaboration(String collaborationId) throws IOException, JSONException {
        // Posting with few properties
        ClientResponse response = service.path("collaborations/" + collaborationId).put(ClientResponse.class,
                "{\"role\": \"viewer\"}");
        // Checking response consistency
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONObject finalResult = getJSONFromResponse(response);
        assertEquals("viewer", finalResult.get("role"));
    }

    public void itCanDeleteCollaboration(String collaborationId) throws IOException, JSONException {
        ClientResponse response = service.path("collaborations/" + collaborationId).delete(ClientResponse.class);
        // Checking response consistency
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    protected void checkIfNoCollaboration(String collaborationId) throws IOException, JSONException {
        // Fetching the folder in Nuxeo way
        DocumentModel folder = BoxServerInit.getFolder(1, session);
        ClientResponse response = getResponse(RequestType.GET, "collaborations/" + collaborationId);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        response = getResponse(RequestType.GET, "folders/" + folder.getId() + "/collaborations");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JSONObject finalResult = getJSONFromResponse(response);
        assertEquals("0", finalResult.getString("total_count"));
    }
}
