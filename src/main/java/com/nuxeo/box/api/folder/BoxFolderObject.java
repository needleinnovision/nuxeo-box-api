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
 *     dmetzler <dmetzler@nuxeo.com>
 */
package com.nuxeo.box.api.folder;

import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.exceptions.BoxJSONException;
import com.box.boxjavalibv2.jsonparsing.BoxJSONParser;
import com.box.boxjavalibv2.jsonparsing.BoxResourceHub;
import com.nuxeo.box.api.folder.io.BoxFolderAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

/**
 * WebObject for a Box Folder
 *
 * @since 5.9.2
 */
@WebObject(type = "folder")
@Produces({ MediaType.APPLICATION_JSON })
public class BoxFolderObject extends AbstractResource<ResourceTypeImpl> {

    @GET
    public Object doGet() {
        return getView("index");
    }

    @GET
    @Path("{folderId}")
    public Object doGetFolder(@PathParam("folderId")
    final String folderId) throws NoSuchDocumentException, ClientException, BoxJSONException {
        final CoreSession session = ctx.getCoreSession();
        final DocumentModel folder = session.getDocument(new IdRef(folderId));
        // Adapt nx document to box folder adapter
        final BoxFolderAdapter folderAdapter = folder.getAdapter(BoxFolderAdapter.class);
        folderAdapter.newBoxInstance(session);
        return folderAdapter.getJSONBoxFolder();
    }

    @POST
    public Object doPostFolder(String jsonBoxFolder) throws ClientException, BoxJSONException {
        final CoreSession session = ctx.getCoreSession();
        // Create box folder from json payload
        BoxFolder boxFolder = new BoxJSONParser(new BoxResourceHub()).parseIntoBoxObject(jsonBoxFolder, BoxFolder.class);
        // Fetching its parent to get parent id
        String parentId = boxFolder.getParent().getId();
        DocumentModel documentParent;
        if ("0".equals(parentId)) {
            documentParent = session.getRootDocument();
        } else {
            documentParent = session.getDocument(new IdRef(boxFolder.getParent().getId()));
        }
        // Create the nx document from box folder information
        DocumentModel newFolder = session.createDocumentModel(documentParent.getPathAsString(), boxFolder.getId(), "Folder");
        newFolder = session.createDocument(newFolder);
        // Adapt nx document to box folder adapter
        final BoxFolderAdapter folderAdapter = newFolder.getAdapter(BoxFolderAdapter.class);
        folderAdapter.newBoxInstance(session);
        // Return the new box folder json
        return folderAdapter.getJSONBoxFolder();
    }

    @PUT
    @Path("{folderId}")
    public Object doPutFolder(@PathParam("folderId") String folderId, String jsonBoxFolder) throws ClientException, BoxJSONException, ParseException, IllegalAccessException, InvocationTargetException {
        final CoreSession session = ctx.getCoreSession();
        // Fetch the nx document with given id
        final DocumentModel nxDocument = session.getDocument(new IdRef(folderId));
        // Create box folder from json payload
        BoxFolder boxFolderUpdated = new BoxJSONParser(new BoxResourceHub()).parseIntoBoxObject(jsonBoxFolder, BoxFolder.class);
        // Adapt nx document to box folder adapter
        final BoxFolderAdapter nxDocumentAdapter = nxDocument.getAdapter(BoxFolderAdapter.class);
        nxDocumentAdapter.newBoxInstance(session);
        // Update both nx document and box folder adapter
        nxDocumentAdapter.setBoxFolder(boxFolderUpdated);
        nxDocumentAdapter.save(session);
        // Return the new box folder json
        return nxDocumentAdapter.getJSONBoxFolder();
    }
}
