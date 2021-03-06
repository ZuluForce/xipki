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

package org.xipki.pki.ca.server.mgmt.api;

import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.InvalidConfException;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.api.util.SecurityUtil;
import org.xipki.security.api.util.X509Util;

/**
 * @author Lijun Liao
 */

public class ScepEntry
implements Serializable
{
    private static final Logger LOG = LoggerFactory.getLogger(ScepEntry.class);
    private static final long serialVersionUID = 1L;

    private final String caName;
    private final String control;
    private final String responderType;
    private final String responderConf;

    private final String base64Cert;

    private X509Certificate cert;
    private boolean certFaulty;
    private boolean confFaulty;

    public ScepEntry(
            final String caName,
            final String responderType,
            final String responderConf,
            final String base64Cert,
            final String control)
    throws InvalidConfException
    {
        ParamUtil.assertNotBlank("caName", caName);
        ParamUtil.assertNotBlank("responderType", responderType);
        ParamUtil.assertNotNull("responderConf", responderConf);

        this.base64Cert = base64Cert;

        this.caName = caName.toUpperCase();
        this.responderType = responderType;
        this.responderConf = responderConf;
        this.control = control;

        if (this.base64Cert != null)
        {
            try
            {
                this.cert = X509Util.parseBase64EncodedCert(base64Cert);
            } catch (Throwable t)
            {
                LOG.debug("could not parse the certificate of SCEP responder for CA '"
                        + caName + "'");
                certFaulty = true;
            }
        }
    }

    public X509Certificate getCertificate()
    {
        return cert;
    }

    public String getBase64Cert()
    {
        return base64Cert;
    }

    public String getControl()
    {
        return control;
    }

    public String getResponderType()
    {
        return responderType;
    }

    public String getResponderConf()
    {
        return responderConf;
    }

    public boolean isFaulty()
    {
        return certFaulty || confFaulty;
    }

    public void setConfFaulty(
            boolean faulty)
    {
        this.confFaulty = faulty;
    }

    public String getCaName()
    {
        return caName;
    }

    public void setCertificate(
            final X509Certificate cert)
    {
        if (base64Cert != null)
        {
            throw new IllegalStateException("certificate is already by specified by base64Cert");
        }
        this.cert = cert;
    }

    @Override
    public String toString()
    {
        return toString(false);
    }

    public String toString(
            final boolean verbose)
    {
        return toString(verbose, true);
    }

    public String toString(
            final boolean verbose,
            final boolean ignoreSensitiveInfo)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("caName: ").append(caName).append('\n');
        sb.append("faulty: ").append(isFaulty()).append('\n');
        sb.append("responderType: ").append(responderType).append('\n');
        sb.append("responderConf: ");
        if (responderConf == null)
        {
            sb.append("null");
        } else
        {
            sb.append(SecurityUtil.signerConfToString(responderConf, verbose, ignoreSensitiveInfo));
        }
        sb.append('\n');
        sb.append("control: ").append(control).append("\n");
        if (cert != null)
        {
            sb.append("cert: ").append("\n");
            sb.append("\tissuer: ").append(
                    X509Util.getRFC4519Name(cert.getIssuerX500Principal())).append('\n');
            sb.append("\tserialNumber: ").append(cert.getSerialNumber()).append('\n');
            sb.append("\tsubject: ").append(
                    X509Util.getRFC4519Name(cert.getSubjectX500Principal())).append('\n');

            if (verbose)
            {
                sb.append("\tencoded: ");
                try
                {
                    sb.append(Base64.toBase64String(cert.getEncoded()));
                } catch (CertificateEncodingException e)
                {
                    sb.append("ERROR");
                }
            }
        } else
        {
            sb.append("cert: null\n");
        }

        return sb.toString();
    }

}
