package io.inprice.manager.dao;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface CompanyDao {

  @SqlUpdate("update link set active=false where active=true and company_id in (select id from company where subs_renewal_at <= now() - interval 1 day)")
  int inactivateLinks();

}
