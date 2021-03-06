/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2014 - 2015 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.pki.ocsp.server.impl;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.ocsp.OCSPRequest;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.audit.api.AuditEvent;
import org.xipki.audit.api.AuditEventData;
import org.xipki.audit.api.AuditLevel;
import org.xipki.audit.api.AuditService;
import org.xipki.audit.api.AuditServiceRegister;
import org.xipki.audit.api.AuditStatus;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.StringUtil;
import org.xipki.pki.ocsp.server.impl.OcspRespWithCacheInfo.ResponseCacheInfo;
import org.xipki.security.api.HashCalculator;

/**
 * @author Lijun Liao
 */

public class HttpOcspServlet extends HttpServlet
{
    private final Logger LOG = LoggerFactory.getLogger(HttpOcspServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String CT_REQUEST  = "application/ocsp-request";
    private static final String CT_RESPONSE = "application/ocsp-response";

    private AuditServiceRegister auditServiceRegister;

    private OcspServer server;

    public HttpOcspServlet()
    {
    }

    public void setServer(
            final OcspServer server)
    {
        this.server = server;
    }

    @Override
    protected void doGet(
            final HttpServletRequest request,
            final HttpServletResponse response)
    throws ServletException, IOException
    {
        ResponderAndRelativeUri r = server.getResponderAndRelativeUri(request);
        if (r == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Responder responder = r.getResponder();
        if (responder.getRequestOption().supportsHttpGet())
        {
            processRequest(request, response, r, true);
        } else
        {
            super.doGet(request, response);
        }
    }

    @Override
    protected void doPost(
            final HttpServletRequest request,
            final HttpServletResponse response)
    throws ServletException, IOException
    {
        ResponderAndRelativeUri r = server.getResponderAndRelativeUri(request);
        if (r == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (StringUtil.isNotBlank(r.getRelativeUri()))
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        processRequest(request, response, r, false);
    }

    private void processRequest(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ResponderAndRelativeUri r,
            final boolean getMethod)
    throws ServletException, IOException
    {
        Responder responder = r.getResponder();
        AuditEvent auditEvent = null;

        AuditLevel auditLevel = AuditLevel.INFO;
        AuditStatus auditStatus = AuditStatus.SUCCESSFUL;
        String auditMessage = null;

        long start = 0;

        AuditService auditService = (auditServiceRegister == null)
                ? null
                : auditServiceRegister.getAuditService();

        if (auditService != null && responder.getAuditOption() != null)
        {
            start = System.currentTimeMillis();
            auditEvent = new AuditEvent(new Date());
            auditEvent.setApplicationName("OCSP");
            auditEvent.setName("PERF");
        }

        try
        {
            if (server == null)
            {
                String message = "responder in servlet not configured";
                LOG.error(message);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentLength(0);

                auditLevel = AuditLevel.ERROR;
                auditStatus = AuditStatus.FAILED;
                auditMessage = message;
                return;
            }

            InputStream requestStream;
            if (getMethod)
            {
                String relativeUri = r.getRelativeUri();

                // RFC2560 A.1.1 specifies that request longer than 255 bytes SHOULD be sent by
                // POST, we support GET for longer requests anyway.
                if (relativeUri.length() > responder.getRequestOption().getMaxRequestSize())
                {
                    response.setContentLength(0);
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

                    auditStatus = AuditStatus.FAILED;
                    auditMessage = "request too large";
                    return;
                }

                requestStream = new ByteArrayInputStream(Base64.decode(relativeUri));
            } else
            {
                // accept only "application/ocsp-request" as content type
                if (!CT_REQUEST.equalsIgnoreCase(request.getContentType()))
                {
                    response.setContentLength(0);
                    response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);

                    auditStatus = AuditStatus.FAILED;
                    auditMessage = "unsupporte media type " + request.getContentType();
                    return;
                }

                // request too long
                if (request.getContentLength() > responder.getRequestOption().getMaxRequestSize())
                {
                    response.setContentLength(0);
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

                    auditStatus = AuditStatus.FAILED;
                    auditMessage = "request too large";
                    return;
                } // if (CT_REQUEST)

                requestStream = request.getInputStream();
            } // end if (getMethod)

            OCSPRequest ocspRequest;
            try
            {
                ASN1StreamParser parser = new ASN1StreamParser(requestStream);
                ocspRequest = OCSPRequest.getInstance(parser.readObject());
            } catch (Exception e)
            {
                response.setContentLength(0);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                auditStatus = AuditStatus.FAILED;
                auditMessage = "bad request";

                final String message = "could not parse the request (OCSPRequest)";
                if (LOG.isErrorEnabled())
                {
                    LOG.error(LogUtil.buildExceptionLogFormat(message), e.getClass().getName(),
                            e.getMessage());
                }
                LOG.debug(message, e);

                return;
            }

            OCSPReq ocspReq = new OCSPReq(ocspRequest);

            response.setContentType(HttpOcspServlet.CT_RESPONSE);

            OcspRespWithCacheInfo ocspRespWithCacheInfo =
                    server.answer(responder, ocspReq, auditEvent, getMethod);
            if (ocspRespWithCacheInfo == null)
            {
                auditMessage = "processRequest returned null, this should not happen";
                LOG.error(auditMessage);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentLength(0);

                auditLevel = AuditLevel.ERROR;
                auditStatus = AuditStatus.FAILED;
            } else
            {
                OCSPResp resp = ocspRespWithCacheInfo.getResponse();
                byte[] encodedOcspResp = resp.getEncoded();
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentLength(encodedOcspResp.length);

                ResponseCacheInfo cacheInfo = ocspRespWithCacheInfo.getCacheInfo();
                if (getMethod && cacheInfo != null)
                {
                    long now = System.currentTimeMillis();
                    // RFC 5019 6.2: Date: The date and time at which the OCSP server generated
                    // the HTTP response.
                    response.setDateHeader("Date", now);
                    // RFC 5019 6.2: Last-Modified: date and time at which the OCSP responder
                    // last modified the response.
                    response.setDateHeader("Last-Modified", cacheInfo.getThisUpdate());
                    // RFC 5019 6.2: Expires: This date and time will be the same as the
                    // nextUpdate time-stamp in the OCSP
                    // response itself.
                    // This is overridden by max-age on HTTP/1.1 compatible components
                    if (cacheInfo.getNextUpdate() != null)
                    {
                        response.setDateHeader("Expires", cacheInfo.getNextUpdate());
                    }
                    // RFC 5019 6.2: This profile RECOMMENDS that the ETag value be the ASCII
                    // HEX representation of the SHA1 hash of the OCSPResponse structure.
                    response.setHeader("ETag",
                            "\"" + HashCalculator.hexSha1(encodedOcspResp) + "\"");

                    // Max age must be in seconds in the cache-control header
                    long maxAge;
                    if (responder.getResponseOption().getCacheMaxAge() != null)
                    {
                        maxAge = responder.getResponseOption().getCacheMaxAge().longValue();
                    } else
                    {
                        maxAge = OcspServer.defaultCacheMaxAge;
                    }

                    if (cacheInfo.getNextUpdate() != null)
                    {
                        maxAge = Math.min(maxAge,
                                (cacheInfo.getNextUpdate() - cacheInfo.getThisUpdate()) / 1000);
                    }

                    response.setHeader("Cache-Control", "max-age=" + maxAge
                            + ",public,no-transform,must-revalidate");
                } // end if (getMethod && cacheInfo != null)
                response.getOutputStream().write(encodedOcspResp);
            } // end if (ocspRespWithCacheInfo)
        } catch (EOFException e)
        {
            final String message = "Connection reset by peer";
            if (LOG.isErrorEnabled())
            {
                LOG.warn(LogUtil.buildExceptionLogFormat(message), e.getClass().getName(),
                        e.getMessage());
            }
            LOG.debug(message, e);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        } catch (Throwable t)
        {
            final String message = "Throwable thrown, this should not happen!";
            LOG.error(message, t);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);

            auditLevel = AuditLevel.ERROR;
            auditStatus = AuditStatus.FAILED;
            auditMessage = "internal error";
        }
        finally
        {
            try
            {
                response.flushBuffer();
            } finally
            {
                if (auditEvent != null)
                {
                    if (auditLevel != null)
                    {
                        auditEvent.setLevel(auditLevel);
                    }

                    if (auditStatus != null)
                    {
                        auditEvent.setStatus(auditStatus);
                    }

                    if (auditMessage != null)
                    {
                        auditEvent.addEventData(new AuditEventData("message", auditMessage));
                    }

                    auditEvent.setDuration(System.currentTimeMillis() - start);

                    if (!auditEvent.containsChildAuditEvents())
                    {
                        auditService.logEvent(auditEvent);
                    } else
                    {
                        List<AuditEvent> expandedAuditEvents = auditEvent.expandAuditEvents();
                        for (AuditEvent event : expandedAuditEvents)
                        {
                            auditService.logEvent(event);
                        }
                    }
                } // end if (auditEvent != null)
            } // end inner try
        } // end external try
    }

    public void setAuditServiceRegister(
            final AuditServiceRegister auditServiceRegister)
    {
        this.auditServiceRegister = auditServiceRegister;
    }

}
