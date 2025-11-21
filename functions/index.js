const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

// ---------------------------------------------------------------
// 1) chats/{chatId}/messages/{messageId} 생성 → 개별 메시지 push
// ---------------------------------------------------------------
exports.sendPushOnNewMessage = onDocumentCreated(
  { region: "us-central1" },
  async (event) => {
    const message = event.data.data();
    const chatId = event.params.chatId;

    console.log(`📩 새 메시지 감지됨: ${chatId}`, message);

    const senderName = message.senderName || "MateMatch";
    const text = message.text || "새 메시지가 도착했습니다.";

    const chatDoc = await admin.firestore()
      .collection("chats")
      .doc(chatId)
      .get();

    const participants = chatDoc.data()?.participants || [];
    const receiverUid = participants.find(uid => uid !== message.senderId);
    if (!receiverUid) return;

    const userDoc = await admin.firestore()
      .collection("users")
      .doc(receiverUid)
      .get();

    const token = userDoc.data()?.fcmToken;
    if (!token) return;

    const payload = {
      notification: {
        title: senderName,
        body: text,
      },
      token: token,
    };

    await admin.messaging().send(payload);
    console.log(`📨 FCM 전송 완료 → ${receiverUid}`);
  }
);

// ---------------------------------------------------------------
// 2) chats/{chatRoomId} 변경 시 → room-level broadcast 알림
// ---------------------------------------------------------------
exports.sendChatNotification = onDocumentUpdated(
  { region: "us-central1" },
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();

    if (before.lastMessage === after.lastMessage) return;

    const chatRoomId = event.params.chatRoomId;
    const lastMessage = after.lastMessage;
    const participants = after.participants;

    const userDocs = await admin.firestore()
      .collection("users")
      .where(admin.firestore.FieldPath.documentId(), "in", participants)
      .get();

    const tokens = [];
    userDocs.forEach(doc => {
      const token = doc.data().fcmToken;
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) return;

    await admin.messaging().sendMulticast({
      notification: {
        title: "새 메시지가 도착했습니다",
        body: lastMessage,
      },
      data: { chatRoomId }
    });

    console.log("📨 Chat broadcast 알림 전송 완료");
  }
);
