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

package org.xipki.ca.cmp;

import org.xipki.security.common.ParamChecker;

public class ProtectionVerificationResult {
    private final Object requestor;
    private final ProtectionResult protectionResult;

    public ProtectionVerificationResult(Object requestor, ProtectionResult protectionResult) {
        ParamChecker.assertNotNull("protectionResult", protectionResult);

        this.requestor = requestor;
        this.protectionResult = protectionResult;
    }

    public Object getRequestor() {
        return requestor;
    }

    public ProtectionResult getProtectionResult() {
        return protectionResult;
    }


}
