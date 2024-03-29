/* ownCloud Android Library is available under MIT license
 *
 *   Copyright (C) 2019 ownCloud GmbH.
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
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.net.URL;

/**
 * Get the data from the server about ALL the known shares owned by the requester.
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */

public class GetRemoteSharesOperation extends RemoteOperation {

    private static final String TAG = GetRemoteSharesOperation.class.getSimpleName();

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;

        try {
            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon();
            uriBuilder.appendEncodedPath(ShareUtils.SHARING_API_PATH);

            GetMethod getMethod = new GetMethod(
                    new URL(client.getBaseUri() + ShareUtils.SHARING_API_PATH)
            );

            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeHttpMethod(getMethod);

            if (isSuccess(status)) {

                // Parse xml response and obtain the list of shares
                ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                        new ShareXMLParser()
                );
                parser.setOwnCloudVersion(client.getOwnCloudVersion());
                parser.setServerBaseUri(client.getBaseUri());
                result = parser.parse(getMethod.getResponseBodyAsString());
            } else {
                result = new RemoteOperationResult<>(getMethod);
            }
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while getting remote shares ", e);
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpConstants.HTTP_OK);
    }
}