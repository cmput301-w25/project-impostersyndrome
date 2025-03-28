package com.example.impostersyndrom.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import java.util.List;
import java.util.ArrayList;

public class CommentDataManager {

    private FirebaseFirestore db;

    public CommentDataManager() {
        db = FirebaseFirestore.getInstance();
    }

    // Helper: returns the comments subcollection for a given mood
    private CollectionReference getCommentsCollection(String moodId) {
        return db.collection("moods").document(moodId).collection("comments");
    }

    public interface OnCommentAddedListener {
        void onCommentAdded();
        void onError(String errorMessage);
    }

    /**
     * Adds a comment to the comments subcollection for the specified mood.
     * For a top-level comment, ensure that parentId is null.
     *
     * @param moodId  The ID of the mood document.
     * @param comment The Comment object to add.
     * @param listener Callback for success or error.
     */
    public void addComment(String moodId, Comment comment, final OnCommentAddedListener listener) {
        String newId = getCommentsCollection(moodId).document().getId();
        comment.setId(newId);
        // For a top-level comment, ensure parentId is null:
        if(comment.getParentId() == null) {
            comment.setParentId(null);
        }
        getCommentsCollection(moodId).document(newId).set(comment)
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onCommentAdded(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onError(e.getMessage()); });
    }

    public interface OnCommentsFetchedListener {
        void onCommentsFetched(List<Comment> comments);
        void onError(String errorMessage);
    }

    /**
     * Fetches all top-level comments (parentId == null) for the specified mood.
     * Comments are ordered by the "timestamp" field in ascending order.
     *
     * @param moodId   The ID of the mood document.
     * @param listener Callback with the list of comments or an error.
     */
    public void fetchComments(String moodId, final OnCommentsFetchedListener listener) {
        getCommentsCollection(moodId)
                .whereEqualTo("parentId", null)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment comment = doc.toObject(Comment.class);
                        if(comment != null) {
                            comment.setId(doc.getId());
                        }
                        comments.add(comment);
                    }
                    if (listener != null) listener.onCommentsFetched(comments);
                })
                .addOnFailureListener(e -> { if (listener != null) listener.onError(e.getMessage()); });
    }

    public interface OnCommentDeletedListener {
        void onCommentDeleted();
        void onError(String errorMessage);
    }

    /**
     * Deletes a comment from the comments subcollection for the specified mood.
     *
     * @param moodId    The ID of the mood document.
     * @param commentId The ID of the comment to delete.
     * @param listener  Callback for success or error.
     */
    public void deleteComment(String moodId, String commentId, final OnCommentDeletedListener listener) {
        getCommentsCollection(moodId).document(commentId).delete()
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onCommentDeleted(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onError(e.getMessage()); });
    }
}
