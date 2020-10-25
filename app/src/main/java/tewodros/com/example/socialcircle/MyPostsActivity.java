package tewodros.com.example.socialcircle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class MyPostsActivity extends AppCompatActivity {

    private Toolbar mToolBar;
    private RecyclerView myPostsList;
    private FirebaseAuth mAuth;
    private DatabaseReference postsRef,usersRef,likesRef;
    private String currentUserId;
    Boolean likeChecker = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");


        mToolBar = findViewById(R.id.my_posts_bar_layout);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setIcon(getDrawable(R.drawable.social_circle_app_icon));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Friends");

        myPostsList = findViewById(R.id.my_post_list);
        myPostsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myPostsList.setLayoutManager(linearLayoutManager);

        DisplayMyAllPosts();


    }

    private void DisplayMyAllPosts() {
        Query myPosts = postsRef.orderByChild("uid").startAt(currentUserId).endAt(currentUserId+"\uf8ff");
        FirebaseRecyclerOptions<Posts> options = new FirebaseRecyclerOptions.Builder<Posts>()
                .setQuery(myPosts,Posts.class).build();
        FirebaseRecyclerAdapter<Posts,MyPostsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Posts, MyPostsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final MyPostsViewHolder holder, int position, @NonNull Posts model) {
                final String postKey = getRef(position).getKey();


                holder.username.setText(model.getFullname());
                Picasso.get().load(model.getProfileimage()).into(holder.image);
                holder.postTime.setText(model.getTime());
                holder.postDate.setText(model.getDate());
                holder.postDescription.setText(model.getDescription());
                Picasso.get().load(model.getPostimage()).into(holder.postImage);

                holder.setLikesButtonStatus(postKey);

                holder.displayNumberOfLikes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent clickLikeIntent = new Intent(MyPostsActivity.this,LikeActivity.class);
                        clickLikeIntent.putExtra("PostKey",postKey);
                        startActivity(clickLikeIntent);
                    }
                });
                holder.mView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent clickPostIntent = new Intent(MyPostsActivity.this,ClickPostActivity.class);
                                                        clickPostIntent.putExtra("PostKey",postKey);
                                                        startActivity(clickPostIntent);
                                                    }
                                                }
                );
                holder.sharePostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String description = holder.postDescription.getText().toString();
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.postImage.getDrawable();
                        Bitmap bitmap = bitmapDrawable.getBitmap();

                        shareImageAndText(description,bitmap);
                    }
                });
                holder.commentPostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent commentsIntent = new Intent(MyPostsActivity.this,CommentsActivity.class);
                        commentsIntent.putExtra("PostKey",postKey);
                        startActivity(commentsIntent);
                    }
                });
                holder.shareText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String description = holder.postDescription.getText().toString();
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.postImage.getDrawable();
                        Bitmap bitmap = bitmapDrawable.getBitmap();

                        shareImageAndText(description,bitmap);
                    }
                });
                holder.commentText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent commentsIntent = new Intent(MyPostsActivity.this,CommentsActivity.class);
                        commentsIntent.putExtra("PostKey",postKey);
                        startActivity(commentsIntent);
                    }
                });
                holder.likePostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        likeChecker = true;

                        likesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(likeChecker.equals(true)){
                                    if(dataSnapshot.child(postKey).hasChild(currentUserId)){
                                        likesRef.child(postKey).child(currentUserId).removeValue();
                                        likeChecker = false;
                                    }
                                    else{
                                        likesRef.child(postKey).child(currentUserId).setValue(true);
                                        usersRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.exists()) {

                                                    String fullName = dataSnapshot.child("fullname").getValue().toString();
                                                    String image = dataSnapshot.child("profileimage").getValue().toString();

                                                    HashMap likeMap = new HashMap();
                                                    likeMap.put("fullname",fullName);
                                                    likeMap.put("profileimage",image);

                                                    likesRef.child(postKey).child(currentUserId).updateChildren(likeMap);

                                                }

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                        likeChecker = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }

            @NonNull
            @Override
            public MyPostsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.all_posts_layout,parent,false);
                MyPostsViewHolder holder = new MyPostsViewHolder(view);
                return holder;
            }
        };
        myPostsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }
    private void shareImageAndText(String description, Bitmap bitmap) {
        Uri uri = saveImageToShare(bitmap);

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM,uri);
        sIntent.putExtra(Intent.EXTRA_TEXT,description);
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent,"Share Via"));


    }
    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(MyPostsActivity.this.getCacheDir(),"images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();
            File file = new File(imageFolder,"shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(MyPostsActivity.this,"tewodros.com.example.socialcircle.fileprovider",file);
        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

        }
        return uri;
    }

    public static class MyPostsViewHolder extends RecyclerView.ViewHolder{
        View mView;
        TextView username;
        CircleImageView image;
        TextView postTime;
        TextView postDate;
        TextView postDescription;
        ImageView postImage;
        ImageButton likePostButton,commentPostButton,sharePostButton;
        TextView displayNumberOfLikes,commentText,shareText;
        int countLikes;
        String currentUserId;
        DatabaseReference likesRef;
        public MyPostsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            username = mView.findViewById(R.id.post_user_name);
            image = mView.findViewById(R.id.post_profile_image);
            postTime = mView.findViewById(R.id.post_time);
            postDate = mView.findViewById(R.id.post_date);
            postDescription = mView.findViewById(R.id.post_description);
            postImage = mView.findViewById(R.id.post_image);
            likePostButton = mView.findViewById(R.id.like_button);
            commentPostButton = mView.findViewById(R.id.comment_button);
            sharePostButton = mView.findViewById(R.id.share_button);
            commentText = mView.findViewById(R.id.comment_text);
            shareText = mView.findViewById(R.id.share_text);

            displayNumberOfLikes = mView.findViewById(R.id.display_number_of_likes);
            likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        }
        public void setLikesButtonStatus(final String postKey) {
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(postKey).hasChild(currentUserId)){
                        countLikes = (int) dataSnapshot.child(postKey).getChildrenCount();
                        likePostButton.setImageResource(R.drawable.like);
                        displayNumberOfLikes.setText((Integer.toString(countLikes))+" likes");

                    }
                    else{
                        countLikes = (int) dataSnapshot.child(postKey).getChildrenCount();
                        likePostButton.setImageResource(R.drawable.dislike);
                        displayNumberOfLikes.setText((Integer.toString(countLikes))+" likes");

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
    }

