package com.kdt.yts.YouSumback.Util;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class WhisperRunner {

    public String runWhisper(String youtubeId) throws Exception {
        String command = "whisper " + youtubeId + ".wav --model medium --output_format txt";
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new RuntimeException("Whisper 실행 실패");
        }

        return youtubeId + ".txt";
    }
}
