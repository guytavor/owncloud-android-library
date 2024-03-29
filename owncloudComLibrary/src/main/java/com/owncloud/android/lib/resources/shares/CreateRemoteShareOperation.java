/* ownCloud Android Library is available under MIT license
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2019 ownCloud GmbH
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.shares;

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import okhttp3.FormBody;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Creates a new share.  This allows sharing with a user or group or as a link.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */
public class CreateRemoteShareOperation extends RemoteOperation {

    private static final String TAG = CreateRemoteShareOperation.class.getSimpleName();

    private static final String PARAM_NAME = "name";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_EXPIRATION_DATE = "expireDate";
    private static final String PARAM_PUBLIC_UPLOAD = "publicUpload";
    private static final String PARAM_PATH = "path";
    private static final String PARAM_SHARE_TYPE = "shareType";
    private static final String PARAM_SHARE_WITH = "shareWith";
    private static final String PARAM_PERMISSIONS = "permissions";
    private static final String FORMAT_EXPIRATION_DATE = "yyyy-MM-dd";

    private String mRemoteFilePath;
    private ShareType mShareType;
    private String mShareWith;
    private boolean mGetShareDetails;

    /**
     * Name to set for the public link
     */
    private String mName = "";

    /**
     * Password to set for the public link
     */
    private String mPassword;

    /**
     * Expiration date to set for the public link
     */
    private long mExpirationDateInMillis = 0;

    /**
     * Access permissions for the file bound to the share
     */
    private int mPermissions;

    /**
     * Upload permissions for the public link (only folders)
     */
    private Boolean mPublicUpload;

    /**
     * Constructor
     *
     * @param remoteFilePath Full path of the file/folder being shared. Mandatory argument
     * @param shareType      0 = user, 1 = group, 3 = Public link. Mandatory argument
     * @param shareWith      User/group ID with who the file should be shared.  This is mandatory for shareType
     *                       of 0 or 1
     * @param publicUpload   If false (default) public cannot upload to a public shared folder.
     *                       If true public can upload to a shared folder. Only available for public link shares
     * @param password       Password to protect a public link share. Only available for public link shares
     * @param permissions    1 - Read only Default for public shares
     *                       2 - Update
     *                       4 - Create
     *                       8 - Delete
     *                       16- Re-share
     *                       31- All above Default for private shares
     *                       For user or group shares.
     *                       To obtain combinations, add the desired values together.
     *                       For instance, for Re-Share, delete, read, update, add 16+8+2+1 = 27.
     */
    public CreateRemoteShareOperation(
            String remoteFilePath,
            ShareType shareType,
            String shareWith,
            boolean publicUpload,
            String password,
            int permissions
    ) {

        mRemoteFilePath = remoteFilePath;
        mShareType = shareType;
        mShareWith = shareWith;
        mPublicUpload = publicUpload;
        mPassword = password;
        mPermissions = permissions;
        mGetShareDetails = false;        // defaults to false for backwards compatibility
    }

    /**
     * Set name to create in Share resource. Ignored by servers previous to version 10.0.0
     *
     * @param name Name to set to the target share.
     *             Null or empty string result in no value set for the name.
     */
    public void setName(String name) {
        this.mName = (name == null) ? "" : name;
    }

    /**
     * Set password to create in Share resource.
     *
     * @param password Password to set to the target share.
     *                 Null or empty string result in no value set for the password.
     */
    public void setPassword(String password) {
        mPassword = password;
    }

    /**
     * Set expiration date to create in Share resource.
     *
     * @param expirationDateInMillis Expiration date to set to the target share.
     *                               Zero or negative value results in no value sent for expiration date.
     */
    public void setExpirationDate(long expirationDateInMillis) {
        mExpirationDateInMillis = expirationDateInMillis;
    }

    /**
     * Set permissions to create in Share resource.
     *
     * @param permissions Permissions to set to the target share.
     *                    Values <= 0 result in value set  to the permissions.
     */
    public void setPermissions(int permissions) {
        mPermissions = permissions;
    }

    /**
     * * Enable upload permissions to create in Share resource.
     * *
     * * @param publicUpload  Upload permission to set to the target share.
     * *                      Null results in no update applied to the upload permission.
     */
    public void setPublicUpload(Boolean publicUpload) {
        mPublicUpload = publicUpload;
    }

    public void setGetShareDetails(boolean set) {
        mGetShareDetails = set;
    }

    @Override
    protected RemoteOperationResult<ShareParserResult> run(OwnCloudClient client) {
        RemoteOperationResult<ShareParserResult> result;

        try {
            FormBody.Builder formBodyBuilder = new FormBody.Builder()
                    .add(PARAM_PATH, mRemoteFilePath)
                    .add(PARAM_SHARE_TYPE, Integer.toString(mShareType.getValue()))
                    .add(PARAM_SHARE_WITH, mShareWith);

            if (mName.length() > 0) {
                formBodyBuilder.add(PARAM_NAME, mName);
            }

            if (mExpirationDateInMillis > 0) {
                DateFormat dateFormat = new SimpleDateFormat(FORMAT_EXPIRATION_DATE, Locale.getDefault());
                Calendar expirationDate = Calendar.getInstance();
                expirationDate.setTimeInMillis(mExpirationDateInMillis);
                String formattedExpirationDate = dateFormat.format(expirationDate.getTime());
                formBodyBuilder.add(PARAM_EXPIRATION_DATE, formattedExpirationDate);
            }

            if (mPublicUpload) {
                formBodyBuilder.add(PARAM_PUBLIC_UPLOAD, Boolean.toString(true));
            }
            if (mPassword != null && mPassword.length() > 0) {
                formBodyBuilder.add(PARAM_PASSWORD, mPassword);
            }
            if (OCShare.DEFAULT_PERMISSION != mPermissions) {
                formBodyBuilder.add(PARAM_PERMISSIONS, Integer.toString(mPermissions));
            }

            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH);

            PostMethod postMethod = new PostMethod(new URL(uriBuilder.build().toString()));

            postMethod.setRequestBody(formBodyBuilder.build());

            postMethod.setRequestHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.CONTENT_TYPE_URLENCODED_UTF8);
            postMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeHttpMethod(postMethod);

            ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                    new ShareXMLParser()
            );

            if (isSuccess(status)) {
                parser.setOneOrMoreSharesRequired(true);
                parser.setOwnCloudVersion(client.getOwnCloudVersion());
                parser.setServerBaseUri(client.getBaseUri());
                result = parser.parse(postMethod.getResponseBodyAsString());

                if (result.isSuccess() && mGetShareDetails) {

                    // TODO Use executeHttpMethod
                    // retrieve more info - POST only returns the index of the new share
                    OCShare emptyShare = result.getData().getShares().get(0);
                    GetRemoteShareOperation getInfo = new GetRemoteShareOperation(
                            emptyShare.getRemoteId()
                    );
                    result = getInfo.execute(client);
                }

            } else {
                result = parser.parse(postMethod.getResponseBodyAsString());
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while Creating New Share", e);
        }
        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpConstants.HTTP_OK);
    }
}