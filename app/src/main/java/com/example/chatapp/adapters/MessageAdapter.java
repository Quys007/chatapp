package com.example.chatapp.adapters;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.models.Message;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT_TEXT = 1;
    private static final int VIEW_TYPE_RECEIVED_TEXT = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
    private static final int VIEW_TYPE_SENT_FILE = 5;
    private static final int VIEW_TYPE_RECEIVED_FILE = 6;
    private static final int VIEW_TYPE_SENT_VOICE = 7;
    private static final int VIEW_TYPE_RECEIVED_VOICE = 8;

    private final Context context;
    private final List<Message> messageList;
    private final String currentUserId;
    private MediaPlayer mediaPlayer;
    private int currentlyPlayingPosition = -1;

    public MessageAdapter(Context context, List<Message> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);

        switch (message.getType()) {
            case "image":
                return isSent ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_RECEIVED_IMAGE;
            case "file":
                return isSent ? VIEW_TYPE_SENT_FILE : VIEW_TYPE_RECEIVED_FILE;
            case "voice":
                return isSent ? VIEW_TYPE_SENT_VOICE : VIEW_TYPE_RECEIVED_VOICE;
            default:
                return isSent ? VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case VIEW_TYPE_SENT_TEXT:
                return new SentTextViewHolder(inflater.inflate(R.layout.item_container_sent_message, parent, false));
            case VIEW_TYPE_RECEIVED_TEXT:
                return new ReceivedTextViewHolder(inflater.inflate(R.layout.item_container_received_message, parent, false));
            case VIEW_TYPE_SENT_IMAGE:
                return new SentImageViewHolder(inflater.inflate(R.layout.item_container_sent_image, parent, false));
            case VIEW_TYPE_RECEIVED_IMAGE:
                return new ReceivedImageViewHolder(inflater.inflate(R.layout.item_container_received_image, parent, false));
            case VIEW_TYPE_SENT_FILE:
                return new SentFileViewHolder(inflater.inflate(R.layout.item_container_sent_file, parent, false));
            case VIEW_TYPE_RECEIVED_FILE:
                return new ReceivedFileViewHolder(inflater.inflate(R.layout.item_container_received_file, parent, false));
            case VIEW_TYPE_SENT_VOICE:
                return new SentVoiceViewHolder(inflater.inflate(R.layout.item_container_sent_voice, parent, false));
            case VIEW_TYPE_RECEIVED_VOICE:
                return new ReceivedVoiceViewHolder(inflater.inflate(R.layout.item_container_received_voice, parent, false));
            default:
                throw new IllegalArgumentException("Invalid ViewType");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        String formattedTime = formatTime(message.getTimestamp());

        if (holder instanceof SentTextViewHolder) {
            SentTextViewHolder vh = (SentTextViewHolder) holder;
            vh.txtMessage.setText(message.getContent());
            vh.txtTime.setText(formattedTime);
            vh.txtStatus.setText(message.isSeen() ? "Đã đọc" : "Đã gửi");
        } else if (holder instanceof ReceivedTextViewHolder) {
            ReceivedTextViewHolder vh = (ReceivedTextViewHolder) holder;
            vh.txtMessage.setText(message.getContent());
            vh.txtTime.setText(formattedTime);
        } else if (holder instanceof SentImageViewHolder) {
            SentImageViewHolder vh = (SentImageViewHolder) holder;
            Glide.with(context).load(message.getContent()).into(vh.imgMessage);
            vh.txtTime.setText(formattedTime);
            vh.txtStatus.setText(message.isSeen() ? "Đã đọc" : "Đã gửi");
        } else if (holder instanceof ReceivedImageViewHolder) {
            ReceivedImageViewHolder vh = (ReceivedImageViewHolder) holder;
            Glide.with(context).load(message.getContent()).into(vh.imgMessage);
            vh.txtTime.setText(formattedTime);
        } else if (holder instanceof SentVoiceViewHolder) {
            SentVoiceViewHolder vh = (SentVoiceViewHolder) holder;
            vh.txtDuration.setText(message.getDuration() + "s");
            vh.txtTime.setText(formattedTime);
            vh.txtStatus.setText(message.isSeen() ? "Đã đọc" : "Đã gửi");
            vh.btnPlay.setOnClickListener(v -> playAudio(message.getContent(), position));
        } else if (holder instanceof ReceivedVoiceViewHolder) {
            ReceivedVoiceViewHolder vh = (ReceivedVoiceViewHolder) holder;
            vh.txtDuration.setText(message.getDuration() + "s");
            vh.txtTime.setText(formattedTime);
            vh.btnPlay.setOnClickListener(v -> playAudio(message.getContent(), position));
        }
    }

    private void playAudio(String audioUrl, int position) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });
        } catch (IOException e) {
            Toast.makeText(context, "Không thể phát âm thanh", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolders
    static class SentTextViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime, txtStatus;
        SentTextViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.tvMessage);
            txtTime = itemView.findViewById(R.id.tvTime);
            txtStatus = itemView.findViewById(R.id.tvSeen);
        }
    }

    static class ReceivedTextViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;
        ReceivedTextViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.tvMessage);
            txtTime = itemView.findViewById(R.id.tvTime);
        }
    }

    static class SentImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMessage;
        TextView txtTime, txtStatus;
        SentImageViewHolder(View itemView) {
            super(itemView);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            txtTime = itemView.findViewById(R.id.tvTime);
            txtStatus = itemView.findViewById(R.id.tvSeen);
        }
    }

    static class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMessage;
        TextView txtTime;
        ReceivedImageViewHolder(View itemView) {
            super(itemView);
            imgMessage = itemView.findViewById(R.id.imgMessage);
            txtTime = itemView.findViewById(R.id.tvTime);
        }
    }

    static class SentFileViewHolder extends RecyclerView.ViewHolder {
        TextView txtFileName, txtTime, txtStatus;
        SentFileViewHolder(View itemView) {
            super(itemView);
            txtFileName = itemView.findViewById(R.id.txtSentFileName);
            txtTime = itemView.findViewById(R.id.txtSentTime);
            txtStatus = itemView.findViewById(R.id.txtSentStatus);
        }
    }

    static class ReceivedFileViewHolder extends RecyclerView.ViewHolder {
        TextView txtFileName, txtTime;
        ReceivedFileViewHolder(View itemView) {
            super(itemView);
            txtFileName = itemView.findViewById(R.id.txtReceivedFileName);
            txtTime = itemView.findViewById(R.id.txtReceivedTime);
        }
    }

    static class SentVoiceViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnPlay;
        TextView txtDuration, txtTime, txtStatus;
        SentVoiceViewHolder(View itemView) {
            super(itemView);
            btnPlay = itemView.findViewById(R.id.btnPlaySentVoice);
            txtDuration = itemView.findViewById(R.id.txtSentVoiceDuration);
            txtTime = itemView.findViewById(R.id.txtSentTime);
            txtStatus = itemView.findViewById(R.id.txtSentStatus);
        }
    }

    static class ReceivedVoiceViewHolder extends RecyclerView.ViewHolder {
        ImageButton btnPlay;
        TextView txtDuration, txtTime;
        ReceivedVoiceViewHolder(View itemView) {
            super(itemView);
            btnPlay = itemView.findViewById(R.id.btnPlayReceivedVoice);
            txtDuration = itemView.findViewById(R.id.txtReceivedVoiceDuration);
            txtTime = itemView.findViewById(R.id.txtReceivedTime);
        }
    }
}
