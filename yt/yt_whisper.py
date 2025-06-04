import subprocess
import tempfile
import whisper
import os
import sys
import uuid

def download_audio(youtube_url):
    print(f"\n🎧 오디오 다운로드 중... ({youtube_url})")

    # 유니크한 파일 이름 생성
    unique_id = str(uuid.uuid4())
    output_base = f"/tmp/{unique_id}"
    output_path = output_base + ".webm"

    # yt-dlp 명령어
    command = [
        "yt-dlp",
        "--no-cache-dir",
        "--no-part",
        "-f", "bestaudio[ext=webm]",
        "-o", output_path,
        youtube_url
    ]

    subprocess.run(command, check=True)

    # 다운로드 완료 확인
    if not os.path.exists(output_path) or os.path.getsize(output_path) == 0:
        raise RuntimeError("❌ 다운로드된 파일이 없거나 손상됨")

    return output_path

def transcribe_audio(audio_path):
    print("🧠 Whisper로 음성 인식 중...")

    model = whisper.load_model("base")  # 필요시 tiny/small/medium/large 로 교체
    result = model.transcribe(audio_path, fp16=False)

    return result["text"]

def main():
    if len(sys.argv) < 2:
        print("❗ YouTube URL을 인자로 넘겨줘야 해.")
        sys.exit(1)

    youtube_url = sys.argv[1]

    try:
        audio_path = download_audio(youtube_url)
        text = transcribe_audio(audio_path)

        print("\n📄 변환된 텍스트:\n")
        print(text)

    except subprocess.CalledProcessError as e:
        print(f"\n❌ yt-dlp 실행 실패: {e}")
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")

if __name__ == "__main__":
    main()
