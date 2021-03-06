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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.xipki.common.util.DateUtil;
import org.xipki.console.karaf.IllegalCmdParamException;
import org.xipki.security.api.CRLReason;
import org.xipki.security.api.CertRevocationInfo;

/**
 * @author Lijun Liao
 */

@Command(scope = "xipki-ca", name = "ca-revoke",
        description = "revoke CA")
public class CaRevokeCmd extends CaCmd
{
    public static List<CRLReason> permitted_reasons = Collections.unmodifiableList(
            Arrays.asList(new CRLReason[]
            {
                CRLReason.UNSPECIFIED, CRLReason.KEY_COMPROMISE, CRLReason.CA_COMPROMISE,
                CRLReason.AFFILIATION_CHANGED, CRLReason.SUPERSEDED,
                CRLReason.CESSATION_OF_OPERATION,
                CRLReason.CERTIFICATE_HOLD,    CRLReason.PRIVILEGE_WITHDRAWN}));

    @Argument(index = 0, name = "name", description = "CA name", required = true)
    private String caName;

    @Option(name = "--reason",
            required = true,
            description = "CRL reason\n"
                    + "(required)")
    private String reason;

    @Option(name = "--rev-date",
            description = "revocation date, UTC time of format yyyyMMddHHmmss\n"
                    + "(defaults to current time)")
    private String revocationDateS;

    @Option(name = "--inv-date",
            description = "invalidity date, UTC time of format yyyyMMddHHmmss")
    private String invalidityDateS;

    @Override
    protected Object _doExecute()
    throws Exception
    {
        CRLReason crlReason = CRLReason.getInstance(reason);
        if (crlReason == null)
        {
            throw new IllegalCmdParamException("invalid reason " + reason);
        }

        if (!permitted_reasons.contains(crlReason))
        {
            throw new IllegalCmdParamException("reason " + reason + " is not permitted");
        }

        if (!caManager.getCaNames().contains(caName))
        {
            throw new IllegalCmdParamException("invalid CA name " + caName);
        }

        Date revocationDate = null;
        if (isNotBlank(revocationDateS))
        {
            revocationDate = DateUtil.parseUTCTimeyyyyMMddhhmmss(revocationDateS);
        } else
        {
            revocationDate = new Date();
        }

        Date invalidityDate = null;
        if (isNotBlank(invalidityDateS))
        {
            invalidityDate = DateUtil.parseUTCTimeyyyyMMddhhmmss(invalidityDateS);
        }

        CertRevocationInfo revInfo = new CertRevocationInfo(crlReason, revocationDate,
                invalidityDate);
        boolean b = caManager.revokeCa(caName, revInfo);
        output(b, "revoked", "could not revoke", "CA " + caName);
        return null;
    }
}
