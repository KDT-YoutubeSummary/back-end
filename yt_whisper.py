import sys
import yt_dlp
import whisper
import json

url = sys.argv[1]

ydl_opts = {
    'format': 'bestaudio/best',
    'outtmpl': 'downloads/%(id)s.%(ext)s',
    'quiet': True
}

with yt_dlp.YoutubeDL(ydl_opts) as ydl:
    info = ydl.extract_info(url, download=True)
    video_id = info['id']
    title = info.get('title')
    duration = info.get('duration')
    uploader = info.get('uploader')
    file_path = f"downloads/{video_id}.webm"

model = whisper.load_model("base")
result = model.transcribe(file_path)

print(json.dumps({
    "videoId": video_id,
    "title": title,
    "originalUrl": url,
    "duration": duration,
    "uploaderName": uploader,
    "transcript": result["text"]
}))
