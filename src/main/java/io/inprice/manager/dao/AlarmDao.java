package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.models.Alarm;

public interface AlarmDao {

	final String PRODUCT_ENTITY_FIELDS = ", p.id as entity_id, p.sku as entity_sku, p.name as entity_name, " +
			"p.position as entity_position, p.price as entity_price, p.min_price as entity_min_price, " +
			"p.avg_price as entity_avg_price, p.max_price as entity_max_price ";

	final String LINK_ENTITY_FIELDS = ", l.id, IFNULL(l.seller, ''), IFNULL(l.name, l.url) as entity_name, l.position, l.price, 0, 0, 0 ";

	@SqlQuery("select * from alarm where id=:id")
  @UseRowMapper(AlarmMapper.class)
	Alarm findById(@Bind("id") Long id);

	@SqlQuery(
		"select a.*" + PRODUCT_ENTITY_FIELDS + ", u.email, u.full_name, w.currency_format from alarm a " +
	  "inner join product p on p.alarm_id = a.id " +
	  "inner join workspace w on w.id = a.workspace_id " +
	  "inner join user u on u.id = w.admin_id " +
	  "where p.tobe_alarmed=true " +
		"union " +
		"select a.*" + LINK_ENTITY_FIELDS + ", u.email, u.full_name, w.currency_format from alarm a " +
	  "inner join link l on l.alarm_id = a.id " +
	  "inner join workspace w on w.id = a.workspace_id " +
	  "inner join user u on u.id = w.admin_id " +
	  "where l.tobe_alarmed=true " +
		"order by workspace_id, topic, entity_name"
	)
  @UseRowMapper(AlarmMapper.class)
	List<Alarm> findTobeAlarmedList();

  @SqlUpdate("update <table> set tobe_alarmed=false, alarmed_at=now() where id in (<idList>) and tobe_alarmed=true")
  int setAlarmsOFF(@Define("table") String table, @BindList("idList") List<Long> idList);

}
