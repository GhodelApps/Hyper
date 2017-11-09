/*
 * Copyright 2016 Geetesh Kalakoti <kalakotig@gmail.com>
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

package io.geeteshk.hyper.helper;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * Web server class using NanoHTTPD
 */
public class HyperServer extends NanoHTTPD {

    /**
     * Log TAG
     */
    private static final String TAG = HyperServer.class.getSimpleName();

    /**
     * File types and respective mimes
     */
    private final String[] mTypes = {"css", "js", "ico", "png", "jpg", "jpe", "svg", "bm", "gif", "ttf", "otf", "woff", "woff2", "eot", "sfnt"};
    private final String[] mMimes = {"text/css", "text/js", "image/x-icon", "image/png", "image/jpg", "image/jpeg", "image/svg+xml", "image/bmp", "image/gif", "application/x-font-ttf", "application/x-font-opentype", "application/font-woff", "application/font-woff2", "application/vnd.ms-fontobject", "application/font-sfnt"};

    private List<String> logs = new ArrayList<>();

    /**
     * ProjectManager to host web server for
     */
    private String mProject;

    /**
     * public Constructor
     *
     * @param project to host server for
     */
    public HyperServer(String project, List<String> logs) {
        super(8080);
        mProject = project;
        this.logs = logs;
    }

    /**
     * Serving files on server
     *
     * @param session not sure what this is
     * @return response
     */
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String mimeType = getMimeType(uri);

        if (uri.equals("/")) {
            File indexFile = ProjectManager.getIndexFile(mProject);
            String indexStr = indexFile.getPath();
            indexStr.replace(new File(Constants.HYPER_ROOT + File.separator + mProject).getPath(), "");
            uri = File.separator + indexStr;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(Constants.HYPER_ROOT + File.separator + mProject + uri);
        } catch (IOException e) {
            logs.add(e.getMessage());
        }

        try {
            return newFixedLengthResponse(Response.Status.OK, mimeType, IOUtils.toString(inputStream, Charset.defaultCharset()));
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            return newFixedLengthResponse(e.toString());
        }
    }

    /**
     * Get mimetype from uri
     *
     * @param uri of file
     * @return file mimetype
     */
    private String getMimeType(String uri) {
        for (int i = 0; i < mTypes.length; i++) {
            if (uri.endsWith("." + mTypes[i])) {
                return mMimes[i];
            }
        }

        return NanoHTTPD.MIME_HTML;
    }
}