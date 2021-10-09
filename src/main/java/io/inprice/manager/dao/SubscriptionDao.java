package io.inprice.manager.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import io.inprice.common.models.WorkspaceTrans;

public interface SubscriptionDao {

  @SqlUpdate(
    "insert into workspace_trans (workspace_id, event_id, successful, reason, description, file_url, event) " + 
    "values (:trans.workspaceId, :trans.eventId, :trans.successful, :trans.reason, :trans.description, :trans.fileUrl, :event)"
  )
  boolean insertTrans(@BindBean("trans") WorkspaceTrans trans, @Bind("event") String event);

  @SqlUpdate(
    "update workspace " +
    "set subs_renewal_at=null, pre_status=status, status=:status, last_status_update=now() "+
    "where id=:id"
  )
  boolean terminate(@Bind("id") Long id, @Bind("status") String status);

}
