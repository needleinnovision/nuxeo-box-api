/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.box.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.nuxeo.box.api.BoxConstants;
import org.nuxeo.box.api.adapter.BoxAdapter;
import org.nuxeo.box.api.folder.adapter.BoxFolderAdapter;
import org.nuxeo.box.api.marshalling.dao.BoxCollaboration;
import org.nuxeo.box.api.marshalling.dao.BoxCollaborationRole;
import org.nuxeo.box.api.marshalling.dao.BoxCollection;
import org.nuxeo.box.api.marshalling.dao.BoxComment;
import org.nuxeo.box.api.marshalling.dao.BoxFile;
import org.nuxeo.box.api.marshalling.dao.BoxFolder;
import org.nuxeo.box.api.marshalling.dao.BoxGroup;
import org.nuxeo.box.api.marshalling.dao.BoxItem;
import org.nuxeo.box.api.marshalling.dao.BoxObject;
import org.nuxeo.box.api.marshalling.dao.BoxTypedObject;
import org.nuxeo.box.api.marshalling.dao.BoxUser;
import org.nuxeo.box.api.marshalling.exceptions.BoxJSONException;
import org.nuxeo.box.api.marshalling.exceptions.BoxRestException;
import org.nuxeo.box.api.marshalling.exceptions.NXBoxJsonException;
import org.nuxeo.box.api.marshalling.jsonparsing.BoxJSONParser;
import org.nuxeo.box.api.marshalling.jsonparsing.BoxResourceHub;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Box Service Utils
 *
 * @since 5.9.3
 */
public class BoxServiceImpl implements BoxService {

    /**
     * The mapping between Nuxeo ACLs and Box Collaboration
     */
    protected final BiMap<String, String> nxBoxRole;

    @Override
    public BiMap<String, String> getNxBoxRole() {
        return nxBoxRole;
    }

    public BoxServiceImpl() {
        nxBoxRole = HashBiMap.create();
        nxBoxRole.put(SecurityConstants.EVERYTHING, BoxCollaborationRole.EDITOR);
        nxBoxRole.put(SecurityConstants.READ, BoxCollaborationRole.VIEWER);
        nxBoxRole.put(SecurityConstants.WRITE, BoxCollaborationRole.VIEWER_UPLOADER);
    }

    @Override
    public BoxCollection searchBox(String term, CoreSession session, String limit, String offset)
            {
        final Map<String, Object> collectionProperties = new HashMap<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM " + "Document where ecm:fulltext = '" + term + "'");
        DocumentModelList documentModels = session.query(query.toString(), null, Long.parseLong(limit),
                Long.parseLong(offset), false);
        // Adapt all documents to box document listing to get all properties
        List<BoxTypedObject> boxDocuments = new ArrayList<>();
        for (DocumentModel doc : documentModels) {
            BoxAdapter boxAdapter = doc.getAdapter(BoxAdapter.class);
            boxDocuments.add(boxAdapter.getBoxItem());
        }
        collectionProperties.put(BoxCollection.FIELD_ENTRIES, boxDocuments);
        collectionProperties.put(BoxCollection.FIELD_TOTAL_COUNT, documentModels.size());
        return new BoxCollection(Collections.unmodifiableMap(collectionProperties));
    }

    @Override
    public List<BoxTypedObject> getBoxDocumentCollection(DocumentModelList documentModels, String fields)
            {

        final List<BoxTypedObject> boxObject = new ArrayList<>();
        for (DocumentModel documentModel : documentModels) {
            final Map<String, Object> documentProperties = new HashMap<>();
            documentProperties.put(BoxTypedObject.FIELD_ID, getBoxId(documentModel));
            documentProperties.put(BoxItem.FIELD_SEQUENCE_ID, getBoxSequenceId(documentModel));
            documentProperties.put(BoxItem.FIELD_ETAG, getBoxEtag(documentModel));
            documentProperties.put(BoxItem.FIELD_NAME, getBoxName(documentModel));
            // NX MD5 -> Box SHA1
            if (documentModel.hasSchema("file")) {
                Blob blob = (Blob) documentModel.getPropertyValue("file:content");
                if (blob != null) {
                    documentProperties.put(BoxFile.FIELD_SHA1, blob.getDigest());
                }
            }
            // This different instantiation is related to the param type
            // which is automatically added in json payload by Box marshaller
            // following the box object type
            BoxTypedObject boxChild;
            boxChild = documentModel.isFolder() ? new BoxFolder() : new BoxFile();
            // Depending of fields filter provided in the REST call:
            // Properties setup (* -> all)
            if (!"*".equals(fields) && fields != null) {
                for (String field : fields.split(",")) {
                    boxChild.put(field, documentProperties.get(field));
                }
            } else {
                boxChild.putAll(documentProperties);
            }
            boxObject.add(boxChild);
        }
        return boxObject;
    }

    /**
     * @param boxFolderAdapter the related box folder
     * @param ace the specific ACE for this collaboration
     * @return a box collaboration
     */
    @Override
    public BoxCollaboration getBoxCollaboration(BoxFolderAdapter boxFolderAdapter, ACE ace, String collaborationId)
            {
        Map<String, Object> boxCollabProperties = new HashMap<>();
        // Nuxeo acl doesn't provide id yet
        boxCollabProperties.put(BoxCollaboration.FIELD_ID,
                computeCollaborationId(boxFolderAdapter.getBoxItem().getId(), collaborationId));
        // Nuxeo acl doesn't provide created date yet
        boxCollabProperties.put(BoxCollaboration.FIELD_CREATED_AT, null);
        // Nuxeo acl doesn't provide modified date yet
        boxCollabProperties.put(BoxCollaboration.FIELD_MODIFIED_AT, null);

        // Creator
        final UserManager userManager = Framework.getLocalService(UserManager.class);
        boxCollabProperties.put(BoxCollaboration.FIELD_CREATED_BY, boxFolderAdapter.getBoxItem().getCreatedBy());

        // Nuxeo doesn't provide expiration date yet
        boxCollabProperties.put(BoxCollaboration.FIELD_EXPIRES_AT, null);
        // Nuxeo doesn't provide status on ACL setup (accepted...)
        boxCollabProperties.put(BoxCollaboration.FIELD_STATUS, "active");
        // Nuxeo doesn't provide acknowledge date on status (see
        // just above)
        boxCollabProperties.put(BoxCollaboration.FIELD_ACKNOWLEGED_AT, null);

        // Document itself -> a mandatory folder
        boxCollabProperties.put(BoxCollaboration.FIELD_FOLDER, boxFolderAdapter.getMiniItem());

        // User or Group whom can access to the document
        NuxeoPrincipal user = userManager.getPrincipal(ace.getUsername());
        NuxeoGroup group = userManager.getGroup(ace.getUsername());
        boxCollabProperties.put(BoxCollaboration.FIELD_ACCESSIBLE_BY, user != null ? fillUser(user) : fillGroup(group));

        // Box Role
        boxCollabProperties.put(BoxCollaboration.FIELD_ROLE, nxBoxRole.get(ace.getPermission()));
        return new BoxCollaboration(boxCollabProperties);
    }

    /**
     * Marshalling the box object to JSON
     */
    @Override
    public String toJSONString(BoxObject boxObject) throws BoxJSONException {
        BoxJSONParser boxJSONParser = new BoxJSONParser(new BoxResourceHub());
        try {
            return boxObject.toJSONString(boxJSONParser);
        } catch (BoxJSONException e) {
            throw new BoxRestException("Box Parser Exception", e, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    /**
     * Helpers to get Ids for sequence, etag and id itself. In case of root, sequence and etag are null and id = 0
     * according to the box documentation.
     */

    @Override
    public String getBoxId(DocumentModel doc) {
        if (doc != null) {
            return doc.getName() != null ? doc.getId() : "0";
        }
        return null;
    }

    @Override
    public String getBoxSequenceId(DocumentModel doc) {
        if (doc != null) {
            return doc.getName() != null ? doc.getId() : null;
        }
        return null;
    }

    @Override
    public String getBoxEtag(DocumentModel doc) {
        if (doc != null) {
            return doc.getName() != null ? doc.getId() + "_" + doc.getVersionLabel() : null;
        }
        return null;
    }

    @Override
    public String getBoxName(DocumentModel doc) {
        if (doc != null) {
            return doc.getName() != null ? doc.getName() : "/";
        }
        return null;
    }

    /**
     * Return a box user from a Nuxeo user metamodel
     */
    @Override
    public BoxUser fillUser(NuxeoPrincipal creator) {
        final Map<String, Object> mapUser = new HashMap<>();
        mapUser.put(BoxItem.FIELD_ID, creator != null ? creator.getPrincipalId() : "system");
        mapUser.put(BoxItem.FIELD_NAME, creator != null ? creator.getFirstName() + " " + creator.getLastName()
                : "system");
        mapUser.put(BoxUser.FIELD_LOGIN, creator != null ? creator.getName() : "system");
        return new BoxUser(Collections.unmodifiableMap(mapUser));
    }

    /**
     * Return a box group from a Nuxeo user metamodel
     */
    @Override
    public BoxGroup fillGroup(NuxeoGroup group) {
        final Map<String, Object> mapGroup = new HashMap<>();
        mapGroup.put(BoxItem.FIELD_ID, group != null ? group.getName() : "system");
        mapGroup.put(BoxItem.FIELD_NAME, group != null ? group.getLabel() : "system");
        mapGroup.put(BoxUser.FIELD_LOGIN, group != null ? group.getName() : "system");
        return new BoxGroup(Collections.unmodifiableMap(mapGroup));
    }

    @Override
    public BoxFolder getBoxFolder(String jsonBoxFolder) throws BoxJSONException {
        return new BoxJSONParser(new BoxResourceHub()).parseIntoBoxObject(jsonBoxFolder, BoxFolder.class);
    }

    @Override
    public BoxFile getBoxFile(String jsonBoxFile) throws BoxJSONException {
        return new BoxJSONParser(new BoxResourceHub()).parseIntoBoxObject(jsonBoxFile, BoxFile.class);
    }

    @Override
    public BoxComment getBoxComment(String jsonBoxComment) throws BoxJSONException {
        return new BoxJSONParser(new BoxResourceHub()).parseIntoBoxObject(jsonBoxComment, BoxComment.class);
    }

    @Override
    public BoxCollaboration getBoxCollaboration(String jsonBoxCollaboration) throws BoxJSONException {
        return new BoxJSONParser(new BoxResourceHub()).parseIntoBoxObject(jsonBoxCollaboration, BoxCollaboration.class);
    }

    @Override
    public String getJSONFromBox(BoxTypedObject boxTypedObject) throws BoxJSONException {
        return boxTypedObject.toJSONString(new BoxJSONParser(new BoxResourceHub()));
    }

    /**
     * @return a Box Exception Response in JSON
     */
    @Override
    public String getJSONBoxException(Exception e, int status) {
        NXBoxJsonException boxException = new NXBoxJsonException();
        // Message
        boxException.setCode(e.getMessage());
        // Detailed Message
        boxException.setMessage(e.getCause() != null ? e.getCause().getMessage() : null);
        boxException.setStatus(status);
        ObjectMapper mapper = new ObjectMapper();
        String jsonExceptionResponse = StringUtils.EMPTY;
        try {
            jsonExceptionResponse = mapper.writeValueAsString(boxException);
        } catch (JsonProcessingException e1) {
            throw new BoxRestException("error when marshalling server " + "exception:", e1,
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
        return jsonExceptionResponse;
    }

    /**
     * @return the array containing Folder Id and Collab Id
     */
    @Override
    public String[] getCollaborationArrayIds(String collaborationId) {
        String[] collaborationIds = collaborationId.split(BoxConstants.BOX_COLLAB_DELIM);
        if (collaborationIds.length == 0) {
            return new String[2];
        }
        return collaborationIds;
    }

    public String computeCollaborationId(String folderId, String collaborationId) {
        return folderId.concat(BoxConstants.BOX_COLLAB_DELIM).concat(collaborationId);
    }
}
