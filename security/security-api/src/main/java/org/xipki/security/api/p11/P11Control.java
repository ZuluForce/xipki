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

package org.xipki.security.api.p11;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.ParamUtil;

/**
 * @author Lijun Liao
 */

public class P11Control
{
    private final String defaultModuleName;
    private final Map<String, P11ModuleConf> moduleConfs;
    private final Set<String> moduleNames;

    public P11Control(
            final String defaultModuleName,
            final Set<P11ModuleConf> moduleConfs)
    {
        ParamUtil.assertNotBlank("defaultModuleName", defaultModuleName);

        this.defaultModuleName = defaultModuleName;
        if (CollectionUtil.isEmpty(moduleConfs))
        {
            this.moduleConfs = Collections.emptyMap();
            this.moduleNames = Collections.emptySet();
        } else
        {
            this.moduleConfs = new HashMap<>(moduleConfs.size());
            Set<String> _moduleNames = new HashSet<>();
            for (P11ModuleConf conf : moduleConfs)
            {
                this.moduleConfs.put(conf.getName(), conf);
                _moduleNames.add(conf.getName());
            }
            this.moduleNames = Collections.unmodifiableSet(_moduleNames);
        }
    }

    public String getDefaultModuleName()
    {
        return defaultModuleName;
    }

    public P11ModuleConf getModuleConf(
            final String moduleName)
    {
        return (moduleConfs == null)
                ? null
                : moduleConfs.get(moduleName);
    }

    public Set<String> getModuleNames()
    {
        return moduleNames;
    }

}
