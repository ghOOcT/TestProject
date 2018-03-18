package com.example.max.testproject.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.max.testproject.R;
import com.example.max.testproject.model.TestProject;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;


import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public ImageView messageImageViewOne;
        public ImageView messageImageViewTwo;
        public TextView messengerTextView;
        public TextView countViewOne;
        public TextView countViewTwo;
        public CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageViewOne = (ImageView) itemView.findViewById(R.id.messageImageViewOne);
            messageImageViewTwo = (ImageView) itemView.findViewById(R.id.messageImageViewTwo);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
            countViewOne = (TextView) itemView.findViewById(R.id.choose_count_one);
            countViewTwo = (TextView) itemView.findViewById(R.id.choose_count_two);
        }
    }


    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "choose";
    public static final String ANONYMOUS = "anonymous";

    public String mUsername;
    public String nameUser;
    public String mUserId;
    public String mPhotoUrl;
    public SharedPreferences mSharedPreferences;
    public GoogleApiClient mGoogleApiClient;

    private static Integer viewLikeOne,viewLikeTwo;
    Integer likeOne,likeTwo;

    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;

    public FirebaseAuth mFirebaseAuth;
    public FirebaseUser mFirebaseUser;


    public ImageView messageImageViewOne;
    public ImageView messageImageViewTwo;

    public DatabaseReference mFirebaseDatabaseReference;
    public FirebaseRecyclerAdapter<TestProject, MessageViewHolder>
            mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_add_choose_page:
                        Intent addChoose = new Intent(MainActivity.this, AddImage.class);
                        startActivity(addChoose);
                        break;
                    case R.id.navigation_tape_page:
                        Intent tapePage = new Intent(MainActivity.this, MainActivity.class);
                        startActivity(tapePage);
                        break;
                    case R.id.navigation_home_page:
                        Intent homePage = new Intent(MainActivity.this, UserActivity.class);
                        startActivity(homePage);
                        break;
                }
                return false;
            }
        });

        mUsername = ANONYMOUS;

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mUserId = mFirebaseUser.getUid();
        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SingInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();


        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        messageImageViewOne = (ImageView) findViewById(R.id.messageImageViewOne);
        messageImageViewTwo = (ImageView) findViewById(R.id.messageImageViewTwo);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        SnapshotParser<TestProject> parser = new SnapshotParser<TestProject>() {
            @Override
            public TestProject parseSnapshot(DataSnapshot dataSnapshot) {
                TestProject choose = dataSnapshot.getValue(TestProject.class);
                if (choose != null) {
                    choose.setmKey(dataSnapshot.getKey());
                }
                return choose;
            }
        };

        DatabaseReference messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        FirebaseRecyclerOptions<TestProject> options =
                new FirebaseRecyclerOptions.Builder<TestProject>()
                        .setQuery(messagesRef, parser)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<TestProject, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            final TestProject choose) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                viewHolder.messageTextView.setText(choose.getYourChoose());

                String imageUrlOne = choose.getImageUrlOne();
                String imageUrlTwo = choose.getImageUrlTwo();

                StorageReference storageReferenceOne = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrlOne);
                storageReferenceOne.getDownloadUrl().addOnCompleteListener(
                        new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String downloadUrlOne = task.getResult().toString();
                                    Picasso.with(viewHolder.messageImageViewOne.getContext())
                                            .load(downloadUrlOne)
                                            .fit()
                                            .placeholder(R.mipmap.ic_launcher)
                                            .into(viewHolder.messageImageViewOne);

                                } else {
                                    Log.w(TAG, "Getting download url was not successful.",
                                            task.getException());
                                }
                            }
                        });

                StorageReference storageReferenceTwo = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrlTwo);
                storageReferenceTwo.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            String downloadUrlTwo = task.getResult().toString();
                            Picasso.with(viewHolder.messageImageViewTwo.getContext())
                                    .load(downloadUrlTwo)
                                    .fit()
                                    .placeholder(R.mipmap.ic_launcher)
                                    .into(viewHolder.messageImageViewTwo);
                        } else {
                            Log.w(TAG, "Getting download url was not successful.",
                                    task.getException());
                        }
                    }
                });

                if (mUserId.equals(choose.getId())) {

                    viewLikeOne = choose.getChooseOne();
                    viewLikeTwo = choose.getChooseTwo();

                    viewHolder.countViewOne.setText("За первое фото проголосавали: " + viewLikeOne);
                    viewHolder.countViewTwo.setText("За второе фото проголосавали: " + viewLikeTwo);

                } else {

                    ValueEventListener valueEventListenerm = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            nameUser = dataSnapshot.child(MESSAGES_CHILD).child(choose.getmKey()).child("VotesUSers")
                                    .child(mFirebaseAuth.getCurrentUser().getDisplayName()).getValue(String.class);

                            if (mFirebaseUser.getUid().equals(nameUser)) {

                                viewHolder.messageImageViewOne.setEnabled(false);
                                viewHolder.messageImageViewTwo.setEnabled(false);

                                viewLikeOne = choose.getChooseOne();
                                viewLikeTwo = choose.getChooseTwo();

                                viewHolder.countViewOne.setText("За первое фото проголосавали: " + viewLikeOne);
                                viewHolder.countViewTwo.setText("За второе фото проголосавали: " + viewLikeTwo);

                            } else {

                                viewHolder.messageImageViewOne.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(final View view) {

                                        DatabaseReference upvotes = mFirebaseDatabaseReference.child(MESSAGES_CHILD)
                                                .child(choose.getmKey()).child("chooseOne");
                                        upvotes.runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                likeOne = mutableData.getValue(Integer.class);
                                                if (likeOne == null) {
                                                    return Transaction.success(mutableData);
                                                }
                                                likeOne++;
                                                mutableData.setValue(likeOne);
                                                return Transaction.success(mutableData);

                                            }

                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                                Toast.makeText(MainActivity.this, "Голос ушел, все ок!!!", Toast.LENGTH_SHORT);
                                            }
                                        });

                                        mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(choose.getmKey()).child("VotesUSers").
                                                child(mFirebaseAuth.getCurrentUser().getDisplayName()).setValue(mFirebaseUser.getUid());
                                    }
                                });

                                viewHolder.messageImageViewTwo.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(final View view) {

                                        DatabaseReference upvotes = mFirebaseDatabaseReference.child(MESSAGES_CHILD)
                                                .child(choose.getmKey()).child("chooseTwo");
                                        upvotes.runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                likeTwo = mutableData.getValue(Integer.class);
                                                if (likeTwo == null) {
                                                    return Transaction.success(mutableData);
                                                }

                                                likeTwo++;
                                                mutableData.setValue(likeTwo);
                                                Log.i(TAG, "value vote: " + likeTwo);
                                                return Transaction.success(mutableData);

                                            }

                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                                Toast.makeText(MainActivity.this, "Голос ушел, все ок!!!", Toast.LENGTH_SHORT);
                                            }
                                        });
                                        mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(choose.getmKey()).child("VotesUSers").
                                                child(mFirebaseAuth.getCurrentUser().getDisplayName()).setValue(mFirebaseUser.getUid());

                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w(TAG, "Error, in update DB");
                        }
                    };

                    mFirebaseDatabaseReference.addValueEventListener(valueEventListenerm);
                }

                viewHolder.messengerTextView.setText(choose.getNameUser());
                if (choose.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Picasso.with(MainActivity.this)
                            .load(choose.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SingInActivity.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void addImage(View v) {
        Intent addImageObj = new Intent(this, AddImage.class);
        startActivity(addImageObj);
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }
}