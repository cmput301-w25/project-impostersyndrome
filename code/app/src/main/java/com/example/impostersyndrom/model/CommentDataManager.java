package com.example.impostersyndrom.model;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.ArrayList;

import com.google.firebase.firestore.FieldValue;

public class CommentDataManager {

    private FirebaseFirestore db;

    public CommentDataManager() {
        db = FirebaseFirestore.getInstance();
    }

    // Returns the comments subcollection for a given mood
    private CollectionReference getCommentsCollection(String moodId) {
        return db.collection("moods").document(moodId).collection("comments");
    }

    public interface OnCommentAddedListener {
        void onCommentAdded();

        void onError(String errorMessage);
    }

    public interface OnRepliesFetchedListener {
        void onRepliesFetched(List<Comment> replies);

        void onError(String errorMessage);
    }

    public void addComment(String moodId, Comment comment, final OnCommentAddedListener listener) {
        String newId = getCommentsCollection(moodId).document().getId();
        comment.setId(newId);
        // For a top-level comment, ensure parentId is null
        if (comment.getParentId() == null) {
            comment.setParentId(null);
        }
        getCommentsCollection(moodId).document(newId).set(comment)
                .addOnSuccessListener(aVoid -> {
                    // If this is a reply, update the parent's replyCount
                    if (comment.getParentId() != null) {
                        getCommentsCollection(moodId).document(comment.getParentId())
                                .update("replyCount", com.google.firebase.firestore.FieldValue.increment(1));
                    }
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

    public void fetchComments(String moodId, final OnCommentsFetchedListener listener) {
        getCommentsCollection(moodId)
                .whereEqualTo("parentId", null)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                        }
                        comments.add(comment);
                    }
                    if (listener != null) listener.onCommentsFetched(comments);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void fetchReplies(String moodId, String parentId, final OnRepliesFetchedListener listener) {
        getCommentsCollection(moodId)
                .whereEqualTo("parentId", parentId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> replies = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment reply = doc.toObject(Comment.class);
                        if (reply != null) {
                            reply.setId(doc.getId());
                        }
                        replies.add(reply);
                    }
                    if (listener != null) listener.onRepliesFetched(replies);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public interface OnCommentDeletedListener {
        void onCommentDeleted();

        void onError(String errorMessage);
    }
    public void deleteCommentAndReplies(String moodId, String commentId, final OnCommentDeletedListener listener) {
        // First, retrieve the comment to know if it is a reply
        DocumentReference commentRef = getCommentsCollection(moodId).document(commentId);
        commentRef.get().addOnSuccessListener(documentSnapshot -> {
            Comment comment = documentSnapshot.toObject(Comment.class);
            if (comment == null) {
                if (listener != null) listener.onError("Comment not found");
                return;
            }
            // If this comment is a reply, decrement the parent's replyCount
            if (comment.getParentId() != null) {
                getCommentsCollection(moodId).document(comment.getParentId())
                        .update("replyCount", FieldValue.increment(-1));
            }
            // Now, query for child replies of this comment.
            getCommentsCollection(moodId)
                    .whereEqualTo("parentId", commentId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Task<Void>> deletionTasks = new ArrayList<>();
                        // For each child reply, recursively delete it.
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            deletionTasks.add(deleteCommentAndRepliesTask(moodId, doc.getId()));
                        }
                        // After all child deletions are done, delete this comment
                        Tasks.whenAll(deletionTasks)
                                .addOnSuccessListener(aVoid -> commentRef.delete()
                                        .addOnSuccessListener(aVoid2 -> {
                                            if (listener != null) listener.onCommentDeleted();
                                        })
                                        .addOnFailureListener(e -> {
                                            if (listener != null) listener.onError(e.getMessage());
                                        }))
                                .addOnFailureListener(e -> {
                                    if (listener != null) listener.onError(e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) listener.onError(e.getMessage());
                    });
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    // Helper method to wrap the recursive deletion in a Task
    private Task<Void> deleteCommentAndRepliesTask(String moodId, String commentId) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        deleteCommentAndReplies(moodId, commentId, new OnCommentDeletedListener() {
            @Override
            public void onCommentDeleted() {
                tcs.setResult(null);
            }

            @Override
            public void onError(String errorMessage) {
                tcs.setException(new Exception(errorMessage));
            }
        });
        return tcs.getTask();
    }
}
