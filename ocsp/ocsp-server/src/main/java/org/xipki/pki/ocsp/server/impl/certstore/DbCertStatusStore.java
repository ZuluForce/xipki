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

package org.xipki.pki.ocsp.server.impl.certstore;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.common.util.LogUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.datasource.api.DataSourceWrapper;
import org.xipki.datasource.api.exception.DataAccessException;
import org.xipki.pki.ocsp.api.CertStatusInfo;
import org.xipki.pki.ocsp.api.CertStatusStore;
import org.xipki.pki.ocsp.api.CertStatusStoreException;
import org.xipki.pki.ocsp.api.CertprofileOption;
import org.xipki.pki.ocsp.api.IssuerHashNameAndKey;
import org.xipki.pki.ocsp.server.impl.IssuerEntry;
import org.xipki.pki.ocsp.server.impl.IssuerStore;
import org.xipki.security.api.CertRevocationInfo;
import org.xipki.security.api.HashAlgoType;

/**
 * @author Lijun Liao
 */

public class DbCertStatusStore extends CertStatusStore
{
    private static final Logger LOG = LoggerFactory.getLogger(DbCertStatusStore.class);
    private static final String sqlCs = "REV,RR,RT,RIT,PN FROM CERT WHERE IID=? AND SN=?";
    private static final Map<HashAlgoType, String> sqlCsHashMap = new HashMap<>();

    static
    {
        for (HashAlgoType h : HashAlgoType.values())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("ID,REV,RR,RT,RIT,PN,");
            sb.append(h.getShortName()).append(" ");
            sb.append(" FROM CERT INNER JOIN CHASH ON ");
            sb.append(" CERT.IID=? AND CERT.SN=? AND CERT.ID=CHASH.CID");
            sqlCsHashMap.put(h, sb.toString());
        }
    }

    private static class SimpleIssuerEntry
    {
        private final int id;
        private final Long revocationTimeMs;

        public SimpleIssuerEntry(
                final int id,
                final Long revocationTimeMs)
        {
            this.id = id;
            this.revocationTimeMs = revocationTimeMs;
        }

        public boolean match(
                final IssuerEntry issuer)
        {
            if (id != issuer.getId())
            {
                return false;
            }

            if (revocationTimeMs == null)
            {
                return issuer.getRevocationInfo() == null;
            }

            if (issuer.getRevocationInfo() == null)
            {
                return false;
            }

            return revocationTimeMs == issuer.getRevocationInfo().getRevocationTime().getTime();
        }
    }

    private class StoreUpdateService implements Runnable
    {
        @Override
        public void run()
        {
            initIssuerStore();
        }
    }

    private DataSourceWrapper dataSource;
    private final IssuerFilter issuerFilter;

    private IssuerStore issuerStore;

    private boolean initialized = false;
    private boolean initializationFailed = false;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public DbCertStatusStore(
            final String name,
            final IssuerFilter issuerFilter)
    {
        super(name);
        ParamUtil.assertNotNull("issuerFilter", issuerFilter);
        this.issuerFilter = issuerFilter;
    }

    private synchronized void initIssuerStore()
    {
        try
        {
            if (initialized)
            {
                final String sql = "SELECT ID,REV,RT,S1C FROM ISSUER";
                PreparedStatement ps = borrowPreparedStatement(sql);
                ResultSet rs = null;

                try
                {
                    Map<Integer, SimpleIssuerEntry> newIssuers = new HashMap<>();

                    rs = ps.executeQuery();
                    while (rs.next())
                    {
                        String sha1Fp = rs.getString("S1C");
                        if (!issuerFilter.includeIssuerWithSha1Fp(sha1Fp))
                        {
                            continue;
                        }

                        int id = rs.getInt("ID");
                        boolean revoked = rs.getBoolean("REV");
                        Long revTimeMs = null;
                        if (revoked)
                        {
                            revTimeMs = rs.getLong("RT") * 1000;
                        }

                        SimpleIssuerEntry issuerEntry = new SimpleIssuerEntry(id, revTimeMs);
                        newIssuers.put(id, issuerEntry);
                    }

                    // no change in the issuerStore
                    Set<Integer> newIds = newIssuers.keySet();

                    Set<Integer> ids;
                    if (issuerStore != null)
                    {
                        ids = issuerStore.getIds();
                    } else
                    {
                        ids = Collections.emptySet();
                    }

                    boolean issuersUnchanged =
                            ids.size() == newIds.size()
                            && ids.containsAll(newIds)
                            && newIds.containsAll(ids);

                    if (issuersUnchanged)
                    {
                        for (Integer id : newIds)
                        {
                            IssuerEntry entry = issuerStore.getIssuerForId(id);
                            SimpleIssuerEntry newEntry = newIssuers.get(id);
                            if (newEntry.match(entry))
                            {
                                issuersUnchanged = false;
                                break;
                            }
                        }
                    }

                    if (issuersUnchanged)
                    {
                        return;
                    }
                } finally
                {
                    releaseDbResources(ps, rs);
                }
            }

            HashAlgoType[] hashAlgoTypes = {HashAlgoType.SHA1, HashAlgoType.SHA224,
                    HashAlgoType.SHA256, HashAlgoType.SHA384, HashAlgoType.SHA512};

            final String sql = "SELECT ID,NBEFORE,REV,RT,S1C,S1S,S1K,S224S,S224K,S256S,S256K,"
                    + "S384S,S384K,S512S,S512K FROM ISSUER";
            PreparedStatement ps = borrowPreparedStatement(sql);

            ResultSet rs = null;
            try
            {
                rs = ps.executeQuery();
                List<IssuerEntry> caInfos = new LinkedList<>();
                while (rs.next())
                {
                    String sha1Fp = rs.getString("S1C");
                    if (!issuerFilter.includeIssuerWithSha1Fp(sha1Fp))
                    {
                        continue;
                    }

                    int id = rs.getInt("ID");
                    long notBeforeInSecond = rs.getLong("NBEFORE");

                    Map<HashAlgoType, IssuerHashNameAndKey> hashes = new HashMap<>();
                    for (HashAlgoType h : hashAlgoTypes)
                    {
                        String hash_name = rs.getString(h.getShortName() + "S");
                        String hash_key = rs.getString(h.getShortName() + "K");
                        byte[] hashNameBytes = Base64.decode(hash_name);
                        byte[] hashKeyBytes = Base64.decode(hash_key);
                        IssuerHashNameAndKey hash = new IssuerHashNameAndKey(
                                h, hashNameBytes, hashKeyBytes);

                        if (h == HashAlgoType.SHA1)
                        {
                            for (IssuerEntry existingIssuer : caInfos)
                            {
                                if (existingIssuer.matchHash(h, hashNameBytes, hashKeyBytes))
                                {
                                    throw new Exception("found at least two issuers with the"
                                            + " same subject and key");
                                }
                            }
                        }
                        hashes.put(h, hash);
                    }

                    IssuerEntry caInfoEntry = new IssuerEntry(id, hashes,
                            new Date(notBeforeInSecond * 1000));
                    boolean revoked = rs.getBoolean("REV");
                    if (revoked)
                    {
                        long l = rs.getLong("RT");
                        caInfoEntry.setRevocationInfo(new Date(l * 1000));
                    }

                    caInfos.add(caInfoEntry);
                }

                initialized = false;
                this.issuerStore = new IssuerStore(caInfos);
                LOG.info("Updated CertStore: {}", getName());
                initializationFailed = false;
                initialized = true;
            } finally
            {
                releaseDbResources(ps, rs);
            }
        } catch (Exception e)
        {
            final String message = "could not executing initializeStore()";
            if (LOG.isErrorEnabled())
            {
                LOG.error(LogUtil.buildExceptionLogFormat(message), e.getClass().getName(),
                        e.getMessage());
            }
            LOG.debug(message, e);
            initializationFailed = true;
            initialized = true;
        }
    }

    @Override
    public CertStatusInfo getCertStatus(
            final HashAlgoType hashAlgo,
            final byte[] issuerNameHash,
            final byte[] issuerKeyHash,
            final BigInteger serialNumber,
            final boolean includeCertHash,
            final HashAlgoType certHashAlg,
            final CertprofileOption certprofileOption)
    throws CertStatusStoreException
    {
        // wait for max. 0.5 second
        int n = 5;
        while (!initialized && (n-- > 0))
        {
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
            }
        }

        if (!initialized)
        {
            throw new CertStatusStoreException("initialization of CertStore is still in process");
        }

        if (initializationFailed)
        {
            throw new CertStatusStoreException("initialization of CertStore failed");
        }

        String coreSql;
        HashAlgoType certHashAlgo = null;
        if (includeCertHash)
        {
            certHashAlgo = (certHashAlg == null)
                    ? hashAlgo
                    : certHashAlg;
            coreSql = sqlCsHashMap.get(certHashAlgo);
        } else
        {
            coreSql = sqlCs;
        }

        try
        {
            Date thisUpdate = new Date();

            IssuerEntry issuer = issuerStore.getIssuerForFp(hashAlgo, issuerNameHash,
                    issuerKeyHash);
            if (issuer == null)
            {
                return CertStatusInfo.getIssuerUnknownCertStatusInfo(thisUpdate, null);
            }

            // our database supports up to 63 bit (8 byte positive) serialNumber
            if (serialNumber.bitLength() > 63)
            {
                return CertStatusInfo.getUnknownCertStatusInfo(thisUpdate, null);
            }

            ResultSet rs = null;
            CertStatusInfo certStatusInfo = null;

            PreparedStatement ps = borrowPreparedStatement(
                    dataSource.createFetchFirstSelectSQL(coreSql, 1));

            try
            {
                ps.setInt(1, issuer.getId());
                ps.setLong(2, serialNumber.longValue());

                rs = ps.executeQuery();

                if (rs.next())
                {
                    String certprofile = rs.getString("PN");
                    boolean ignore = certprofile != null
                            && certprofileOption != null
                            && !certprofileOption.include(certprofile);
                    if (ignore)
                    {
                        certStatusInfo = CertStatusInfo.getIgnoreCertStatusInfo(thisUpdate, null);
                    } else
                    {
                        byte[] certHash = null;
                        if (includeCertHash)
                        {
                            certHash = Base64.decode(rs.getString(certHashAlgo.getShortName()));
                        }

                        boolean revoked = rs.getBoolean("REV");
                        if (revoked)
                        {
                            int reason = rs.getInt("RR");
                            long revocationTime = rs.getLong("RT");
                            long invalidatityTime = rs.getLong("RIT");

                            Date invTime = null;
                            if (invalidatityTime != 0 && invalidatityTime != revocationTime)
                            {
                                invTime = new Date(invalidatityTime * 1000);
                            }
                            CertRevocationInfo revInfo = new CertRevocationInfo(reason,
                                    new Date(revocationTime * 1000),
                                    invTime);
                            certStatusInfo = CertStatusInfo.getRevokedCertStatusInfo(revInfo,
                                    certHashAlgo, certHash,
                                    thisUpdate, null, certprofile);
                        } else
                        {
                            certStatusInfo = CertStatusInfo.getGoodCertStatusInfo(certHashAlgo,
                                    certHash, thisUpdate,
                                    null, certprofile);
                        }
                    } // end if (ignore)
                } else
                {
                    if (isUnknownSerialAsGood())
                    {
                        certStatusInfo = CertStatusInfo.getGoodCertStatusInfo(certHashAlgo, null,
                                thisUpdate, null, null);
                    } else
                    {
                        certStatusInfo = CertStatusInfo.getUnknownCertStatusInfo(thisUpdate, null);
                    }
                } // end if (rs.next())
            } catch (SQLException e)
            {
                throw dataSource.translate(coreSql, e);
            } finally
            {
                releaseDbResources(ps, rs);
            }

            if (isIncludeArchiveCutoff())
            {
                int retentionInterval = getRetentionInterval();
                Date t;
                if (retentionInterval != 0)
                {
                    // expired certificate remains in status store for ever
                    if (retentionInterval < 0)
                    {
                        t = issuer.getNotBefore();
                    } else
                    {
                        long nowInMs = System.currentTimeMillis();
                        long tInMs = Math.max(issuer.getNotBefore().getTime(),
                                nowInMs - DAY * retentionInterval);
                        t = new Date(tInMs);
                    }

                    certStatusInfo.setArchiveCutOff(t);
                }
            }

            return certStatusInfo;
        } catch (DataAccessException e)
        {
            throw new CertStatusStoreException(e.getMessage(), e);
        }
    }

    /**
     *
     * @return the next idle preparedStatement, {@code null} will be returned
     *         if no PreparedStament can be created within 5 seconds
     * @throws DataAccessException
     */
    private PreparedStatement borrowPreparedStatement(
            final String sqlQuery)
    throws DataAccessException
    {
        PreparedStatement ps = null;
        Connection c = dataSource.getConnection();
        if (c != null)
        {
            ps = dataSource.prepareStatement(c, sqlQuery);
        }
        if (ps == null)
        {
            throw new DataAccessException("could not create prepared statement for " + sqlQuery);
        }
        return ps;
    }

    @Override
    public boolean isHealthy()
    {
        final String sql = "SELECT ID FROM ISSUER";

        try
        {
            PreparedStatement ps = borrowPreparedStatement(sql);
            ResultSet rs = null;
            try
            {
                rs = ps.executeQuery();
                return true;
            } finally
            {
                releaseDbResources(ps, rs);
            }
        } catch (Exception e)
        {
            final String message = "isHealthy()";
            if (LOG.isErrorEnabled())
            {
                LOG.error(LogUtil.buildExceptionLogFormat(message), e.getClass().getName(),
                        e.getMessage());
            }
            LOG.debug(message, e);
            return false;
        }
    }

    private void releaseDbResources(
            final Statement ps,
            final ResultSet rs)
    {
        dataSource.releaseResources(ps, rs);
    }

    @Override
    public void init(
            final String conf,
            final DataSourceWrapper datasource)
    throws CertStatusStoreException
    {
        ParamUtil.assertNotNull("datasource", datasource);
        this.dataSource = datasource;
        initIssuerStore();

        StoreUpdateService storeUpdateService = new StoreUpdateService();
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                storeUpdateService, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown()
    throws CertStatusStoreException
    {
        if (scheduledThreadPoolExecutor != null)
        {
            scheduledThreadPoolExecutor.shutdown();
            scheduledThreadPoolExecutor = null;
        }
    }

    @Override
    public boolean canResolveIssuer(
            final HashAlgoType hashAlgo,
            final byte[] issuerNameHash,
            final byte[] issuerKeyHash)
    {
        return null != issuerStore.getIssuerForFp(hashAlgo, issuerNameHash, issuerKeyHash);
    }

    @Override
    public Set<IssuerHashNameAndKey> getIssuerHashNameAndKeys()
    {
        return issuerStore.getIssuerHashNameAndKeys();
    }

    @Override
    public CertRevocationInfo getCARevocationInfo(
            final HashAlgoType hashAlgo,
            final byte[] issuerNameHash,
            final byte[] issuerKeyHash)
    {
        IssuerEntry issuer = issuerStore.getIssuerForFp(hashAlgo, issuerNameHash, issuerKeyHash);
        return (issuer == null)
                ? null
                : issuer.getRevocationInfo();
    }

}
