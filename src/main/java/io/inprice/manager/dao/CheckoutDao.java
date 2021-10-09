package io.inprice.manager.dao;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface CheckoutDao {

  @Transaction
  @SqlUpdate(
    "update checkout " +
    "set status='EXPIRED', description='Expired by the platform.', updated_at=now() " +
    "where status = 'PENDING' " + 
    "  and created_at <= now() - interval 2 hour"
  )
  int expirePendings();

}
