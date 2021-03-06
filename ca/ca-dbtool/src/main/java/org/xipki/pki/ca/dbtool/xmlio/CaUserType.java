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

package org.xipki.pki.ca.dbtool.xmlio;

import javax.xml.stream.XMLStreamException;

/**
 * @author Lijun Liao
 */

public class CaUserType extends DbDataObject
{
    public static final String TAG_ROOT = "user";

    public static final String TAG_id = "id";
    private Integer id;

    public static final String TAG_name = "name";
    private String name;

    public static final String TAG_password = "password";
    private String password;

    public static final String TAG_cnRegex = "cnRegex";
    private String cnRegex;

    public Integer getId()
    {
        return id;
    }

    public void setId(
            final int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(
            final String name)
    {
        this.name = name;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(
            final String password)
    {
        this.password = password;
    }

    public String getCnRegex()
    {
        return cnRegex;
    }

    public void setCnRegex(
            final String cnRegex)
    {
        this.cnRegex = cnRegex;
    }

    @Override
    public void validate()
    throws InvalidDataObjectException
    {
        assertNotNull("id", id);
        assertNotBlank("name", name);
        assertNotBlank("password", password);
        assertNotBlank("cnRegex", cnRegex);
    }

    @Override
    public void writeTo(
            final DbiXmlWriter writer)
    throws InvalidDataObjectException, XMLStreamException
    {
        validate();

        writer.writeStartElement(TAG_ROOT);
        writeIfNotNull(writer, TAG_id, id);
        writeIfNotNull(writer, TAG_name, name);
        writeIfNotNull(writer, TAG_password, password);
        writeIfNotNull(writer, TAG_cnRegex, cnRegex);
        writer.writeEndElement();
        writer.writeNewline();
    }

}
