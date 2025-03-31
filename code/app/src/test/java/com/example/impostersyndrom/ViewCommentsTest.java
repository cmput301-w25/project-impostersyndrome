package com.example.impostersyndrom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.impostersyndrom.model.Comment;
import com.example.impostersyndrom.model.CommentDataManager;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.Date;


@RunWith(MockitoJUnitRunner.class)
public class ViewCommentsTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockCommentsCollection;

    @Mock
    private DocumentReference mockMoodDocument;

    @Mock
    private Query mockQuery;

    private CommentDataManager commentDataManager;
    private final String testMoodId = "mood123";
    private Comment comment1, comment2;

    @Before
    public void setUp() {
        // initialize the real CommentDataManager with mocked dependencies
        commentDataManager = new CommentDataManager();

        // modify CommentDataManager to accept Firestore in constructor
        // commentDataManager = new CommentDataManager(mockFirestore);

        comment1 = new Comment(testMoodId, "user1", "Alice", "First comment", new Date());
        comment2 = new Comment(testMoodId, "user2", "Bob", "Second comment", new Date());

        when(mockFirestore.collection("moods")).thenReturn(mockCommentsCollection);
        when(mockCommentsCollection.document(testMoodId)).thenReturn(mockMoodDocument);
        when(mockMoodDocument.collection("comments")).thenReturn(mockCommentsCollection);
        when(mockCommentsCollection.whereEqualTo("parentId", null)).thenReturn(mockQuery);
    }

    @Test
    public void testFetchComments_Success() {
        // setup fo mock query behavior
        when(mockQuery.orderBy("timestamp", any())).thenReturn(mockQuery);

        CommentDataManager.OnCommentsFetchedListener mockListener =
                mock(CommentDataManager.OnCommentsFetchedListener.class);
        commentDataManager.fetchComments(testMoodId, mockListener);
        verify(mockCommentsCollection).whereEqualTo("parentId", null);
        verify(mockQuery).orderBy("timestamp", any());
    }

    @Test
    public void testAddComment() {
        when(mockCommentsCollection.document()).thenReturn(mock(DocumentReference.class));

        CommentDataManager.OnCommentAddedListener mockListener =
                mock(CommentDataManager.OnCommentAddedListener.class);

        commentDataManager.addComment(testMoodId, comment1, mockListener);

        verify(mockCommentsCollection).document();
    }
}