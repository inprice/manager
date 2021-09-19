package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.WorkspaceMapper;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.models.Workspace;

public interface WorkspaceDao {

  @SqlUpdate("update workspace set user_count=user_count-<number> where id=:id")
  boolean decreaseUserCount(@Bind("id") Long id, @Define("number") Integer number);

  @SqlQuery("select * from workspace where status in (<statusList>) and subs_renewal_at <= now()")
  @UseRowMapper(WorkspaceMapper.class)
  List<Workspace> findExpiredFreeWorkspaceList(@BindList("statusList") List<String> statusList);

  @SqlUpdate("insert into workspace_history (workspace_id, status) values (:workspaceId, :status)")
  boolean insertStatusHistory(@Bind("workspaceId") Long workspaceId, @Bind("status") WorkspaceStatus status);

  // finds only those workspaces who have two days remaining
  @SqlQuery(
    "select * from workspace "+
    "where status in (<statusList>) "+
    "  and TIMESTAMPDIFF(DAY, subs_renewal_at, now()) > 1 "+
    "  and TIMESTAMPDIFF(DAY, subs_renewal_at, now()) < 4"
  )
  @UseRowMapper(WorkspaceMapper.class)
  List<Workspace> findAboutToExpiredFreeWorkspaceList(@BindList("statusList") List<String> statusList);

}
