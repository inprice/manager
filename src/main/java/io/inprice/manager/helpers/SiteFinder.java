package io.inprice.manager.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.inprice.common.helpers.Beans;
import io.inprice.common.models.Site;
import io.inprice.common.utils.URLUtils;
import io.inprice.manager.repository.SiteRepository;

public class SiteFinder {

  private static final SiteRepository repository = Beans.getSingleton(SiteRepository.class);

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
          List<Site> siteList = repository.getAll();
          sitesByDomain = new TreeMap<>(Collections.reverseOrder());
          siteList.forEach(site -> sitesByDomain.put(site.getDomain(), site));
        }
      }
    }

    return sitesByDomain;
  }

}
