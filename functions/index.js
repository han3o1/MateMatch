exports.sendPushOnNewMessage = onDocumentCreated(
  {
    region: "us-central1",
    document: "chats/{chatId}/messages/{messageId}"
  },
  async (event) => {

    if (!event.data || !event.data.value) {
      console.log("[Push] ❌ event.data.value undefined");
      return;
    }

    const doc = event.data.value;
    const fields = doc.fields;

    if (!fields) {
      console.log("[Push] ❌ message.fields undefined");
      return;
    }

    const senderId = fields.senderId?.stringValue;
    const text = fields.text?.stringValue;

    console.log(`[Push] 📩 message from ${senderId}: ${text}`);

    const chatId = event.params.chatId;

    const chatDoc = await admin.firestore()
      .collection("chats")
      .doc(chatId)
      .get();

    const participants = chatDoc.data()?.participants || [];
    const receiverUid = participants.find(uid => uid !== senderId);

    if (!receiverUid) {
      console.log("[Push] ❌ no receiverUid");
      return;
    }

    const userDoc = await admin.firestore()
      .collection("users")
      .doc(receiverUid)
      .get();

    const token = userDoc.data()?.fcmToken;
    if (!token) {
      console.log("[Push] ❌ no fcmToken");
      return;
    }

    await admin.messaging().send({
      token,
      notification: {
        title: "MateMatch",
        body: text || "새 메시지가 도착했습니다."
      }
    });

    console.log(`[Push] 📨 single push sent → ${receiverUid}`);
  }
);
