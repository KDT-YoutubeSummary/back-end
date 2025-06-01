package com.YouSumback.util;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class WhisperRunner {

    public String runWhisper(String audioPath) {
        try {
            // 정확한 Python 실행 경로 지정
            String pythonPath = "/mnt/c/Users/정수/whisper-env/bin/python3";
            String scriptPath = "/mnt/c/Users/정수/IdeaProjects/back-end/yt/yt_whisper.py";

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, audioPath);
            pb.redirectErrorStream(true); // stderr를 stdout으로 병합

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("🔍 whisper 출력: " + line);  // 실시간 로그 출력
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Whisper 실행 실패 (exit code " + exitCode + ")");
            }

            return output.toString();

        } catch (Exception e) {
            throw new RuntimeException("Whisper 실행 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
