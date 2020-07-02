package io.inprice.manager.repository;

import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.UserStatus;

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
