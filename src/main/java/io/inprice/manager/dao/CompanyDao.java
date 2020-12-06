package io.inprice.manager.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface CompanyDao {

  @SqlQuery("select id from company where status in (<statusList>) and subs_renewal_at <= now()")
  List<Long> findExpiredFreeCompanyIdList(@BindList("statusList") List<String> statusList);

  @SqlQuery("select id from company where status='SUBSCRIBED' and subs_renewal_at => now() + interval 4 day")
  List<Long> findExpiredSubscribedCompanyIdList();

  @SqlUpdate("update company set status='STOPPED', last_status_update=now() where id=:companyId")
  boolean stopCompany(@Bind("companyId") long companyId);

  @SqlUpdate("insert into company_history (company_id, status) values (:companyId, :status)")
  boolean insertCompanyStatusHistory(@Bind("companyId") Long companyId, @Bind("status") String status);

}
