# ChatApp

Ứng dụng nhắn tin 1-1 realtime trên Android, sử dụng Firebase Firestore và Firebase Storage.

## Tính năng

- Nhắn tin văn bản theo thời gian thực
- Gửi hình ảnh từ thư viện (chọn bằng `ActivityResultContracts.GetContent`)
- Gửi tin nhắn thoại (nhấn giữ nút micro để ghi âm, thả ra để gửi)
- Hiển thị trạng thái hoạt động của người nhận ("Đang hoạt động" / "Hoạt động gần đây")
- Đánh dấu tin nhắn đã xem (`isSeen`)
- Lưu tin nhắn cuối cùng của mỗi cuộc trò chuyện để làm danh sách chat

## Công nghệ sử dụng

| Thành phần | Mô tả |
|---|---|
| Ngôn ngữ | Java |
| UI | AppCompat, RecyclerView, EdgeToEdge |
| Backend | Firebase Firestore (tin nhắn, người dùng) |
| Lưu trữ | Firebase Storage (ảnh, file ghi âm) |
| Tải ảnh | Glide |
| Ghi âm | `MediaRecorder` (MPEG-4 / AAC, định dạng `.m4a`) |

## Cấu trúc dự án

```
com.example.chatapp
├── ChatActivity.java        # Màn hình chat chính
├── adapters
│   └── MessageAdapter.java  # Adapter hiển thị danh sách tin nhắn
└── models
    ├── Message.java         # Model tin nhắn
    └── User.java            # Model người dùng
```

## Cấu trúc dữ liệu Firebase

### Firestore

```
users/{userId}
    displayName : String
    avatarUrl   : String
    isOnline    : Boolean
    lastSeen    : Long (millis)

chats/{chatId}
    members     : [userIdA, userIdB]
    updatedAt   : Long (millis)
    lastMessage : { content, senderId, timestamp }

chats/{chatId}/messages/{messageId}
    messageId, senderId, receiverId
    type        : "text" | "image" | "voice"
    content     : String (nội dung hoặc URL tải về)
    timestamp   : Long (millis)
    isSeen      : Boolean
    duration    : Long (giây, dùng cho tin nhắn thoại)
```

`chatId` được tạo bằng cách ghép hai `userId` theo thứ tự bảng chữ cái (`idNhoHon_idLonHon`), nên cả hai phía luôn dùng chung một cuộc trò chuyện.

### Storage

```
chat_media/{chatId}/{uuid}
```

## Cài đặt

1. Clone dự án và mở bằng Android Studio.
2. Tạo dự án trên [Firebase Console](https://console.firebase.google.com/), thêm ứng dụng Android với package `com.example.chatapp`.
3. Tải `google-services.json` và đặt vào thư mục `app/`.
4. Bật **Cloud Firestore** và **Firebase Storage** trong Firebase Console.
5. Thêm các dependency cần thiết vào `app/build.gradle`:

```gradle
implementation platform('com.google.firebase:firebase-bom:<phiên bản>')
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-storage'
implementation 'com.github.bumptech.glide:glide:<phiên bản>'
implementation 'androidx.recyclerview:recyclerview:<phiên bản>'
```

6. Khai báo quyền trong `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

7. Build và chạy ứng dụng trên thiết bị hoặc máy ảo.

## Cấu hình người dùng

Hiện tại ID người dùng đang được gán cứng trong `ChatActivity`:

```java
private String currentUserId = "USER_A_ID";
private String receiverId = "USER_B_ID";
```

Hãy thay bằng ID thật (ví dụ lấy từ `FirebaseAuth` hoặc truyền qua `Intent`) và tạo sẵn hai document tương ứng trong collection `users`.

## Cách sử dụng

- **Gửi văn bản:** nhập nội dung rồi nhấn nút gửi.
- **Gửi ảnh:** nhấn nút đính kèm và chọn ảnh.
- **Gửi giọng nói:** nhấn giữ nút micro để ghi, thả ra để gửi (ghi âm dưới 1 giây sẽ bị huỷ).

## Hạn chế và hướng phát triển

- Chưa xử lý kết quả cấp quyền ghi âm (`onRequestPermissionsResult`), lần cấp quyền đầu tiên người dùng phải nhấn giữ lại.
- Chưa có chức năng phát lại tin nhắn thoại trong adapter (cần `MediaPlayer`/`ExoPlayer`).
- Trạng thái online được cập nhật theo `onResume`/`onPause` của Activity; nên dùng thêm `onDisconnect` hoặc Realtime Database để chính xác hơn.
- Cần cấu hình Firestore/Storage Security Rules trước khi đưa vào sử dụng thực tế.
- Nên bổ sung đăng nhập (Firebase Authentication), danh sách cuộc trò chuyện và thông báo đẩy (FCM).


