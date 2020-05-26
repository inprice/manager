package io.inprice.scrapper.manager.helpers;

import java.sql.ResultSet;

public interface ModelMapper<M> {

   M map(ResultSet rs);

}
