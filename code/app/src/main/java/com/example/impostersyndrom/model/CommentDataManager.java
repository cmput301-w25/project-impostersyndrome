package com.example.impostersyndrom.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class CommentDataManager {

    private FirebaseFirestore db;
    private CollectionReference commentsCollection;

    public CommentDataManager() {
        db = FirebaseFirestore.getInstance();
        // "comments" is the new collection for comment data in Firestore
        commentsCollection = db.collection("comments");
    }

    public interface OnCommentAddedListener {
        void onCommentAdded();
        void onError(String errorMessage);
    }

    public void addComment(Comment comment, final OnCommentAddedListener listener) {
        // Use the comment's id as the document id
        commentsCollection.document(comment.getId()).set(comment)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onCommentAdded();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public interface OnCommentsFetchedListener {
        void onCommentsFetched(List<Comment> comments);
        void onError(String errorMessage);
    }

    public void fetchComments(String postId, final OnCommentsFetchedListener listener) {
        // Query comments for a given postId ordered by timestamp ascending
        commentsCollection.whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment comment = doc.toObject(Comment.class);
                        comments.add(comment);
                    }
                    if (listener != null) listener.onCommentsFetched(comments);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public interface OnCommentDeletedListener {
        void onCommentDeleted();
        void onError(String errorMessage);
    }

    public void deleteComment(String commentId, final OnCommentDeletedListener listener) {
        commentsCollection.document(commentId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onCommentDeleted();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
}
