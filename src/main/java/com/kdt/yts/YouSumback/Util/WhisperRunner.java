package com.kdt.yts.YouSumback.Util;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WhisperRunner {

    public static void runWhisper(String youtubeUrl, String youtubeId) throws Exception {
        String basePath = "src/main/resources/audiofiles/";
        String audioPath = basePath + youtubeId + ".wav";
        String txtPath = basePath + youtubeId + ".txt";

        // Python 스크립트 실행 명령어
        List<String> command = List.of(
                "python3",
                "src/main/resources/scripts/run_whisper.py",
                audioPath
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(".")); // 현재 디렉토리 기준
        pb.redirectErrorStream(true); // stderr를 stdout에 병합

        Process process = pb.start();

        // 콘솔 출력 로그
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[WHISPER PY] " + line);
            }
        }

        // 프로세스 종료 대기 (최대 5분)
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("❌ Whisper Python 스크립트 실행 시간 초과로 강제 종료됨");
        }

        // 종료 코드 체크
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("❌ Whisper 실행 실패 (exit code: " + exitCode + ")");
        }

        // 결과 파일 존재 확인
        File resultFile = new File(txtPath);
        if (!resultFile.exists()) {
            throw new RuntimeException("❌ Whisper 실행 후 .txt 파일이 생성되지 않았습니다.");
        }

        System.out.println("✅ Whisper 실행 성공: " + resultFile.getAbsolutePath());
    }
}
