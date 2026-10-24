package com.example.chatapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChats;
    private FirebaseFirestore db;
    private String currentUserId = "USER_A_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db = FirebaseFirestore.getInstance();
        recyclerViewChats = findViewById(R.id.rvConversations);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        loadChatConversations();
    }

    private void loadChatConversations() {
        db.collection("chats")
                .whereArrayContains("members", currentUserId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    for (DocumentChange doc : value.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED || doc.getType() == DocumentChange.Type.MODIFIED) {
                            // Cập nhật UI danh sách hội thoại
                        }
                    }
                });
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
}