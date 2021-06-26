package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.models.Alarm;

public interface AlarmDao {

	@SqlQuery(
		"select a.*, g.name as _name, u.email, u.name as username, acc.currency_format from alarm a " +
	  "inner join link_group g on g.id = a.group_id " +
	  "inner join account acc on acc.id = a.account_id " +
	  "inner join user u on u.id = acc.admin_id " +
	  "where tobe_notified=true " +
		"union " +
		"select a.*, IFNULL(l.name, l.url) as _name, u.email, u.name as username, acc.currency_format from alarm a " +
	  "inner join link l on l.id = a.link_id " +
	  "inner join account acc on acc.id = a.account_id " +
	  "inner join user u on u.id = acc.admin_id " +
		"where tobe_notified=true " +
		"order by account_id, topic"
	)
  @UseRowMapper(AlarmMapper.class)
	List<Alarm> findTobeNotifiedLit();

  @SqlUpdate("update alarm set tobe_notified=false, notified_at=now() where id in (<idList>) and tobe_notified=true")
  int setNotified(@BindList("idList") List<Long> idList);

}
