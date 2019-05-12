package io.inprice.scrapper.manager.helpers;

import io.inprice.scrapper.common.logging.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class UserAgents {

    private static final Logger log = new Logger(UserAgents.class);

    private static final List<String> uaList = new ArrayList<>();
    private static final List<String> refererList = new ArrayList<>();

    static {
        log.debug("User-agents are imported...");
        try {
            List<File> filesInFolder = Files.walk(Paths.get("./user-agents"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            filesInFolder.forEach(file -> {
                log.debug("   %s is imported...", file.getName());

                try {
                    List<String> lines = Files.readAllLines(Paths.get("./user-agents/" + file.getName()));
                    uaList.addAll(lines);
                } catch (Exception e) {
                    log.warn("   %s has problem...", file.getName());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            log.warn("Error in importing user-agents", e);
        }

        log.debug("Referer are imported...");
        refererList.add("https://www.google.com");
        refererList.add("https://www.twitter.com");
        refererList.add("https://www.facebook.com");
        refererList.add("https://www.instagram.com");
        refererList.add("https://www.yandex.com");
    }

    public static String findARandomUA() {
        String ua = uaList.get(random(uaList.size()));
        log.debug("ua: %s", ua);
        return ua;
    }

    public static String findARandomReferer() {
        String referer = refererList.get(random(refererList.size()));
        log.debug("referer: %s", referer);
        return referer;
    }

    private static int random(int upperBound) {
        return ThreadLocalRandom.current().nextInt(0, upperBound);
    }
}
