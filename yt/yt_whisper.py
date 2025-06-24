# import os
# import yt_dlp
# from faster_whisper import WhisperModel
# from flask import Flask, request, jsonify
#
# app = Flask(__name__)
#
# # --- 유튜브 ID 추출 ---
# def extract_youtube_id(link):
#     if "v=" in link:
#         return link.split("v=")[1].split("&")[0]
#     elif "youtu.be/" in link:
#         return link.split("youtu.be/")[1].split("?")[0]
#     return None
#
# # --- 유튜브 음성 다운로드 및 Whisper 처리 ---
# def process_audio_and_transcribe(youtube_url, youtube_id):
#     temp_dir = "/tmp"
#     local_audio_path = ""
#
#     try:
#         print("[PROCESS] 음성 다운로드 시작")
#         audio_template = os.path.join(temp_dir, f"{youtube_id}.%(ext)s")
#         ydl_opts = {
#             'format': 'bestaudio/best',
#             'postprocessors': [{
#                 'key': 'FFmpegExtractAudio',
#                 'preferredcodec': 'mp3'
#             }],
#             'outtmpl': audio_template
#         }
#         with yt_dlp.YoutubeDL(ydl_opts) as ydl:
#             ydl.download([youtube_url])
#
#         local_audio_path = os.path.join(temp_dir, f"{youtube_id}.mp3")
#
#         print("[PROCESS] Whisper 변환 시작")
#         model = WhisperModel("small", device="cuda" if os.environ.get("USE_CUDA", "0") == "1" else "cpu")
#         segments, _ = model.transcribe(local_audio_path, language="ko")
#
#         transcript_lines = []
#         for segment in segments:
#             transcript_lines.append(f"[{segment.start:.2f} --> {segment.end:.2f}] {segment.text.strip()}")
#
#         return "\n".join(transcript_lines)
#
#     except Exception as e:
#         print(f"[ERROR] 변환 중 오류: {e}")
#         return None
#
#     finally:
#         if os.path.exists(local_audio_path):
#             os.remove(local_audio_path)
#
# # --- Flask API 엔드포인트 ---
# @app.route('/transcribe', methods=['POST'])
# def transcribe_endpoint():
#     data = request.get_json()
#     youtube_url = data.get("videoUrl")
#     youtube_id = data.get("youtubeId")
#
#     if not youtube_url or not youtube_id:
#         return jsonify({"error": "youtubeId, videoUrl 둘 다 필요합니다"}), 400
#
#     print("="*50)
#     print(f"[START] Whisper 처리 시작 - ID: {youtube_id}")
#     print("="*50)
#
#     transcript_text = process_audio_and_transcribe(youtube_url, youtube_id)
#
#     if transcript_text:
#         return jsonify({"transcript": transcript_text}), 200
#     else:
#         return jsonify({"error": "Whisper 변환 실패"}), 500
#
# # --- Flask 서버 시작 ---
# if __name__ == '__main__':
#     app.run(host='0.0.0.0', port=8000)
