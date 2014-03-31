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

package org.xipki.ca.client.impl;

import org.xipki.security.common.ParamChecker;

class RACertProfileMapping {
	private final String requestedProfile;
	private final String destProfile;
	private final String destCA;
	
	RACertProfileMapping(String requestedProfile, String destProfile, String destCA) 
	{
		ParamChecker.assertNotNull("requestedProfile", requestedProfile);
		ParamChecker.assertNotNull("destProfile", destProfile);
		ParamChecker.assertNotNull("destCA", destCA);

		this.requestedProfile = requestedProfile;
		this.destProfile = destProfile;
		this.destCA = destCA;
	}

	public String getRequestedProfile() {
		return requestedProfile;
	}

	public String getDestProfile() {
		return destProfile;
	}

	public String getDestCA() {
		return destCA;
	}	

}
