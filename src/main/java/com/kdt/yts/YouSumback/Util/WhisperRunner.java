package com.kdt.yts.YouSumback.Util;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WhisperRunner {

    public static void runWhisper(String youtubeUrl, String youtubeId) throws Exception {
        String audioDir = "src/main/resources/audiofiles/";
        String textDir = "src/main/resources/textfiles/";
        String audioPath = audioDir + youtubeId + ".wav";
        String txtPath = textDir + youtubeId + ".txt";

        // Python 스크립트 실행 명령어
        List<String> command = List.of(
                "python3",
                "src/main/resources/scripts/run_whisper.py",
                audioPath,
                youtubeId,
                textDir
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(".")); // 현재 프로젝트 루트 기준
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[WHISPER PY] " + line);
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("❌ Whisper Python 스크립트 실행 시간 초과로 강제 종료됨");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("❌ Whisper 실행 실패 (exit code: " + exitCode + ")");
        }

        File resultFile = new File(txtPath);
        if (!resultFile.exists()) {
            throw new RuntimeException("❌ Whisper 실행 후 .txt 파일이 생성되지 않았습니다.");
        }

        System.out.println("✅ Whisper 실행 성공: " + resultFile.getAbsolutePath());
    }
}
