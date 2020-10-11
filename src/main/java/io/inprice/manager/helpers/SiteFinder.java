package io.inprice.manager.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jdbi.v3.core.Handle;

import io.inprice.common.helpers.Database;
import io.inprice.common.models.Site;
import io.inprice.common.utils.URLUtils;
import io.inprice.manager.dao.SiteDao;

public class SiteFinder {

  private static final Object lock = new Object();
  private static Map<String, Site> sitesByDomain;

  public static Site findSiteByUrl(String url) {
    final String domain = URLUtils.extractDomain(url);
    Site found = null;

    if (domain != null) {
      found = getDomainSiteMap().get(domain);
      if (found == null) { // if not found
        Map<String, Site> sites = getDomainSiteMap();
        for (Map.Entry<String, Site> entry : sites.entrySet()) {
          if (domain.endsWith(entry.getKey())) {
            found = entry.getValue();
            break;
          }
        }
      }
    }
    return found;
  }

  private static Map<String, Site> getDomainSiteMap() {
    if (sitesByDomain == null) {
      synchronized (lock) {
        if (sitesByDomain == null) {
          try (Handle handle = Database.getHandle()) {
            SiteDao siteDao = handle.attach(SiteDao.class);
            List<Site> siteList = siteDao.findAll();
            sitesByDomain = new TreeMap<>(Collections.reverseOrder());
            siteList.forEach(site -> sitesByDomain.put(site.getDomain(), site));
          }
        }
      }
    }
    return sitesByDomain;
  }

}
