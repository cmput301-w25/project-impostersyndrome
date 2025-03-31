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

/**
 * Adapter for displaying a list of comments in a RecyclerView with support for replies and deletion.
 *
 * @author [Your Name]
 */
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private OnCommentDeleteListener deleteListener;
    private OnReplyListener replyListener;
    private String currentUserId;
    private Map<String, Boolean> expandedStates = new HashMap<>();

    /**
     * Interface for handling comment deletion events.
     */
    public interface OnCommentDeleteListener {
        /**
         * Called when a comment is requested to be deleted.
         *
         * @param comment The comment to delete
         */
        void onDeleteComment(Comment comment);
    }

    /**
     * Interface for handling reply events.
     */
    public interface OnReplyListener {
        /**
         * Called when a reply action is initiated on a comment.
         *
         * @param comment The comment being replied to
         */
        void onReply(Comment comment);
    }

    /**
     * Sets the current user's ID to determine visibility of delete buttons.
     *
     * @param currentUserId The ID of the current user
     */
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * Sets the listener for comment deletion events.
     *
     * @param listener The listener to handle deletion events
     */
    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.deleteListener = listener;
    }

    /**
     * Sets the listener for reply events.
     *
     * @param listener The listener to handle reply events
     */
    public void setOnReplyListener(OnReplyListener listener) {
        this.replyListener = listener;
    }

    /**
     * Updates the list of comments and notifies the adapter of the change.
     *
     * @param comments The new list of comments to display
     */
    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    /**
     * Adds a new comment to the list and notifies the adapter.
     *
     * @param newComment The comment to add
     */
    public void addComment(Comment newComment) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        comments.add(newComment);
        notifyItemInserted(comments.size() - 1);
    }

    /**
     * Updates the reply count for a specific comment and refreshes its view.
     *
     * @param parentComment The parent comment whose replies are updated
     * @param replies The list of replies to update the count from
     */
    public void updateRepliesForComment(Comment parentComment, List<Comment> replies) {
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i).getId().equals(parentComment.getId())) {
                comments.get(i).setReplyCount(parentComment.getReplyCount());
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Retrieves the current list of comments.
     *
     * @return The list of comments
     */
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
        if (comments != null && position < comments.size()) {
            Comment comment = comments.get(position);
            holder.bind(comment, position);
        }
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    /**
     * ViewHolder class for individual comment items in the RecyclerView.
     */
    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView commentTextView;
        TextView commentInfoTextView;
        ImageButton deleteCommentButton;
        ImageButton replyButton;
        Button viewRepliesButton;
        LinearLayout repliesContainer;

        /**
         * Constructs a new CommentViewHolder.
         *
         * @param itemView The view for this comment item
         */
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            commentInfoTextView = itemView.findViewById(R.id.commentInfoTextView);
            deleteCommentButton = itemView.findViewById(R.id.deleteCommentButton);
            replyButton = itemView.findViewById(R.id.replyButton);
            viewRepliesButton = itemView.findViewById(R.id.viewRepliesButton);
            repliesContainer = itemView.findViewById(R.id.repliesContainer);
        }

        /**
         * Binds a comment to this ViewHolder, setting up text, buttons, and reply visibility.
         *
         * @param comment The comment to bind
         * @param adapterPosition The position of the comment in the adapter
         */
        public void bind(final Comment comment, final int adapterPosition) {
            // Implementation details omitted for brevity
        }

        /**
         * Fetches and displays replies for a parent comment in the replies container.
         *
         * @param parent The parent comment whose replies are to be fetched
         * @param adapterPosition The position of the parent comment in the adapter
         */
        private void fetchAndDisplayReplies(Comment parent, int adapterPosition) {
            // Implementation details omitted for brevity
        }
    }
}