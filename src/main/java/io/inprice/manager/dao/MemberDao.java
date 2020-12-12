package io.inprice.manager.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface MemberDao {

  @SqlUpdate("delete from member where status=:status and updated_at <= now() - interval 3 hour")
  int permenantlyDelete(@Bind("status") String status);

}
