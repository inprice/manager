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
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;

public interface LinkDao {

  @SqlQuery(
    "select l.*, p.price as product_price from link as l " + 
    "inner join account as a on a.id = l.account_id " + 
    "left join product as p on p.id = l.product_id " + 
    "where a.status in (<accountStatuses>) " + 
    "  and l.status in (<linkStatuses>) " + 
    "  and <extraCondition> " + 
    "limit 100"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByStatus(@BindList("accountStatuses") List<AccountStatus> accountStatuses, 
		@BindList("linkStatuses") List<LinkStatus> linkStatuses, @Define("extraCondition") String extraCondition);

  @Transaction
  @SqlUpdate("update link set last_check=now() where id in (<linkIds>)")
  void bulkUpdateLastCheck(@BindList("linkIds") List<Long> linkIds);

  @Transaction
  @SqlUpdate("delete from link where import_detail_id is not null and (status =:status or retry >= <retryLimit>)")
  int deleteImportedLinks(@Bind("status") String status, @Define("retryLimit") int retryLimit);

}
