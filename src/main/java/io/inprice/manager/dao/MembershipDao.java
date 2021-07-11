package io.inprice.manager.dao;

import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface MembershipDao {
	
	@SqlQuery(
		"select account_id, count(1) as counter from member " +
		"where status='DELETED' " +
		"  and updated_at <= now() - interval 3 hour " +
		"group by account_id"
	)
  @KeyColumn("account_id")
  @ValueColumn("counter")
  Map<Long, Integer> findAccountInfoOfDeletedMembers();

  @SqlUpdate("delete from member where status='DELETED' and updated_at <= now() - interval 3 hour")
  boolean deletePermenantly();

}
