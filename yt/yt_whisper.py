import sys
import os
import boto3
import yt_dlp
from faster_whisper import WhisperModel

# --- AWS 및 S3 설정 ---
S3_BUCKET_NAME = "yousum-s3"
AWS_REGION = "ap-northeast-2"
s3_client = boto3.client('s3', region_name=AWS_REGION)
TEMP_DIR = "/tmp"

# --- 유튜브 관련 함수 ---
def extract_youtube_id(link):
    if "v=" in link: return link.split("v=")[1].split("&")[0]
    elif "youtu.be/" in link: return link.split("youtu.be/")[1].split("?")[0]
    return None

# [수정] 영상 길이를 먼저 추출하는 함수 추가
def get_video_duration(youtube_url):
    try:
        ydl_opts = {'quiet': True, 'extract_flat': True}
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(youtube_url, download=False)
            duration = info.get('duration', 0)
            return int(duration)
    except Exception as e:
        print(f"[WARN] 영상 길이 추출 실패: {e}")
        return -1

def has_korean_subtitles(youtube_url):
    print("[INFO] 한국어 자막 존재 여부를 확인합니다...")
    try:
        ydl_opts = {'listsubtitles': True, 'quiet': True}
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(youtube_url, download=False)
            subtitles = info.get('subtitles', {})
            auto_captions = info.get('automatic_captions', {})
            if 'ko' in subtitles or 'ko' in auto_captions:
                print("[INFO] ✅ 한국어 자막을 발견했습니다.")
                return True
    except Exception as e:
        print(f"[WARN] 자막 확인 중 오류: {e}")
    print("[INFO] ❌ 한국어 자막이 없습니다.")
    return False

def download_subtitle_and_upload_to_s3(youtube_url, youtube_id):
    local_subtitle_path = os.path.join(TEMP_DIR, f"{youtube_id}.ko.vtt")
    try:
        print("[PROCESS] 자막 다운로드를 시작합니다...")
        ydl_opts = {'writeautomaticsub': True, 'subtitleslangs': ['ko'], 'skip_download': True, 'outtmpl': os.path.join(TEMP_DIR, youtube_id)}
        with yt_dlp.YoutubeDL(ydl_opts) as ydl: ydl.download([youtube_url])
        if not os.path.exists(local_subtitle_path): raise FileNotFoundError("자막 파일이 생성되지 않았습니다.")
        s3_key = f"subtitles/{youtube_id}.vtt"
        s3_client.upload_file(local_subtitle_path, S3_BUCKET_NAME, s3_key)
        print(f"[SUCCESS] 자막을 S3에 업로드했습니다: s3://{S3_BUCKET_NAME}/{s3_key}")
        return s3_key
    except Exception as e:
        print(f"[ERROR] 자막 처리 중 오류 발생: {e}")
        return None
    finally:
        if os.path.exists(local_subtitle_path): os.remove(local_subtitle_path)

def process_audio_with_whisper(youtube_url, youtube_id):
    local_audio_path, local_text_path = "", ""
    try:
        print("[PROCESS] 음성 파일 다운로드를 시작합니다...")
        audio_filename_template = os.path.join(TEMP_DIR, f"{youtube_id}.%(ext)s")
        ydl_opts = {'format': 'bestaudio/best', 'postprocessors': [{'key': 'FFmpegExtractAudio', 'preferredcodec': 'mp3'}], 'outtmpl': audio_filename_template}
        with yt_dlp.YoutubeDL(ydl_opts) as ydl: ydl.download([youtube_url])
        local_audio_path = os.path.join(TEMP_DIR, f"{youtube_id}.mp3")
        audio_s3_key = f"audios/{youtube_id}.mp3"
        s3_client.upload_file(local_audio_path, S3_BUCKET_NAME, audio_s3_key)
        print(f"[INFO] 음성 파일을 S3에 업로드했습니다: s3://{S3_BUCKET_NAME}/{audio_s3_key}")
        print("[PROCESS] Whisper 변환을 시작합니다...")
        model = WhisperModel("small", device="cuda" if os.environ.get("USE_CUDA", "0") == "1" else "cpu")
        segments, _ = model.transcribe(local_audio_path, language="ko")
        local_text_path = os.path.join(TEMP_DIR, f"{youtube_id}.txt")
        with open(local_text_path, "w", encoding="utf-8") as f:
            for segment in segments: f.write(f"[{segment.start:.2f} --> {segment.end:.2f}] {segment.text.strip()}\n")
        text_s3_key = f"transcripts/{youtube_id}.txt"
        s3_client.upload_file(local_text_path, S3_BUCKET_NAME, text_s3_key)
        print(f"[SUCCESS] 변환된 텍스트를 S3에 업로드했습니다: s3://{S3_BUCKET_NAME}/{text_s3_key}")
        return text_s3_key
    except Exception as e:
        print(f"[ERROR] 음성 처리 중 오류 발생: {e}")
        return None
    finally:
        if os.path.exists(local_audio_path): os.remove(local_audio_path)
        if os.path.exists(local_text_path): os.remove(local_text_path)

# --- 메인 실행 흐름 ---
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("❌ 사용법: python3 yt_whisper.py <YouTube_URL>")
        sys.exit(1)
    youtube_url = sys.argv[1]
    youtube_id = extract_youtube_id(youtube_url)
    if not youtube_id:
        print("❌ 유효한 YouTube URL이 아닙니다."); sys.exit(10)

    # [수정] 영상 길이를 먼저 가져와서 출력
    duration = get_video_duration(youtube_url)
    print(f"[DURATION_RESULT]{duration}")

    print("="*50); print(f"작업 시작: (ID: {youtube_id})"); print("="*50)

    if has_korean_subtitles(youtube_url):
        final_s3_path = download_subtitle_and_upload_to_s3(youtube_url, youtube_id)
    else:
        final_s3_path = process_audio_with_whisper(youtube_url, youtube_id)

    print("\n" + "="*50)
    if final_s3_path:
        print(f"✅ 작업 완료. 최종 결과물은 S3에 저장되었습니다.")
        # [수정] 최종 S3 경로를 표준 출력으로 명확하게 전달
        print(f"[S3_PATH_RESULT]{final_s3_path}")
    else:
        print("❌ 작업 실패. 로그를 확인해주세요.")
    print("="*50)