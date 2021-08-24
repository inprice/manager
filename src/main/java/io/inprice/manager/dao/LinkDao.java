package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.meta.LinkStatusGroup;
import io.inprice.common.models.Link;
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.PlatformDao;

public interface LinkDao {

  @SqlQuery(
  	"select l.*" + AlarmDao.FIELDS + PlatformDao.FIELDS + " from link as l " + 
    "inner join account as a on a.id = l.account_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "left join platform as p on p.id = l.platform_id " + 
    "where a.status in ('FREE', 'COUPONED', 'SUBSCRIBED') " +
    "  and l.status = 'TOBE_CLASSIFIED' " +
    "  and (l.checked_at is null OR l.checked_at <= (now() - interval <interval> <period>)) " +
    "  and l.retry = <retry> " +
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findTobeClassifiedLinks(@Define("retry") int retry, @Define("interval") int interval, 
  		@Define("period") String period, @Define("limit") int limit);

  @SqlQuery(
  	"select l.*" + AlarmDao.FIELDS + PlatformDao.FIELDS + " from link as l " + 
    "inner join account as a on a.id = l.account_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "left join platform as p on p.id = l.platform_id " + 
    "where a.status in ('FREE', 'COUPONED', 'SUBSCRIBED') " +
    "  and l.status_group = :statusGroup " +
    "  and l.checked_at <= (now() - interval <interval> <period>) " +
    "  and l.retry = <retry> " +
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findScrappingLinks(@Bind("statusGroup") LinkStatusGroup statusGroup, @Define("retry") int retry,
  		@Define("interval") int interval, @Define("period") String period, @Define("limit") int limit);

  @Transaction
  @SqlUpdate("update link set checked_at=now() where id in (<linkIds>)")
  void bulkUpdateCheckedAt(@BindList("linkIds") List<Long> linkIds);

  @SqlUpdate("update link set platform_id=:platformId, status=:status where id=:linkId")
  void setPlatform(@Bind("linkId") Long linkId, @Bind("platformId") Long platformId, @Bind("status") LinkStatus status);

  @SqlQuery(
		"select l.*" + PlatformDao.FIELDS + " from link as l " +
    "left join platform as p on p.id = l.platform_id " + 
		"where l.url=:url " +
    "  and l.platform_id is not null " +
		"order by l.updated_at desc " +
    "limit 1"
	)
  @UseRowMapper(LinkMapper.class)
  Link findTheSameLinkByUrl(@Bind("url") String url);

}
