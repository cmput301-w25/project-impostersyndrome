package com.example.impostersyndrom.controller;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.Comment;
import com.example.impostersyndrom.model.CommentDataManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private OnCommentDeleteListener deleteListener;
    private OnReplyListener replyListener;
    private String currentUserId;

    // Map to track expanded state for each comment (true if replies are shown)
    private Map<String, Boolean> expandedStates = new HashMap<>();

    public interface OnCommentDeleteListener {
        void onDeleteComment(Comment comment);
    }

    public interface OnReplyListener {
        void onReply(Comment comment);
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnReplyListener(OnReplyListener listener) {
        this.replyListener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void addComment(Comment newComment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(newComment);
        notifyItemInserted(comments.size() - 1);
    }

    // Method to update replies for a comment, if needed
    public void updateRepliesForComment(Comment parentComment, List<Comment> replies) {
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i).getId().equals(parentComment.getId())) {
                comments.get(i).setReplyCount(parentComment.getReplyCount());
                notifyItemChanged(i);
                break;
            }
        }
    }

    public List<Comment> getComments() {
        return comments;
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
            holder.bind(comment, position);
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
        ImageButton replyButton;
        Button viewRepliesButton;
        LinearLayout repliesContainer;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            commentInfoTextView = itemView.findViewById(R.id.commentInfoTextView);
            deleteCommentButton = itemView.findViewById(R.id.deleteCommentButton);
            replyButton = itemView.findViewById(R.id.replyButton);
            viewRepliesButton = itemView.findViewById(R.id.viewRepliesButton);
            repliesContainer = itemView.findViewById(R.id.repliesContainer);
        }

        public void bind(final Comment comment, final int adapterPosition) {
            // Set main comment text and info
            commentTextView.setText(comment.getText());
            String timeString = "";
            if(comment.getTimestamp() != null) {
                timeString = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                        .format(comment.getTimestamp());
            }
            commentInfoTextView.setText("Posted by " + comment.getUsername() + " at " + timeString);

            // Show delete button only for comments by current user
            if (currentUserId != null && currentUserId.equals(comment.getUserId())) {
                deleteCommentButton.setVisibility(View.VISIBLE);
                deleteCommentButton.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteComment(comment);
                    }
                });
            } else {
                deleteCommentButton.setVisibility(View.INVISIBLE);
            }

            // Show reply button only for top level comments
            if (comment.getParentId() == null) {
                replyButton.setVisibility(View.VISIBLE);
                replyButton.setOnClickListener(v -> {
                    if (replyListener != null) {
                        replyListener.onReply(comment);
                    }
                });
            } else {
                replyButton.setVisibility(View.GONE);
            }

            // Reset replies container state
            repliesContainer.removeAllViews();
            repliesContainer.setVisibility(View.GONE);
            viewRepliesButton.setText("View Replies");

            // Show the viewRepliesButton only if replyCount > 0
            if (comment.getReplyCount() > 0) {
                viewRepliesButton.setVisibility(View.VISIBLE);
            } else {
                viewRepliesButton.setVisibility(View.GONE);
            }

            // Check the expanded state from our map and display accordingly
            boolean isExpanded = expandedStates.getOrDefault(comment.getId(), false);
            if (isExpanded) {
                fetchAndDisplayReplies(comment, adapterPosition);
            } else {
                repliesContainer.setVisibility(View.GONE);
                viewRepliesButton.setText("View Replies");
            }

            // Toggle expansion state on viewRepliesButton click
            viewRepliesButton.setOnClickListener(v -> {
                boolean currentlyExpanded = expandedStates.getOrDefault(comment.getId(), false);
                if (!currentlyExpanded) {
                    expandedStates.put(comment.getId(), true);
                    fetchAndDisplayReplies(comment, adapterPosition);
                } else {
                    expandedStates.put(comment.getId(), false);
                    repliesContainer.setVisibility(View.GONE);
                    viewRepliesButton.setText("View Replies");
                }
            });
        }

        // Helper method to fetch and display replies for a given parent comment
        private void fetchAndDisplayReplies(Comment parent, int adapterPosition) {
            new CommentDataManager().fetchReplies(parent.getMoodId(), parent.getId(), new CommentDataManager.OnRepliesFetchedListener() {
                @Override
                public void onRepliesFetched(List<Comment> replies) {
                    repliesContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
                    for (Comment reply : replies) {
                        View replyView = inflater.inflate(R.layout.item_comment, repliesContainer, false);
                        TextView replyText = replyView.findViewById(R.id.commentTextView);
                        TextView replyInfo = replyView.findViewById(R.id.commentInfoTextView);
                        replyText.setText(reply.getText());
                        String replyTime = reply.getTimestamp() != null
                                ? new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(reply.getTimestamp())
                                : "";
                        replyInfo.setText("Posted by " + reply.getUsername() + " at " + replyTime);
                        replyView.findViewById(R.id.replyButton).setVisibility(View.GONE);
                        // Show delete button for this reply if it belongs to current user
                        if (currentUserId != null && currentUserId.equals(reply.getUserId())) {
                            replyView.findViewById(R.id.deleteCommentButton).setVisibility(View.VISIBLE);
                            replyView.findViewById(R.id.deleteCommentButton).setOnClickListener(v -> {
                                if (deleteListener != null) {
                                    deleteListener.onDeleteComment(reply);
                                }
                            });
                        } else {
                            replyView.findViewById(R.id.deleteCommentButton).setVisibility(View.GONE);
                        }
                        repliesContainer.addView(replyView);
                    }
                    // If no replies exist, collapse the view and update expanded state
                    if (replies.isEmpty()) {
                        repliesContainer.setVisibility(View.GONE);
                        viewRepliesButton.setText("View Replies");
                        expandedStates.put(parent.getId(), false);
                        comments.get(adapterPosition).setReplyCount(0);
                        notifyItemChanged(adapterPosition);
                    } else {
                        repliesContainer.setVisibility(View.VISIBLE);
                        viewRepliesButton.setText("Hide Replies");
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e("CommentsAdapter", "Failed to load replies: " + errorMessage);
                    expandedStates.put(parent.getId(), false);
                    repliesContainer.setVisibility(View.GONE);
                    viewRepliesButton.setText("View Replies");
                }
            });
        }
    }
}
