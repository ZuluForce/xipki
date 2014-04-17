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

package org.xipki.ocsp.client.api;

import java.math.BigInteger;
import java.net.URL;
import java.security.cert.X509Certificate;

import org.bouncycastle.cert.ocsp.BasicOCSPResp;

public interface OCSPRequestor {
    BasicOCSPResp ask(X509Certificate cacert, X509Certificate cert,
            URL responderUrl, RequestOptions requestOptions)
    throws OCSPRequestorException;

    BasicOCSPResp ask(X509Certificate caCert, BigInteger serialNumber,
            URL responderUrl, RequestOptions requestOptions)
    throws OCSPRequestorException;
}
