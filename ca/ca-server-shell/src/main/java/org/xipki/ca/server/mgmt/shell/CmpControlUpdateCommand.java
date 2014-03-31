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

package org.xipki.ca.server.mgmt.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(scope = "ca", name = "cmpcontrol-update", description="Update CMP control")
public class CmpControlUpdateCommand extends CaCommand {
	@Option(name = "-ecc", aliases = { "--enableConfirmCert" },
            description = "Confirm of certificate is required, the default is not required")
    protected Boolean           enabledConfirmCert;

	@Option(name = "-dcc", aliases = { "--disableConfirmCert" },
            description = "Confirm of certificate is not required")
    protected Boolean           disableConfirmCert;
	
	@Option(name = "-esc", aliases = { "--enableSendCaCert" },
            description = "Enable sending CA certificate in the response, the default is disabled")
    protected Boolean            enableSendCaCert;

	@Option(name = "-dsc", aliases = { "--disableSendCaCert" },
            description = "Disable sending CA certificate in the responsed")
    protected Boolean            disableSendCaCert;

	@Option(name = "-mtb", aliases = { "--msgTimeBias" },
            description = "Message time bias in seconds")
    protected Integer            messageTimeBias;
	
	@Option(name = "-cwt", aliases = { "--confirmWaitTime" },
            description = "Maximal confirmation time in seconds")
    protected Integer            confirmWaitTime;

    @Override
    protected Object doExecute() throws Exception {
		Boolean requireConfirmCert = null;
    	if(enabledConfirmCert != null || disableConfirmCert != null )
    	{
    		requireConfirmCert = isEnabled(enabledConfirmCert, disableConfirmCert, false);
    	}
     	
    	Boolean sendCaCert = null;
    	if(enableSendCaCert != null || disableSendCaCert != null )
    	{
    		sendCaCert = isEnabled(enableSendCaCert, disableSendCaCert, false);
    		System.err.println("Sending CA certificate could not be enabled and disabled at the same time");
    	}
    	    			
		caManager.changeCmpControl(requireConfirmCert, messageTimeBias, confirmWaitTime, sendCaCert);
		
    	return null;
    }
}
