package io.inprice.manager.dao;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.meta.Grup;
import io.inprice.common.models.Link;
import io.inprice.common.repository.ProductPriceDao;
import io.inprice.common.repository.PlatformDao;

public interface LinkDao {

  @SqlQuery(
  	"select l.*" + ProductPriceDao.ALARM_FIELDS + PlatformDao.FIELDS + " from link as l " + 
    "inner join workspace as w on w.id = l.workspace_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "left join platform as pl on pl.id = l.platform_id " + 
    "where w.status in ('FREE', 'VOUCHERED', 'SUBSCRIBED') " +
    "  and l.status = 'TOBE_CLASSIFIED' " +
    "  and (l.checked_at is null OR l.checked_at <= (now() - interval <reviewPeriod> minute)) " +
    "  and l.retry = <retry> " +
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findTobeClassifiedLinks(@Define("retry") int retry, @Define("limit") int limit, @Define("reviewPeriod") int reviewPeriod);

  @SqlQuery(
  	"select l.*" + ProductPriceDao.ALARM_FIELDS + PlatformDao.FIELDS + " from link as l " + 
    "inner join workspace as w on w.id = l.workspace_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "left join platform as pl on pl.id = l.platform_id " + 
    "where w.status in ('FREE', 'VOUCHERED', 'SUBSCRIBED') " +
    "  and l.grup = :grup " +
    "  and l.checked_at <= (now() - interval <reviewPeriod> minute) " +
    "  and l.retry = <retry> " +
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findActiveOrTryingLinks(@Bind("grup") Grup grup, @Define("retry") int retry, @Define("limit") int limit, @Define("reviewPeriod") int reviewPeriod);

  @Transaction
  @SqlUpdate(
		"update link set checked_at=now() " +
		"where id in (" +
			"select lid from (" +
				"select l.id as lid from link as l " +
				"inner join workspace as w on w.id = l.workspace_id " + 
				"where w.status in ('FREE', 'VOUCHERED', 'SUBSCRIBED') " +
				"  and l.url_hash in (<linkHashes>)" +
			") AS x " +
		")"
	)
  void bulkUpdateCheckedAt(@BindList("linkHashes") Set<String> linkHashes);

  @SqlUpdate("update link set platform_id=:platformId, status=:status where id=:linkId")
  void setPlatform(@Bind("linkId") Long linkId, @Bind("platformId") Long platformId, @Bind("status") LinkStatus status);

  @SqlQuery(
		"select * from link " +
		"where url_hash=:urlHash " +
		"  and status != 'TOBE_CLASSIFIED' " +
		"  and checked_at >= (now() - interval 7 day) " +
		"order by checked_at desc, platform_id, grup " +
    "limit 1"
	)
  @UseRowMapper(LinkMapper.class)
  Link findSameLinkByUrlHash(@Bind("urlHash") String urlHash);

}
