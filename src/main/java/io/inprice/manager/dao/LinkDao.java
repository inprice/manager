package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.models.Link;

public interface LinkDao {

  @SqlQuery(
    "select * from link " + 
    "where active=true " + 
    "  and status=:status " + 
    "  and last_check < now() - interval 30 minute " + 
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByStatus(@Bind("status") String status, @Define("limit") int limit);

  @SqlQuery(
    "select * from link as l " + 
    "where active=true " + 
    "  and status=:status " + 
    "  and retry < <retry> " + 
    "  and last_check < now() - interval 30 minute " + 
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findFailedListByStatus(@Bind("status") String status, @Define("retry") int retry, @Define("limit") int limit);

}
