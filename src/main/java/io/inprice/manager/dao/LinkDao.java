package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.models.Link;
import io.inprice.common.repository.AlarmDao;

public interface LinkDao {

	@SqlQuery(
  	"select l.*" + AlarmDao.FIELDS + " from link " + 
    "left join alarm as al on al.id = l.alarm_id " + 
		"where l.status = 'TOBE_CLASSIFIED' " + 
		"  and l.checked_at is null " +
		"limit 100"
	)
	@UseRowMapper(LinkMapper.class)
	List<Link> findNewlyAddedLinks();

  @SqlQuery(
  	"select l.*" + AlarmDao.FIELDS + " from link " + 
    "inner join account as a on a.id = l.account_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "where a.status in ('FREE', 'COUPONED', 'SUBSCRIBED') " +
    "  and l.status in ('AVAILABLE', 'RESOLVED') " +
    "  and l.checked_at <= now() - interval <interval> <timeUnit> " +
    "  and l.retry = <retry> " +
    "limit 100"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findActiveLinks(@Define("retry") int retry, @Define("interval") int interval, @Define("timeUnit") String timeUnit);

  @SqlQuery(
  	"select l.*" + AlarmDao.FIELDS + " from link " + 
    "inner join account as a on a.id = l.account_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "where a.status in ('FREE', 'COUPONED', 'SUBSCRIBED') " +
    "  and l.status in ('NOT_AVAILABLE', 'NETWORK_ERROR') " +
    "  and l.checked_at <= now() - interval <interval> <timeUnit> " +
    "  and l.retry = <retry> " +
    "limit 100"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findFailedLinks(@Define("retry") int retry, @Define("interval") int interval, @Define("timeUnit") String timeUnit);

  @Transaction
  @SqlUpdate("update link set checked_at=now() where id in (<linkIds>)")
  void bulkUpdateCheckedAt(@BindList("linkIds") List<Long> linkIds);

}
