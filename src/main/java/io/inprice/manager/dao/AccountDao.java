package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.AccountMapper;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.models.Account;

public interface AccountDao {

  @SqlUpdate("update account set user_count=user_count-<number> where id=:id")
  boolean decreaseUserCount(@Bind("id") Long id, @Define("number") Integer number);

  @SqlQuery("select * from account where status in (<statusList>) and subs_renewal_at <= now()")
  @UseRowMapper(AccountMapper.class)
  List<Account> findExpiredFreeAccountList(@BindList("statusList") List<String> statusList);

  @SqlUpdate("insert into account_history (account_id, status) values (:accountId, :status)")
  boolean insertStatusHistory(@Bind("accountId") Long accountId, @Bind("status") AccountStatus status);

  // finds only those accounts who have two days remaining
  @SqlQuery(
    "select * from account "+
    "where status in (<statusList>) "+
    "  and TIMESTAMPDIFF(DAY, subs_renewal_at, now()) > 1 "+
    "  and TIMESTAMPDIFF(DAY, subs_renewal_at, now()) < 4"
  )
  @UseRowMapper(AccountMapper.class)
  List<Account> findAboutToExpiredFreeAccountList(@BindList("statusList") List<String> statusList);

}
