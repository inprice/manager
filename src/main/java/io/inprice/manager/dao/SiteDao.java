package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.SiteMapper;
import io.inprice.common.models.Site;

public interface SiteDao {

  @SqlQuery("select * from site where active = true")
  @UseRowMapper(SiteMapper.class)
  List<Site> findAll();

}
