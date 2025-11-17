// Cloud Functions v2 Storage Trigger
const { onObjectFinalized } = require("firebase-functions/v2/storage");
const { logger } = require("firebase-functions");

// Firebase Admin
const admin = require("firebase-admin");
admin.initializeApp();

// Google Speech-to-Text
const speech = require('@google-cloud/speech');
const client = new speech.SpeechClient();

exports.transcribeAudio = onObjectFinalized(
  { region: "us-east1" },
  async (event) => {
  const object = event.data;

  const bucketName = object.bucket;
  const filePath = object.name;
  const contentType = object.contentType;

  logger.log("📥 업로드된 파일:", filePath, contentType);

  // 오디오 파일만 처리
  if (!contentType || !contentType.startsWith("audio/")) {
    logger.log("❌ 오디오 파일이 아니므로 무시함");
    return null;
  }

  // 파일명에서 chatId, senderUid 추출 (예: chat123_uidABC_1732003312.3gp)
  const fileName = filePath.split("/").pop();
  const [chatId, senderUid] = fileName.replace(".3gp", "").split("_");

  if (!chatId || !senderUid) {
    logger.error("❌ 파일명에서 chatId 또는 senderUid 파싱 실패:", fileName);
    return null;
  }

  logger.log("chatId:", chatId, "senderUid:", senderUid);

  // Google Cloud Storage URI
  const gcsUri = `gs://${bucketName}/${filePath}`;

  // STT 설정
  const audio = { uri: gcsUri };
  const config = {
    encoding: "AMR",   // 3gp 기본 코덱 → AMR
    sampleRateHertz: 8000,
    languageCode: "ko-KR",
    enableAutomaticPunctuation: true,
  };

  try {
    logger.log("🎧 STT 요청 시작:", gcsUri);

    const [response] = await client.recognize({ audio, config });

    const transcription = response.results
      .map(r => r.alternatives[0].transcript)
      .join(" ");

    logger.log("📄 변환된 텍스트:", transcription);

    // Firestore 저장
    const db = admin.firestore();

    await db.collection("chats")
      .doc(chatId)
      .collection("messages")
      .add({
        text: transcription,
        senderId: senderUid,
        type: "stt",
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

    logger.log("🎉 Firestore 저장 완료");
  } catch (error) {
    logger.error("❌ STT 변환 실패:", error);
  }

  return null;
});
