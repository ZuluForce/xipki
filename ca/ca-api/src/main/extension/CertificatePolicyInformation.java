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

package lca.ca.profile.extension;

import java.util.List;

public class CertificatePolicyInformation
{
    private String policyId;
    private List<CertificatePolicyQualifier> qualifiers;


    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public void setQualifiers(List<CertificatePolicyQualifier> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public String getPolicyId() {
        return policyId;
    }

    public List<CertificatePolicyQualifier> getQualifiers() {
        return qualifiers;
    }

}
