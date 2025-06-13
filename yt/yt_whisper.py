import sys
import os
import subprocess
from faster_whisper import WhisperModel

# ✅ 유튜브 ID 추출
def extract_youtube_id(link):
    if "v=" in link:
        return link.split("v=")[1].split("&")[0]
    elif "youtu.be/" in link:
        return link.split("youtu.be/")[1].split("?")[0]
    else:
        return None

# ✅ 자막 여부 확인
def has_korean_subtitles(youtube_url):
    result = subprocess.run([
        "yt-dlp", "--list-subs", "--write-auto-sub", youtube_url
    ], capture_output=True, text=True)
    return "ko" in result.stdout or "a.ko" in result.stdout

# ✅ 자막 다운로드
def download_subtitles(youtube_url, youtube_id, text_dir):
    try:
        subprocess.run([
            "yt-dlp", "--cookies", "yt/cookies.txt",
            "--write-auto-sub",
            "--sub-lang", "ko",
            "--skip-download",
            "--output", os.path.join(text_dir, f"{youtube_id}"),
            youtube_url
        ], check=True)
        output_path = os.path.join(text_dir, f"{youtube_id}.ko.vtt")
        return output_path if os.path.exists(output_path) else None
    except subprocess.CalledProcessError as e:
        print(f"[WHISPER] ❌ 자막 다운로드 실패: {e}")
        return None

# ✅ Whisper 변환
def transcribe_with_whisper(audio_path, youtube_id, text_dir):
    print(f"[WHISPER] [INFO] Whisper로 텍스트 변환 중...")
    if not os.path.exists(audio_path):
        print(f"❌ 오디오 파일이 존재하지 않음: {audio_path}")
        sys.exit(10)

    model = WhisperModel("small", device="cuda" if os.environ.get("USE_CUDA", "0") == "1" else "cpu")
    segments, _ = model.transcribe(audio_path, language="ko")

    output_txt_path = os.path.join(text_dir, f"{youtube_id}.txt")
    with open(output_txt_path, "w", encoding="utf-8") as f:
        for segment in segments:
            f.write(f"[{segment.start:.2f} --> {segment.end:.2f}] {segment.text.strip()}\n")

    return output_txt_path

# ✅ 메인 실행
if len(sys.argv) < 2:
    print("❌ 사용법: python yt_whisper.py <YouTube_URL>")
    sys.exit(1)

youtube_url = sys.argv[1]
youtube_id = extract_youtube_id(youtube_url)
if not youtube_id:
    print("❌ 유효한 YouTube URL이 아닙니다.")
    sys.exit(10)

audio_dir = "yt/audiofiles"
text_dir = "yt/textfiles"
os.makedirs(audio_dir, exist_ok=True)
os.makedirs(text_dir, exist_ok=True)

audio_path = os.path.join(audio_dir, f"{youtube_id}.wav")

# ✅ 이미 자막 파일이 있으면 Whisper 생략
existing_subtitle_path = os.path.join(text_dir, f"{youtube_id}.ko.vtt")
if os.path.exists(existing_subtitle_path):
    print(f"[WHISPER] [INFO] 이미 자막 파일 존재: {existing_subtitle_path}")
    sys.exit(0)

# ✅ 자막 있으면 자막 다운로드 → 없으면 Whisper
if has_korean_subtitles(youtube_url):
    print("[WHISPER] [INFO] 한국어 자막이 있어 자막 다운로드로 진행합니다.")
    subtitle_path = download_subtitles(youtube_url, youtube_id, text_dir)
    if subtitle_path:
        print(f"[WHISPER] [완료] 자막 다운로드 성공: {subtitle_path}")
        sys.exit(0)
    else:
        print("[WHISPER] ❌ 자막 다운로드 실패 → Whisper 실행")

print("[WHISPER] [INFO] Whisper로 오디오 변환 시작")

# ✅ yt-dlp로 오디오 다운로드 (.wav)
try:
    subprocess.run([
        "yt-dlp", "--cookies", "yt/cookies.txt",
        "-x", "--audio-format", "wav",
        "-o", os.path.join(audio_dir, f"{youtube_id}.%(ext)s"),
        youtube_url
    ], check=True)
except subprocess.CalledProcessError as e:
    print(f"[WHISPER] ❌ yt-dlp 다운로드 실패: {e}")
    sys.exit(10)

# ✅ Whisper 변환
transcribe_with_whisper(audio_path, youtube_id, text_dir)
print(f"[WHISPER] [완료] Whisper로 텍스트 저장 완료: {youtube_id}.txt")
