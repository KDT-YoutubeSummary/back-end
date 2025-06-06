import sys
import os
import subprocess
os.environ["PATH"] += os.pathsep + r"C:\ffmpeg\bin"

# ✅ 유튜브 ID 추출 함수
def extract_youtube_id(link):
    if "v=" in link:
        return link.split("v=")[1].split("&")[0]
    elif "youtu.be/" in link:
        return link.split("youtu.be/")[1].split("?")[0]
    else:
        return None

# ✅ 1. 입력 URL 확인
if len(sys.argv) < 2:
    print("❌ 사용법: python yt_whisper.py <YouTube_URL>")
    sys.exit(1)

youtube_url = sys.argv[1]
youtube_id = extract_youtube_id(youtube_url)
if not youtube_id:
    print("❌ 유효한 YouTube URL이 아닙니다.")
    sys.exit(10)

# ✅ 2. 경로 설정
audio_dir = "src/main/resources/audiofiles"
text_dir = "src/main/resources/textfiles"
os.makedirs(audio_dir, exist_ok=True)
os.makedirs(text_dir, exist_ok=True)

output_wav_path = os.path.join(audio_dir, f"{youtube_id}.wav")

# ✅ 3. yt-dlp로 .wav 다운로드
print(f"[INFO] YouTube 오디오 다운로드 중: {youtube_url}")
# download_cmd = [
#     "yt-dlp",
#     "-x", "--audio-format", "wav",
#     "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
#     youtube_url
# ]
download_cmd = [
    "yt-dlp",
    "--ffmpeg-location", "C:\\ffmpeg\\bin",  # ✅ 여기에 직접 경로 설정
    "-x", "--audio-format", "wav",
    "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
    youtube_url
]

try:
    subprocess.run(download_cmd, check=True)
except subprocess.CalledProcessError as e:
    print(f"❌ yt-dlp 다운로드 실패: {e}")
    sys.exit(10)

# ✅ 3.5 영상 길이 추출 (ffprobe 사용)
print(f"[INFO] 영상 길이 추출 중 (ffprobe 사용): {output_wav_path}")
duration_cmd = [
    "ffprobe",
    "-v", "error",
    "-show_entries", "format=duration",
    "-of", "default=noprint_wrappers=1:nokey=1",
    output_wav_path
]

try:
    result = subprocess.run(duration_cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    duration_seconds = int(float(result.stdout.strip()))
    print(f"[INFO] 추출된 영상 길이: {duration_seconds}초")

    with open(os.path.join(text_dir, f"{youtube_id}_duration.txt"), "w", encoding="utf-8") as f:
        f.write(str(duration_seconds))

except subprocess.CalledProcessError as e:
    print(f"[WARNING] 영상 길이 추출 실패: {e}")
    duration_seconds = 0
except ValueError:
    print(f"[WARNING] 영상 길이 변환 실패: {result.stdout.strip()}")
    duration_seconds = 0


# ✅ 4. Whisper로 텍스트 변환
print(f"[INFO] Whisper로 텍스트 변환 시작: {output_wav_path}")
whisper_cmd = [
    "whisper",
    output_wav_path,
    "--model", "small",
    "--language", "ko",
    "--output_format", "txt",
    "--output_dir", text_dir              # ✅ 텍스트 파일 저장 경로 변경
]

try:
    subprocess.run(whisper_cmd, check=True)
except subprocess.CalledProcessError as e:
    print(f"❌ Whisper 실행 실패: {e}")
    sys.exit(10)

# ✅ 5. 결과 안내
print(f"[완료] 변환된 텍스트 파일: {os.path.join(text_dir, youtube_id + '.txt')}")
