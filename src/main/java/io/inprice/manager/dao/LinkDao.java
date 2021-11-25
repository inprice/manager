package io.inprice.manager.dao;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.meta.Grup;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.repository.PlatformDao;

public interface LinkDao {

	//used for alarm checks in StatusChanginLinks
	static final String PRODUCT_FIELDS = ", p.position as product_position, p.price as product_price, p.base_price as product_base_price ";

  @SqlQuery(
  	"select l.*" + PRODUCT_FIELDS + PlatformDao.FIELDS + " from link as l " + 
		"inner join product as p on p.id = l.product_id " + 
    "inner join workspace as w on w.id = l.workspace_id " + 
    "left join platform as pl on pl.id = l.platform_id " + 
    "where w.status in ('FREE', 'VOUCHERED', 'SUBSCRIBED') " +
    "  and l.status = 'TOBE_CLASSIFIED' " +
    "  and (l.checked_at is null OR l.checked_at <= (now() - interval <reviewPeriod> minute)) " +
    "  and l.retry = <retry> " +
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findTobeClassifiedLinks(@Define("retry") int retry, @Define("limit") int limit, @Define("reviewPeriod") int reviewPeriod);

  @SqlQuery(
  	"select l.*" + PRODUCT_FIELDS + PlatformDao.FIELDS + " from link as l " + 
		"inner join product as p on p.id = l.product_id " + 
    "inner join workspace as w on w.id = l.workspace_id " + 
    "left join platform as pl on pl.id = l.platform_id " + 
    "where w.status in ('FREE', 'VOUCHERED', 'SUBSCRIBED') " +
    "  and l.grup = :grup " +
    "  and l.checked_at <= (now() - interval <reviewPeriod> minute) " +
    "  and l.retry = <retry> " +
    "limit <limit>"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findActiveOrTryingLinks(@Bind("grup") Grup grup, @Define("retry") int retry, @Define("limit") int limit, @Define("reviewPeriod") int reviewPeriod);

  @SqlUpdate("update link set checked_at=now() where id in (<linkIds>)")
  void bulkUpdateCheckedAt(@BindList("linkIds") Set<Long> linkIds);

  @SqlUpdate("update link set platform_id=:platformId, status=:status where id=:linkId")
  void setPlatform(@Bind("linkId") Long linkId, @Bind("platformId") Long platformId, @Bind("status") LinkStatus status);

  @SqlQuery(
		"select * from link " +
		"where url_hash=:urlHash " +
		"  and status != 'TOBE_CLASSIFIED' " +
		"  and checked_at >= (now() - interval 7 day) " +
		"order by checked_at desc, platform_id, grup " +
    "limit 1"
	)
  @UseRowMapper(LinkMapper.class)
  Link findSameLinkByUrlHash(@Bind("urlHash") String urlHash);

}
