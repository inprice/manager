package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.models.Link;

public interface LinkDao {

  @SqlQuery(
    "select * from link as l " + 
    "inner join company as c on c.id = l.company_id " +
    "where l.status=:status " + 
    "  and c.subs_renewal_at >= now() " + 
    "  and l.last_check < now() - interval 30 minute " + 
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByStatus(@Bind("status") String status, @Define("limit") int limit);

  @SqlQuery(
    "select * from link as l " + 
    "inner join company as c on c.id = l.company_id " +
    "where l.status=:status " + 
    "  and c.subs_renewal_at >= now() " + 
    "  and l.retry < <retry> " + 
    "  and l.last_check < now() - interval 30 minute " + 
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findFailedListByStatus(@Bind("status") String status, @Define("retry") int retry, @Define("limit") int limit);

  @SqlBatch(
    "update link " + 
    "set last_check=now(), retry=retry+<increaseNum> " +
    "where id in (<idList>)"
  )
  void setLastCheckTime(@BindList("idList") List<Long> idList, @Define("increaseNum") int increaseNum);

}
