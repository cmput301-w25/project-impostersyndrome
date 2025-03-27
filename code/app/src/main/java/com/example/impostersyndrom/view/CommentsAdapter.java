package com.example.impostersyndrom.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.Comment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private OnCommentDeleteListener deleteListener;

    public void addComment(Comment newComment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(newComment);
        notifyItemInserted(comments.size() - 1);
    }

    public interface OnCommentDeleteListener {
        void onDeleteComment(Comment comment);
    }

    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        if (comments != null && position < comments.size()){
            Comment comment = comments.get(position);
            holder.bind(comment);
        }
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView commentTextView;
        TextView commentInfoTextView;
        ImageButton deleteCommentButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            commentInfoTextView = itemView.findViewById(R.id.commentInfoTextView);
            deleteCommentButton = itemView.findViewById(R.id.deleteCommentButton);
        }

        public void bind(final Comment comment) {
            commentTextView.setText(comment.getText());
            String timeString = "";
            if(comment.getTimestamp() != null) {
                timeString = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(comment.getTimestamp());
            }
            commentInfoTextView.setText("Posted by " + comment.getUsername() + " at " + timeString);

            deleteCommentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (deleteListener != null) {
                        deleteListener.onDeleteComment(comment);
                    }
                }
            });
        }
    }
}
