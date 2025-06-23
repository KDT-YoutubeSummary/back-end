# import sys
# import os
# import subprocess
# from faster_whisper import WhisperModel
#
# # ✅ ffmpeg 경로 명시
# os.environ["PATH"] += os.pathsep + r"C:\\ffmpeg\\bin"
#
# # ✅ 유튜브 ID 추출
# def extract_youtube_id(link):
#     if "v=" in link:
#         return link.split("v=")[1].split("&")[0]
#     elif "youtu.be/" in link:
#         return link.split("youtu.be/")[1].split("?")[0]
#     else:
#         return None
#
# YTDLP_PATH = r"C:\\Users\\yes36\\AppData\\Local\\Programs\\Python\\Python311\\Scripts\\yt-dlp.exe"
#
# # ✅ 자막 여부 확인
# def has_korean_subtitles(youtube_url):
#     result = subprocess.run([
#         YTDLP_PATH, "--list-subs", "--write-auto-sub", youtube_url
#     ], capture_output=True, text=True)
#     lines = result.stdout.splitlines()
#     for line in lines:
#         if "ko" in line or "a.ko" in line:
#             return True
#     return False
#
# # ✅ 자막 다운로드
# def download_subtitles(youtube_url, youtube_id, text_dir):
#     output_path = os.path.join(text_dir, f"{youtube_id}.ko.vtt")
#     try:
#         subprocess.run([
#             YTDLP_PATH,
#             "--write-auto-sub",
#             "--sub-lang", "ko",
#             "--skip-download",
#             "--ffmpeg-location", r"C:\\ffmpeg\\bin",
#             "-o", os.path.join(text_dir, f"{youtube_id}"),
#             youtube_url
#         ], check=True)
#         return output_path if os.path.exists(output_path) else None
#     except subprocess.CalledProcessError as e:
#         print(f"[WHISPER] ❌ 자막 다운로드 실패: {e}")
#         return None
#
# # ✅ Whisper로 변환
# def transcribe_with_whisper(audio_path, youtube_id, text_dir):
#     print(f"[WHISPER] [INFO] Whisper로 텍스트 변환 중...")
#
#     if not os.path.exists(audio_path):
#         print(f"❌ 오디오 파일이 존재하지 않아 Whisper 실행 중단: {audio_path}")
#         sys.exit(10)
#
#     model = WhisperModel("small", device="cuda" if os.environ.get("USE_CUDA", "0") == "1" else "cpu")
#     segments, _ = model.transcribe(audio_path, language="ko")
#
#     output_txt_path = os.path.join(text_dir, f"{youtube_id}.txt")
#     try:
#         with open(output_txt_path, "w", encoding="utf-8") as f:
#             for segment in segments:
#                 f.write(f"[{segment.start:.2f} --> {segment.end:.2f}] {segment.text.strip()}\n")
#     except Exception as e:
#         print(f"❌ 처리 중 오류: {output_txt_path}, {e}")
#         sys.exit(11)
#
#     return output_txt_path
#
# # ✅ 메인 실행 흐름
# if len(sys.argv) < 2:
#     print("❌ 사용법: python yt_whisper.py <YouTube_URL>")
#     sys.exit(1)
#
# youtube_url = sys.argv[1]
# youtube_id = extract_youtube_id(youtube_url)
# if not youtube_id:
#     print("❌ 유효한 YouTube URL이 아닙니다.")
#     sys.exit(10)
#
# # ✅ 경로 설정
# audio_dir = "src/main/resources/audiofiles"
# text_dir = "src/main/resources/textfiles"
# os.makedirs(audio_dir, exist_ok=True)
# os.makedirs(text_dir, exist_ok=True)
#
# audio_path = os.path.join(audio_dir, f"{youtube_id}.wav")
#
# # ✅ 자막이 있으면 자막 다운로드 → 없으면 Whisper 실행
# if has_korean_subtitles(youtube_url):
#     print(f"[WHISPER] [INFO] 한국어 자막이 있어 자막 다운로드로 진행합니다.")
#     subtitle_path = download_subtitles(youtube_url, youtube_id, text_dir)
#     if subtitle_path:
#         print(f"[WHISPER] [완료] 자막 파일 저장 완료: {subtitle_path}")
#         sys.exit(0)
#     else:
#         print(f"[WHISPER] ❌ 자막 파일 저장 실패. Whisper로 전환합니다.")
#
# print(f"[WHISPER] [INFO] 자막 없음 → Whisper로 오디오 변환 시작")
# print(f"[WHISPER] [INFO] YouTube 오디오 다운로드 중: {youtube_url}")
# try:
#     subprocess.run([
#         YTDLP_PATH,
#         "--ffmpeg-location", r"C:\\ffmpeg\\bin",
#         "--no-check-certificate",
#         "--force-ipv4",
#         "--user-agent", "Mozilla/5.0",
#         "--referer", "https://www.youtube.com",
#         "-x", "--audio-format", "wav",
#         "-f", "bestaudio[ext=m4a]/bestaudio/best",
#         "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
#         youtube_url
#     ], check=True)
# except subprocess.CalledProcessError as e:
#     print(f"[WHISPER] ❌ yt-dlp 다운로드 실패: {e}")
#     sys.exit(10)
#
# transcribe_with_whisper(audio_path, youtube_id, text_dir)
# print(f"[WHISPER] [완료] Whisper로 텍스트 파일 저장 완료")



# import sys
# import os
# import subprocess
# from faster_whisper import WhisperModel
#
# # ✅ ffmpeg 경로 명시 (Windows에서 환경변수 등록 없이 사용할 수 있게)
# os.environ["PATH"] += os.pathsep + r"C:\ffmpeg\bin"
# # os.environ["PATH"] += os.pathsep + "/usr/local/bin"  # Linux/Mac에서 ffmpeg 경로 설정
#
# # ✅ 유튜브 ID 추출
# def extract_youtube_id(link):
#     if "v=" in link:
#         return link.split("v=")[1].split("&")[0]
#     elif "youtu.be/" in link:
#         return link.split("youtu.be/")[1].split("?")[0]
#     else:
#         return None
#
# YTDLP_PATH = r"C:\Users\yes36\AppData\Local\Programs\Python\Python311\Scripts\yt-dlp.exe"
#
# # ✅ 유튜브 자막 리스트 가져오기
# def has_korean_subtitles(youtube_url):
#     result = subprocess.run(
#         [YTDLP_PATH, "--list-subs", youtube_url],
#         capture_output=True, text=True
#     )
#     return "ko" in result.stdout or "a.ko" in result.stdout
#
# # ✅ 자막 다운로드
# def download_subtitles(youtube_url, youtube_id, text_dir):
#     output_path = os.path.join(text_dir, f"{youtube_id}.ko.vtt")
#     try:
#         subprocess.run([
#             "yt-dlp",
#             "--write-subs", "--sub-lang", "ko",
#             "--skip-download",
#             "--ffmpeg-location", r"C:\ffmpeg\bin",  # ✅ 명시적 경로 설정
#             "-o", os.path.join(text_dir, f"{youtube_id}"),
#             youtube_url
#         ], check=True)
#         return output_path
#     except subprocess.CalledProcessError as e:
#         print(f"[WHISPER] ❌ 자막 다운로드 실패: {e}")
#         return None
#
# # ✅ Whisper로 변환
# def transcribe_with_whisper(audio_path, youtube_id, text_dir):
#     print(f"[WHISPER] [INFO] Whisper로 텍스트 변환 중...")
#     model = WhisperModel("small", device="cuda" if os.environ.get("USE_CUDA", "0") == "1" else "cpu")
#     segments, _ = model.transcribe(audio_path, language="ko")
#
#     output_txt_path = os.path.join(text_dir, f"{youtube_id}.txt")
#     with open(output_txt_path, "w", encoding="utf-8") as f:
#         for segment in segments:
#             f.write(f"[{segment.start:.2f} --> {segment.end:.2f}] {segment.text.strip()}\n")
#     return output_txt_path
#
# # ✅ 메인 실행 흐름
# if len(sys.argv) < 2:
#     print("❌ 사용법: python yt_whisper.py <YouTube_URL>")
#     sys.exit(1)
#
# youtube_url = sys.argv[1]
# youtube_id = extract_youtube_id(youtube_url)
# if not youtube_id:
#     print("❌ 유효한 YouTube URL이 아닙니다.")
#     sys.exit(10)
#
# # ✅ 경로 설정
# audio_dir = "src/main/resources/audiofiles"
# text_dir = "src/main/resources/textfiles"
# os.makedirs(audio_dir, exist_ok=True)
# os.makedirs(text_dir, exist_ok=True)
#
# audio_path = os.path.join(audio_dir, f"{youtube_id}.wav")
#
# # ✅ 자막이 있으면 자막 다운로드 → 없으면 Whisper 실행
# if has_korean_subtitles(youtube_url):
#     print(f"[WHISPER] [INFO] 한국어 자막이 있어 자막 다운로드로 진행합니다.")
#     download_subtitles(youtube_url, youtube_id, text_dir)
#     print(f"[WHISPER] [완료] 자막 파일 저장 완료")
# else:
#     print(f"[WHISPER] [INFO] 자막 없음 → Whisper로 오디오 변환 시작")
#
#     print(f"[WHISPER] [INFO] YouTube 오디오 다운로드 중: {youtube_url}")
#     try:
#         subprocess.run([
#             YTDLP_PATH,
#             "--user-agent", "Mozilla/5.0",
#             "--ffmpeg-location", r"C:\\ffmpeg\\bin",
#             "-x", "--audio-format", "wav",
#             "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
#             youtube_url
#         ], check=True)
#     except subprocess.CalledProcessError as e:
#         print(f"[WHISPER] ❌ yt-dlp 다운로드 실패: {e}")
#         sys.exit(10)
#
#     transcribe_with_whisper(audio_path, youtube_id, text_dir)
#     print(f"[WHISPER] [완료] Whisper로 텍스트 파일 저장 완료")


# # ✅ 자막이 있으면 자막 다운로드 → 없으면 Whisper 실행
# if has_korean_subtitles(youtube_url):
#     print(f"[WHISPER] [INFO] 한국어 자막이 있어 자막 다운로드로 진행합니다.")
#     download_subtitles(youtube_url, youtube_id, text_dir)
#     print(f"[WHISPER] [완료] 자막 파일 저장 완료")
# else:
#     print(f"[WHISPER] [INFO] 자막 없음 → Whisper로 오디오 변환 시작")
#
#     # yt-dlp로 오디오 다운로드
#     print(f"[WHISPER] [INFO] YouTube 오디오 다운로드 중: {youtube_url}")
#     try:
#         subprocess.run([
#             "yt-dlp",
#             "--ffmpeg-location", r"C:\ffmpeg\bin",  # ✅ 여기도 경로 추가
#             "-x", "--audio-format", "wav",
#             "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
#             youtube_url
#         ], check=True)
#     except subprocess.CalledProcessError as e:
#         print(f"[WHISPER] ❌ yt-dlp 다운로드 실패: {e}")
#         sys.exit(10)
#
#     # Whisper STT 실행
#     transcribe_with_whisper(audio_path, youtube_id, text_dir)
#     print(f"[WHISPER] [완료] Whisper로 텍스트 파일 저장 완료")


# import sys
# import os
# import subprocess
# # import whisper
# # from concurrent.futures import ThreadPoolExecutor
#
# os.environ["PATH"] += os.pathsep + r"C:\ffmpeg\bin"
#
# # ✅ 유튜브 ID 추출 함수
# def extract_youtube_id(link):
#     if "v=" in link:
#         return link.split("v=")[1].split("&")[0]
#     elif "youtu.be/" in link:
#         return link.split("youtu.be/")[1].split("?")[0]
#     else:
#         return None
#
# # ✅ 1. 입력 URL 확인
# if len(sys.argv) < 2:
#     print("❌ 사용법: python yt_whisper.py <YouTube_URL>")
#     sys.exit(1)
#
# youtube_url = sys.argv[1]
# youtube_id = extract_youtube_id(youtube_url)
# if not youtube_id:
#     print("❌ 유효한 YouTube URL이 아닙니다.")
#     sys.exit(10)
#
# # ✅ 2. 경로 설정
# audio_dir = "src/main/resources/audiofiles"
# text_dir = "src/main/resources/textfiles"
# os.makedirs(audio_dir, exist_ok=True)
# os.makedirs(text_dir, exist_ok=True)
#
# output_wav_path = os.path.join(audio_dir, f"{youtube_id}.wav")
#
# # ✅ 3. yt-dlp로 .wav 다운로드
# print(f"[INFO] YouTube 오디오 다운로드 중: {youtube_url}")
# # download_cmd = [
# #     "yt-dlp",
# #     "-x", "--audio-format", "wav",
# #     "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
# #     youtube_url
# # ]
# download_cmd = [
#     "yt-dlp",
#     "--ffmpeg-location", "C:\\ffmpeg\\bin",  # ✅ 여기에 직접 경로 설정
#     "-x", "--audio-format", "wav",
#     "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
#     youtube_url
# ]
#
# try:
#     subprocess.run(download_cmd, check=True)
# except subprocess.CalledProcessError as e:
#     print(f"❌ yt-dlp 다운로드 실패: {e}")
#     sys.exit(10)
#
# # ✅ 3.5 영상 길이 추출 (ffprobe 사용)
# print(f"[INFO] 영상 길이 추출 중 (ffprobe 사용): {output_wav_path}")
# duration_cmd = [
#     "ffprobe",
#     "-v", "error",
#     "-show_entries", "format=duration",
#     "-of", "default=noprint_wrappers=1:nokey=1",
#     output_wav_path
# ]
# duration_seconds = 0  # 기본값 선언
#
# try:
#     result = subprocess.run(duration_cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
#     duration_seconds = int(float(result.stdout.strip()))
#     print(f"[INFO] 추출된 영상 길이: {duration_seconds}초")
# except subprocess.CalledProcessError as e:
#     print(f"[WARNING] 영상 길이 추출 실패: {e}")
# except ValueError:
#     print(f"[WARNING] 영상 길이 변환 실패: {result.stdout.strip()}")
#
# # ✅ 여기서 print
# print(f"[DURATION_RESULT]{duration_seconds}")
#
#
# # ✅ 4. Whisper로 텍스트 변환
# print(f"[INFO] Whisper로 텍스트 변환 시작: {output_wav_path}")
# whisper_cmd = [
#     "whisper",
#     output_wav_path,
#     "--model", "small",
#     "--language", "ko",
#     "--output_format", "txt",
#     "--output_dir", text_dir              # ✅ 텍스트 파일 저장 경로 변경
# ]
#
# try:
#     subprocess.run(whisper_cmd, check=True)
# except subprocess.CalledProcessError as e:
#     print(f"❌ Whisper 실행 실패: {e}")
#     sys.exit(10)
#
# # ✅ 5. 결과 안내
# print(f"[완료] 변환된 텍스트 파일: {os.path.join(text_dir, youtube_id + '.txt')}")
import sys
import os
import subprocess
from faster_whisper import WhisperModel

# ✅ ffmpeg 경로 (리눅스용)
os.environ["PATH"] += os.pathsep + "/usr/bin"

# ✅ 유튜브 ID 추출
def extract_youtube_id(link):
    if "v=" in link:
        return link.split("v=")[1].split("&")[0]
    elif "youtu.be/" in link:
        return link.split("youtu.be/")[1].split("?")[0]
    else:
        return None

# ✅ yt-dlp 경로 (리눅스에선 그냥 명령어)
YTDLP_PATH = "yt-dlp"

# ✅ 자막 여부 확인
def has_korean_subtitles(youtube_url):
    result = subprocess.run([
        YTDLP_PATH, "--list-subs", "--write-auto-sub", youtube_url
    ], capture_output=True, text=True)
    lines = result.stdout.splitlines()
    for line in lines:
        if "ko" in line or "a.ko" in line:
            return True
    return False

# ✅ 자막 다운로드
def download_subtitles(youtube_url, youtube_id, text_dir):
    output_path = os.path.join(text_dir, f"{youtube_id}.ko.vtt")
    try:
        subprocess.run([
            YTDLP_PATH,
            "--write-auto-sub",
            "--sub-lang", "ko",
            "--skip-download",
            "-o", os.path.join(text_dir, f"{youtube_id}"),
            youtube_url
        ], check=True)
        return output_path if os.path.exists(output_path) else None
    except subprocess.CalledProcessError as e:
        print(f"[WHISPER] ❌ 자림 다운로드 실패: {e}")
        return None

# ✅ Whisper로 변환
def transcribe_with_whisper(audio_path, youtube_id, text_dir):
    print(f"[WHISPER] [INFO] Whisper로 텍스트 변환 중...")

    if not os.path.exists(audio_path):
        print(f"❌ 오디오 파일이 존재하지 않아 Whisper 실행 중단: {audio_path}")
        sys.exit(10)

    model = WhisperModel("small", device="cuda" if os.environ.get("USE_CUDA", "0") == "1" else "cpu")
    segments, _ = model.transcribe(audio_path, language="ko")

    output_txt_path = os.path.join(text_dir, f"{youtube_id}.txt")
    try:
        with open(output_txt_path, "w", encoding="utf-8") as f:
            for segment in segments:
                f.write(f"[{segment.start:.2f} --> {segment.end:.2f}] {segment.text.strip()}\n")
    except Exception as e:
        print(f"❌ 처리 중 오류: {output_txt_path}, {e}")
        sys.exit(11)

    return output_txt_path

# ✅ 메인 실행 흐름
if len(sys.argv) < 2:
    print("❌ 사용법: python yt_whisper.py <YouTube_URL>")
    sys.exit(1)

youtube_url = sys.argv[1]
youtube_id = extract_youtube_id(youtube_url)
if not youtube_id:
    print("❌ 유효한 YouTube URL이 아닙니다.")
    sys.exit(10)

# ✅ 경로 설정
audio_dir = "src/main/resources/audiofiles"
text_dir = "src/main/resources/textfiles"
os.makedirs(audio_dir, exist_ok=True)
os.makedirs(text_dir, exist_ok=True)

audio_path = os.path.join(audio_dir, f"{youtube_id}.wav")

# ✅ 자막이 있으면 자막 다운로드 → 없으면 Whisper 실행
if has_korean_subtitles(youtube_url):
    print(f"[WHISPER] [INFO] 한국어 자막이 있어 자막 다운로드로 진행합니다.")
    subtitle_path = download_subtitles(youtube_url, youtube_id, text_dir)
    if subtitle_path:
        print(f"[WHISPER] [완료] 자막 파일 저장 완료: {subtitle_path}")
        sys.exit(0)
    else:
        print(f"[WHISPER] ❌ 자막 파일 저장 실패. Whisper로 전환합니다.")

print(f"[WHISPER] [INFO] 자막 없음 → Whisper로 오디오 변환 시작")
print(f"[WHISPER] [INFO] YouTube 오디오 다운로드 중: {youtube_url}")
try:
    subprocess.run([
        YTDLP_PATH,
        "--no-check-certificate",
        "--force-ipv4",
        "--user-agent", "Mozilla/5.0",
        "--referer", "https://www.youtube.com",
        "-x", "--audio-format", "wav",
        "-f", "bestaudio[ext=m4a]/bestaudio/best",
        "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
        youtube_url
    ], check=True)
except subprocess.CalledProcessError as e:
    print(f"[WHISPER] ❌ yt-dlp 다운로드 실패: {e}")
    sys.exit(10)

transcribe_with_whisper(audio_path, youtube_id, text_dir)
print(f"[WHISPER] [완료] Whisper로 텍스트 파일 저장 완료")

