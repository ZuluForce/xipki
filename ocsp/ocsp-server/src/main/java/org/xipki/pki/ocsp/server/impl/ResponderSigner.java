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

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSASSAPSSparams;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.api.ConcurrentContentSigner;

/**
 * @author Lijun Liao
 */

class ResponderSigner
{
    private final Map<String, ConcurrentContentSigner> algoSignerMap;
    private final List<ConcurrentContentSigner> signers;

    private final X509CertificateHolder bcCertificate;
    private final X509Certificate certificate;
    private final X509CertificateHolder[] bcCertificateChain;
    private final X509Certificate[] certificateChain;

    private final X500Name responderId;

    public ResponderSigner(
            final List<ConcurrentContentSigner> signers)
    throws CertificateEncodingException, IOException
    {
        ParamUtil.assertNotEmpty("signers", signers);

        this.signers = signers;
        X509Certificate[] _certificateChain = signers.get(0).getCertificateChain();
        int n = _certificateChain.length;
        if (n > 1)
        {
            X509Certificate c = _certificateChain[n - 1];
            if (c.getIssuerX500Principal().equals(c.getSubjectX500Principal()))
            {
                n--;
            }
        }
        this.certificateChain = new X509Certificate[n];
        System.arraycopy(_certificateChain, 0, this.certificateChain, 0, n);

        this.certificate = certificateChain[0];

        this.bcCertificate = new X509CertificateHolder(this.certificate.getEncoded());
        this.bcCertificateChain = new X509CertificateHolder[this.certificateChain.length];
        for (int i = 0; i < certificateChain.length; i++)
        {
            this.bcCertificateChain[i] = new X509CertificateHolder(
                    this.certificateChain[i].getEncoded());
        }

        this.responderId = this.bcCertificate.getSubject();
        algoSignerMap = new HashMap<>();
        for (ConcurrentContentSigner signer : signers)
        {
            String algoName = getSignatureAlgorithmName(signer.getAlgorithmIdentifier());
            algoSignerMap.put(algoName, signer);
        }
    }

    public ConcurrentContentSigner getFirstSigner()
    {
        return signers.get(0);
    }

    public ConcurrentContentSigner getSignerForPreferredSigAlgs(
            final ASN1Sequence preferredSigAlgs)
    {
        if (preferredSigAlgs == null)
        {
            return signers.get(0);
        }

        int size = preferredSigAlgs.size();
        for (int i = 0; i < size; i++)
        {
            ASN1Sequence algObj = (ASN1Sequence) preferredSigAlgs.getObjectAt(i);
            AlgorithmIdentifier sigAlgId = AlgorithmIdentifier.getInstance(algObj.getObjectAt(0));
            String algoName = getSignatureAlgorithmName(sigAlgId);
            if (algoSignerMap.containsKey(algoName))
            {
                return algoSignerMap.get(algoName);
            }
        }
        return null;
    }

    private static String getSignatureAlgorithmName(
            final AlgorithmIdentifier sigAlgId)
    {
        ASN1ObjectIdentifier algOid = sigAlgId.getAlgorithm();
        if (!PKCSObjectIdentifiers.id_RSASSA_PSS.equals(algOid))
        {
            return algOid.getId();
        }

        ASN1Encodable asn1Encodable = sigAlgId.getParameters();
        RSASSAPSSparams param = RSASSAPSSparams.getInstance(asn1Encodable);
        ASN1ObjectIdentifier digestAlgOid = param.getHashAlgorithm().getAlgorithm();
        return digestAlgOid.getId() + "WITHRSAANDMGF1";
    }

    public X500Name getResponderId()
    {
        return responderId;
    }

    public X509Certificate getCertificate()
    {
        return certificate;
    }

    public X509Certificate[] getCertificateChain()
    {
        return certificateChain;
    }

    public X509CertificateHolder getBcCertificate()
    {
        return bcCertificate;
    }

    public X509CertificateHolder[] getBcCertificateChain()
    {
        return bcCertificateChain;
    }

    public boolean isHealthy()
    {
        for (ConcurrentContentSigner signer : signers)
        {
            if (!signer.isHealthy())
            {
                return false;
            }
        }

        return true;
    }

}
