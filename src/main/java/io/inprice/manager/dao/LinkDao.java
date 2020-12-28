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
import io.inprice.common.models.Link;

public interface LinkDao {

  @SqlQuery(
    "select l.*, p.price as product_price from link as l " + 
    "inner join account as c on c.id = l.account_id " + 
    "left join product as p on p.id = l.product_id " + 
    "where c.status in (<activeAccountStatuses>) " + 
    "  and l.status=:status " + 
    "  and (<extraCondition> l.last_check < now() - interval <interval> minute) " + 
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByStatus(@BindList("activeAccountStatuses") List<String> activeAccountStatuses, @Bind("status") String status,
    @Define("interval") int interval, @Define("limit") int limit, @Define("extraCondition") String extraCondition);

  @SqlQuery(
    "select l.*, p.price as product_price from link as l " + 
    "inner join account as c on c.id = l.account_id " + 
    "left join product as p on p.id = l.product_id " + 
    "where c.status in (<activeAccountStatuses>) " + 
    "  and l.status=:status " + 
    "  and l.retry < <retry> " + 
    "  and l.last_check < now() - interval <interval> minute " + 
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findFailedListByStatus(@BindList("activeAccountStatuses") List<String> activeAccountStatuses, @Bind("status") String status, 
    @Define("interval") int interval, @Define("retry") int retry, @Define("limit") int limit);

  @Transaction
  @SqlUpdate("update link set last_check=now() where id in (<linkIds>)")
  void bulkUpdateLastCheck(@BindList("linkIds") List<Long> linkIds);

  @Transaction
  @SqlUpdate("delete from link where import_detail_id is not null and (status =:status or retry >= <retryLimit>)")
  int deleteImportedLinks(@Bind("status") String status, @Define("retryLimit") int retryLimit);

}
