package io.inprice.scrapper.manager.repository;

import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.meta.UserStatus;

public class MembershipRepository {

  private static final Database db = Beans.getSingleton(Database.class);

  public boolean deletePermanently() {
    return db.executeQuery(
      String.format(
        "delete from membership where status = '%s' and updated_at <= now() - interval 3 hour ",
        UserStatus.DELETED.name()
      ),
      "Failed to delete memberships permanently"
    );
  }

}
