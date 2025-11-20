const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendPushOnNewMessage = functions
  .region("us-central1") // ✅ 반드시 이렇게
  .firestore.document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snap, context) => { ... });

exports.sendChatNotification = functions.firestore
  .document("chats/{chatRoomId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    if (before.lastMessage === after.lastMessage) return null;

    const chatRoomId = context.params.chatRoomId;
    const lastMessage = after.lastMessage;
    const participants = after.participants;

    console.log(`💬 ChatRoom ${chatRoomId} 새 메시지: ${lastMessage}`);

    const { onDocumentCreated } = require("firebase-functions/v2/firestore");
    const admin = require("firebase-admin");
    admin.initializeApp();

    // ✅ Firestore 트리거: chats/{chatId}/messages/{messageId} 생성 시 실행
    exports.sendPushOnNewMessage = onDocumentCreated(
      { region: "us-central1" }, // FCM은 반드시 us-central1
      async (event) => {
        const message = event.data.data();
        const chatId = event.params.chatId;

        console.log(`📩 새 메시지 감지됨: ${chatId}`, message);

        // 1️⃣ 송신자 이름 및 메시지 내용
        const senderName = message.senderName || "MateMatch";
        const text = message.text || "새 메시지가 도착했습니다.";

        // 2️⃣ 수신자 ID 가져오기
        const chatDoc = await admin.firestore().collection("chats").doc(chatId).get();
        const participants = chatDoc.data()?.participants || [];

        // 3️⃣ 수신자 UID 식별
        const receiverUid = participants.find(uid => uid !== message.senderId);
        if (!receiverUid) {
          console.log("❌ 수신자 UID 없음, 알림 생략");
          return;
        }

        // 4️⃣ 수신자 fcmToken 조회
        const userDoc = await admin.firestore().collection("users").doc(receiverUid).get();
        const token = userDoc.data()?.fcmToken;

        if (!token) {
          console.log("❌ FCM 토큰 없음, 알림 전송 불가");
          return;
        }

        // 5️⃣ 알림 메시지 생성
        const payload = {
          notification: {
            title: senderName,
            body: text,
          },
          token: token,
        };

        try {
          await admin.messaging().send(payload);
          console.log(`📨 FCM 전송 완료 → ${receiverUid}`);
        } catch (error) {
          console.error("🚨 FCM 전송 실패:", error);
        }
      }
    );


    const userDocs = await admin.firestore()
      .collection("users")
      .where(admin.firestore.FieldPath.documentId(), "in", participants)
      .get();

    const tokens = [];
    userDocs.forEach(doc => {
      const token = doc.data().fcmToken;
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) {
      console.log("⚠️ 전송할 FCM 토큰이 없습니다.");
      return null;
    }

    const payload = {
      notification: {
        title: "새 메시지가 도착했습니다",
        body: lastMessage,
        click_action: "OPEN_CHAT_ACTIVITY"
      },
      data: { chatRoomId }
    };

    const response = await admin.messaging().sendToDevice(tokens, payload);
    console.log("✅ FCM 전송 완료:", response.successCount);

    return null;
  });
