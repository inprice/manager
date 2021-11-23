package io.inprice.manager.dao;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface CheckoutDao {

  @SqlUpdate(
    "update checkout " +
    "set status='EXPIRED', description='Expired by the platform.', updated_at=now() " +
    "where status = 'PENDING' " + 
    "  and created_at <= now() - interval 2 hour"
  )
  int expirePendings();

}
