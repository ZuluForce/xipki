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

package org.xipki.pki.ca.client.shell;

import java.util.Set;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.xipki.common.HealthCheckResult;
import org.xipki.console.karaf.IllegalCmdParamException;

/**
 * @author Lijun Liao
 */

@Command(scope = "xipki-cli", name = "health",
        description = "check healty status of CA")
public class HealthCmd extends ClientCmd
{

    @Option(name = "--ca",
            description = "CA name\n"
                    + "(required if multiple CAs are configured)")
    private String caName;

    @Option(name = "--verbose", aliases = "-v",
            description = "show status verbosely")
    private Boolean verbose = Boolean.FALSE;

    @Override
    protected Object _doExecute()
    throws Exception
    {
        Set<String> caNames = caClient.getCaNames();
        if (isEmpty(caNames))
        {
            throw new IllegalCmdParamException("no CA is configured");
        }

        if (caName != null && !caNames.contains(caName))
        {
            throw new IllegalCmdParamException("CA " + caName + " is not within the configured CAs "
                    + caNames);
        }

        if (caName == null)
        {
            if (caNames.size() == 1)
            {
                caName = caNames.iterator().next();
            } else
            {
                throw new IllegalCmdParamException("no caname is specified, one of " + caNames
                        + " is required");
            }
        }

        HealthCheckResult healthResult = caClient.getHealthCheckResult(caName);
        StringBuilder sb = new StringBuilder();
        sb.append("healthy status for CA ");
        sb.append(caName);
        sb.append(": ");
        String healthyText = healthResult.isHealthy()
                ? "healthy"
                : "not healthy";
        sb.append(healthyText);
        if (verbose.booleanValue())
        {
            sb.append("\n").append(healthResult.toJsonMessage(true));
        }
        System.out.println(sb.toString());
        return null;
    }

}
