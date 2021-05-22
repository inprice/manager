package io.inprice.manager.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface AccountDao {

  @SqlUpdate("update account set user_count=user_count-<number> where id=:id")
  boolean decreaseUserCount(@Bind("id") Long id, @Define("number") Integer number);

}
