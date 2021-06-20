package io.inprice.manager.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.UserMapper;
import io.inprice.common.models.User;

public interface UserDao {

  @SqlQuery("select * from user where id=:id")
  @UseRowMapper(UserMapper.class)
  User findById(@Bind("id") Long id);

}
