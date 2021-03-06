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

package org.xipki.pki.ca.server.mgmt.qa.shell;

import org.apache.karaf.shell.commands.Command;
import org.xipki.pki.ca.server.mgmt.api.CRLControl;
import org.xipki.pki.ca.server.mgmt.api.X509ChangeCrlSignerEntry;
import org.xipki.pki.ca.server.mgmt.api.X509CrlSignerEntry;
import org.xipki.pki.ca.server.mgmt.shell.CrlSignerUpdateCmd;
import org.xipki.console.karaf.CmdFailure;

/**
 * @author Lijun Liao
 */

@Command(scope = "xipki-caqa", name = "crlsigner-check",
        description = "check information of CRL signers (QA)")
public class CrlSignerCheckCmd extends CrlSignerUpdateCmd
{
    @Override
    protected Object _doExecute()
    throws Exception
    {
        X509ChangeCrlSignerEntry ey = getCrlSignerChangeEntry();
        String name = ey.getName();
        out("checking CRL signer " + name);

        X509CrlSignerEntry cs = caManager.getCrlSigner(name);
        if (cs == null)
        {
            throw new CmdFailure("CRL signer named '" + name + "' is not configured");
        }

        if (ey.getSignerType() != null)
        {
            String ex = ey.getSignerType();
            String is = cs.getType();
            MgmtQAShellUtil.assertEquals("signer type", ex, is);
        }

        if (ey.getSignerConf() != null)
        {
            String ex = ey.getSignerConf();
            String is = cs.getConf();
            MgmtQAShellUtil.assertEquals("signer conf", ex, is);
        }

        if (ey.getCrlControl() != null)
        {
            CRLControl ex = new CRLControl(ey.getCrlControl());
            CRLControl is = new CRLControl(cs.getCrlControl());

            if (!ex.equals(is))
            {
                throw new CmdFailure("CRL control: is '" + is.getConf()
                    + "', but expected '" + ex.getConf() + "'");
            }
        }

        if (ey.getBase64Cert() != null)
        {
            String ex = ey.getBase64Cert();
            String is = cs.getBase64Cert();
            MgmtQAShellUtil.assertEquals("certificate", ex, is);
        }

        out(" checked CRL signer " + name);
        return null;
    }
}
