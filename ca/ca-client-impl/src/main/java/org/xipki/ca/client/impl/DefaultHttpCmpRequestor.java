/*
 * Copyright (c) 2014 Lijun Liao
 *
 * TO-BE-DEFINE
 *
 */

package org.xipki.ca.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;

import org.xipki.security.api.ConcurrentContentSigner;
import org.xipki.security.api.SecurityFactory;
import org.xipki.security.common.ParamChecker;

/**
 * @author Lijun Liao
 */

class DefaultHttpCmpRequestor extends X509CmpRequestor
{
    private static final String CMP_REQUEST_MIMETYPE = "application/pkixcmp";
    private static final String CMP_RESPONSE_MIMETYPE = "application/pkixcmp";

    private final URL serverUrl;

    DefaultHttpCmpRequestor(X509Certificate requestorCert,
            X509Certificate responderCert,
            X509Certificate caCert,
            String serverUrl,
            SecurityFactory securityFactory)
    {
        super(requestorCert, responderCert, caCert, securityFactory);
        ParamChecker.assertNotEmpty("serverUrl", serverUrl);

        try
        {
            this.serverUrl = new URL(serverUrl);
        } catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Invalid url: " + serverUrl);
        }
    }

    DefaultHttpCmpRequestor(ConcurrentContentSigner requestor,
            X509Certificate responderCert,
            X509Certificate caCert,
            String serverUrl,
            SecurityFactory securityFactory,
            boolean signRequest)
    {
        super(requestor, responderCert, caCert, securityFactory, signRequest);
        ParamChecker.assertNotEmpty("serverUrl", serverUrl);

        try
        {
            this.serverUrl = new URL(serverUrl);
        } catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Invalid url: " + serverUrl);
        }
    }

    @Override
    public byte[] send(byte[] request)
    throws IOException
    {
        HttpURLConnection httpUrlConnection = (HttpURLConnection) serverUrl.openConnection();
        httpUrlConnection.setDoOutput(true);
        httpUrlConnection.setUseCaches(false);

        int size = request.length;

        httpUrlConnection.setRequestMethod("POST");
        httpUrlConnection.setRequestProperty("Content-Type", CMP_REQUEST_MIMETYPE);
        httpUrlConnection.setRequestProperty("Content-Length", java.lang.Integer.toString(size));
        OutputStream outputstream = httpUrlConnection.getOutputStream();
        outputstream.write(request);
        outputstream.flush();
        InputStream inputstream = httpUrlConnection.getInputStream();
        try
        {
            if (httpUrlConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
            {
                throw new IOException("Bad Response: "
                        + httpUrlConnection.getResponseCode() + "  "
                        + httpUrlConnection.getResponseMessage());
            }
            String responseContentType = httpUrlConnection.getContentType();
            boolean isValidContentType = false;
            if (responseContentType != null)
            {
                if (responseContentType.equalsIgnoreCase(CMP_RESPONSE_MIMETYPE))
                {
                    isValidContentType = true;
                }
            }
            if (isValidContentType == false)
            {
                throw new IOException("Bad Response: Mime type "
                        + responseContentType
                        + " not supported!");
            }

            byte[] buf = new byte[4096];
            ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
            do
            {
                int j = inputstream.read(buf);
                if (j == -1)
                {
                    break;
                }
                bytearrayoutputstream.write(buf, 0, j);
            } while (true);

            return bytearrayoutputstream.toByteArray();
        }finally
        {
            inputstream.close();
        }
    }

}
