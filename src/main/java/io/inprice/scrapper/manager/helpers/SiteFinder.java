package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.models.Site;
import io.inprice.scrapper.manager.repository.Sites;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SiteFinder {

    private static final Object lock = new Object();
    private static Map<String, Site> sitesByDomain;

    public static Site findSiteByUrl(String url) {
        final String domain = extractDomain(url);
        Site found = null;

        if (domain != null) {
            found = getDomainSiteMap().get(domain);
            if (found == null) { //if not found
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

    public static boolean isValidURL(String url) {
        if (url == null
        || url.trim().isEmpty()
        || url.trim().length() < 20
        || url.trim().length() > 2000) return false;
        return url.matches("^(http|https)://.*$");
    }

    private static String extractDomain(String url) {
        final String newForm = url.replaceAll("^(https?)://|www.", "");
        final String[] chunks = newForm.split("/");

        if (chunks.length > 0) return chunks[0];

        return null;
    }

    private static Map<String, Site> getDomainSiteMap() {
        if (sitesByDomain == null) {
            synchronized (lock) {
                if (sitesByDomain == null) {
                    List<Site> siteList = Sites.getAll();
                    sitesByDomain = new TreeMap<>(Collections.reverseOrder());
                    siteList.forEach(site -> {
                        sitesByDomain.put(site.getDomain(), site);
                    });
                }
            }
        }

        return sitesByDomain;
    }

}
