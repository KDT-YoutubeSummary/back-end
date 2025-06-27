// MetadataHelper.java
package com.kdt.yts.YouSumback.Util;

import org.springframework.stereotype.Component;

@Component
public class MetadataHelper {
    public String extractYoutubeId(String url) {
        if (url.contains("v=")) {
            return url.substring(url.indexOf("v=") + 2).split("&")[0];
        } else if (url.contains("youtube/")) {
            return url.substring(url.indexOf("youtube/") + 9).split("\\?")[0];
        } else if (url.contains("youtu.be/")) {
            return url.split("youtu.be/")[1].split("\\?")[0];
        }
        throw new IllegalArgumentException("유효하지 않은 유튜브 링크입니다.");
    }
}
