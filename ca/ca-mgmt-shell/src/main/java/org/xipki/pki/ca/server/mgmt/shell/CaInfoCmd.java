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

package org.xipki.pki.ca.server.mgmt.shell;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.xipki.pki.ca.server.mgmt.api.CAEntry;
import org.xipki.common.util.CollectionUtil;

/**
 * @author Lijun Liao
 */

@Command(scope = "xipki-ca", name = "ca-info",
        description = "show information of CA")
public class CaInfoCmd extends CaCmd
{
    @Argument(index = 0, name = "name", description = "CA name")
    private String caName;

    @Option(name = "--verbose", aliases = "-v",
            description = "show CA information verbosely")
    private Boolean verbose = Boolean.FALSE;

    @Override
    protected Object _doExecute()
    throws Exception
    {
        StringBuilder sb = new StringBuilder();

        if (caName == null)
        {
            Set<String> names = caManager.getCaNames();
            int n = names.size();
            if (n == 0 || n == 1)
            {
                sb.append((n == 0)
                        ? "no"
                        : "1");
                sb.append(" CA is configured\n");
            } else
            {
                sb.append(n).append(" CAs are configured:\n");
            }

            List<String> sorted = new ArrayList<>(names);
            Collections.sort(sorted);
            for (String paramName : sorted)
            {
                sb.append("\t").append(paramName);
                Set<String> aliases = caManager.getAliasesForCA(paramName);
                if (CollectionUtil.isNotEmpty(aliases))
                {
                    sb.append(" (aliases: ").append(toString(aliases)).append(")");
                }
                sb.append("\n");
            }
        } else
        {
            CAEntry entry = caManager.getCA(caName);
            if (entry == null)
            {
                throw new UnexpectedException("could not find CA '" + caName + "'");
            } else
            {
                Set<String> aliases = caManager.getAliasesForCA(caName);
                sb.append("aliases: ").append(toString(aliases)).append("\n");
                sb.append(entry.toString(verbose.booleanValue()));
            }
        }

        out(sb.toString());
        return null;
    }
}
