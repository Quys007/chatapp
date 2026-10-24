package com.example.chatapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.adapters.MessageAdapter;
import com.example.chatapp.models.Message;
import com.example.chatapp.models.User;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText edtInputMessage;
    private ImageButton btnSend, btnAttachImage, btnRecordVoice;
    private ImageView imgToolbarAvatar;
    private TextView txtToolbarName, txtToolbarStatus;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String currentUserId = "USER_A_ID";
    private String receiverId = "USER_B_ID";
    private String chatId;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private long voiceStartTime;

    private ListenerRegistration messageListener;
    private ListenerRegistration presenceListener;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadAndSendMessage(uri, "image", 0);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        chatId = generateChatId(currentUserId, receiverId);

        initViews();
        setupRecyclerView();
        listenToReceiverPresence();
        listenToMessages();
        setupClickListeners();
    }

    private void initViews() {
        recyclerViewChat = findViewById(R.id.rvMessages);
        edtInputMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttachImage = findViewById(R.id.btnAttach);
        btnRecordVoice = findViewById(R.id.btnMic);
        imgToolbarAvatar = findViewById(R.id.imgAvatar);
        txtToolbarName = findViewById(R.id.tvName);
        txtToolbarStatus = findViewById(R.id.tvStatus);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> {
            String text = edtInputMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage("text", text, 0);
                edtInputMessage.setText("");
            }
        });

        btnAttachImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnRecordVoice.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startRecording();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                stopRecordingAndSend();
            }
            return true;
        });
    }

    private String generateChatId(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void listenToReceiverPresence() {
        presenceListener = db.collection("users").document(receiverId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    User receiver = snapshot.toObject(User.class);
                    if (receiver != null) {
                        txtToolbarName.setText(receiver.getDisplayName());
                        Glide.with(this).load(receiver.getAvatarUrl()).into(imgToolbarAvatar);
                        if (receiver.isOnline()) {
                            txtToolbarStatus.setText("Đang hoạt động");
                        } else {
                            txtToolbarStatus.setText("Hoạt động gần đây");
                        }
                    }
                });
    }

    private void listenToMessages() {
        messageListener = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    for (DocumentChange doc : value.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            Message message = doc.getDocument().toObject(Message.class);
                            messageList.add(message);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerViewChat.smoothScrollToPosition(messageList.size() - 1);

                            // Cập nhật 'isSeen = true' nếu là người nhận
                            if (message.getReceiverId().equals(currentUserId) && !message.isSeen()) {
                                doc.getDocument().getReference().update("isSeen", true);
                            }
                        } else if (doc.getType() == DocumentChange.Type.MODIFIED) {
                            Message modifiedMessage = doc.getDocument().toObject(Message.class);
                            for (int i = 0; i < messageList.size(); i++) {
                                if (messageList.get(i).getMessageId().equals(modifiedMessage.getMessageId())) {
                                    messageList.set(i, modifiedMessage);
                                    messageAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private void sendMessage(String type, String content, long duration) {
        String messageId = db.collection("chats").document(chatId).collection("messages").document().getId();
        long timestamp = System.currentTimeMillis();

        Message message = new Message(messageId, currentUserId, receiverId, type, content, timestamp, false, duration);

        db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .set(message);

        Map<String, Object> lastMsgMap = new HashMap<>();
        lastMsgMap.put("members", java.util.Arrays.asList(currentUserId, receiverId));
        lastMsgMap.put("updatedAt", timestamp);

        Map<String, Object> lastMsgContent = new HashMap<>();
        lastMsgContent.put("content", type.equals("text") ? content : "[" + type + "]");
        lastMsgContent.put("senderId", currentUserId);
        lastMsgContent.put("timestamp", timestamp);
        lastMsgMap.put("lastMessage", lastMsgContent);

        db.collection("chats").document(chatId).set(lastMsgMap, com.google.firebase.firestore.SetOptions.merge());
    }

    private void uploadAndSendMessage(Uri fileUri, String fileType, long duration) {
        String fileName = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child("chat_media/" + chatId + "/" + fileName);

        ref.putFile(fileUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri ->
                        sendMessage(fileType, uri.toString(), duration)
                ))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload lỗi!", Toast.LENGTH_SHORT).show());
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            return;
        }

        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".m4a";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            voiceStartTime = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingAndSend() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                long duration = (System.currentTimeMillis() - voiceStartTime) / 1000;
                if (duration >= 1) {
                    Uri fileUri = Uri.fromFile(new File(audioFilePath));
                    uploadAndSendMessage(fileUri, "voice", duration);
                } else {
                    Toast.makeText(this, "Ghi âm quá ngắn", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUserPresence(boolean isOnline) {
        Map<String, Object> map = new HashMap<>();
        map.put("isOnline", isOnline);
        map.put("lastSeen", System.currentTimeMillis());
        db.collection("users").document(currentUserId).update(map);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserPresence(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUserPresence(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
        if (presenceListener != null) presenceListener.remove();
    }
}