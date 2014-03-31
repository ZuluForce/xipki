/*
 * Copyright 2014 xipki.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 *
 */

package org.xipki.ca.cmp.client;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.cmp.CMPCertificate;
import org.bouncycastle.asn1.crmf.ProofOfPossession;
import org.xipki.ca.cmp.client.type.EnrollCertResultEntryType;
import org.xipki.ca.cmp.client.type.EnrollCertResultType;
import org.xipki.ca.cmp.client.type.ErrorResultEntryType;
import org.xipki.ca.cmp.client.type.ErrorResultType;
import org.xipki.ca.cmp.client.type.ResultEntryType;
import org.xipki.ca.common.CertificateOrError;
import org.xipki.ca.common.EnrollCertResult;
import org.xipki.ca.common.PKIErrorException;
import org.xipki.ca.common.RAWorkerException;
import org.xipki.security.api.PasswordResolver;
import org.xipki.security.api.SecurityFactory;

public abstract class AbstractRAWorker
{	
	protected static final ProofOfPossession raVerified = new ProofOfPossession();
	
	protected abstract Certificate getCertificate(CMPCertificate cmpCert)
	throws CertificateException;
	
	protected abstract Certificate getCACertficate(String caname);
	
	protected SecurityFactory securityFactory;	
	protected PasswordResolver passwordResolver;
	
	protected AbstractRAWorker()
	{
	}	

	public void setPasswordResolver(PasswordResolver passwordResolver) {
		this.passwordResolver = passwordResolver;
	}

	protected EnrollCertResult parseEnrollCertResult(EnrollCertResultType result,
			String caname)
	throws RAWorkerException
	{
		Map<String, CertificateOrError> certOrErrors = new HashMap<String, CertificateOrError>();
		for(ResultEntryType resultEntry : result.getResultEntries())
		{
			CertificateOrError certOrError;
			if(resultEntry instanceof EnrollCertResultEntryType)
			{
				EnrollCertResultEntryType entry = (EnrollCertResultEntryType) resultEntry;
				try {
					Certificate cert = getCertificate(entry.getCert());
					certOrError = new CertificateOrError(cert);
				} catch (CertificateException e) {
					throw new RAWorkerException(
							"CertificateParsingException for request (id=" + entry.getId()+"): " + e.getMessage());
				}
			}
			else if(resultEntry instanceof ErrorResultEntryType)
			{
				certOrError = new CertificateOrError(
						((ErrorResultEntryType) resultEntry).getStatusInfo());
			}
			else
			{
				certOrError = null;
			}
			
			certOrErrors.put(resultEntry.getId(), certOrError);			
		}
		
		Certificate caCertificate = getCACertficate(caname);
		
		return new EnrollCertResult(caCertificate, certOrErrors);
	}

	protected static PKIErrorException createPKIErrorException(ErrorResultType errResult)
	{
		return new PKIErrorException(errResult.getStatus(),
				errResult.getPkiFailureInfo(), errResult.getStatusMessage());
	}
	
	public void setSecurityFactory(SecurityFactory securityFactory) {
		this.securityFactory = securityFactory;
	}

	
	
}
