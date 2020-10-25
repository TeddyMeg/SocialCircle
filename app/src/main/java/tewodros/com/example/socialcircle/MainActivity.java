package tewodros.com.example.socialcircle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private RecyclerView postList;
    private Toolbar mToolBar;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private FirebaseAuth mAuth;

    private DatabaseReference usersRef,postsRef,likesRef;

    private CircleImageView navProfileImage;
    private TextView navProfileFullName;
    private TextView navProfileUserName;

    private String currentUserId;

    private ImageButton addNewPostButton;

    Boolean likeChecker = false;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_post:
                    SendUserToPostActivity();
                    return true;
                case R.id.navigation_logout:
                    updateUserStatus("offline");
                    mAuth.signOut();
                    SendUserToLoginActivity();
                    return true;
            }


            return false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        mToolBar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setIcon(getDrawable(R.drawable.social_circle_app_icon));

        addNewPostButton = findViewById(R.id.add_new_post);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this,drawerLayout,mToolBar,R.string.drawer_open,R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        postList  = findViewById(R.id.all_users_post_list);
        postList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        postList.setLayoutManager(linearLayoutManager);

        View v = navigationView.inflateHeaderView(R.layout.navigation_header);
        navProfileImage = v.findViewById(R.id.nav_user_image);
        navProfileFullName = v.findViewById(R.id.nav_user_full_name);
        navProfileUserName = v.findViewById(R.id.nav_user_profile_name);


        usersRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    if(dataSnapshot.hasChild("fullname")){
                        String fullName = dataSnapshot.child("fullname").getValue().toString();
                        navProfileFullName.setText(fullName);
                    }
                    if(dataSnapshot.hasChild("username")){
                        String userName = dataSnapshot.child("username").getValue().toString();
                        navProfileUserName.setText("@"+userName);
                    }
                    if(dataSnapshot.hasChild("profileimage")){
                        String image = dataSnapshot.child("profileimage").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(navProfileImage);
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Profile name does not exist", Toast.LENGTH_SHORT).show();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                UserMenuSelector(menuItem);
                return false;
            }
        });

        addNewPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToPostActivity();
            }
        });

        DisplayAllUsersPosts();

    }
    public void updateUserStatus(String state){
        String saveCurrentDate,saveCurrentTime;

        Calendar calForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        Calendar calForTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calForTime.getTime());

        Map currentStateMap = new HashMap();
        currentStateMap.put("time",saveCurrentTime);
        currentStateMap.put("date",saveCurrentDate);
        currentStateMap.put("type",state);

        usersRef.child(currentUserId).child("userState")
                .updateChildren(currentStateMap);


    }

    private void DisplayAllUsersPosts() {

        Query sortPostsInDescendingOrder = postsRef.orderByChild("counter");

        FirebaseRecyclerOptions<Posts> options = new FirebaseRecyclerOptions.Builder<Posts>()
                .setQuery(sortPostsInDescendingOrder,Posts.class).build();
        FirebaseRecyclerAdapter<Posts,PostsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Posts, PostsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final PostsViewHolder holder, int position, @NonNull Posts model) {

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
                        Intent clickLikeIntent = new Intent(MainActivity.this,LikeActivity.class);
                        clickLikeIntent.putExtra("PostKey",postKey);
                        startActivity(clickLikeIntent);
                    }
                });
                holder.mView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                             Intent clickPostIntent = new Intent(MainActivity.this,ClickPostActivity.class);
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
                        Intent commentsIntent = new Intent(MainActivity.this,CommentsActivity.class);
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
                        Intent commentsIntent = new Intent(MainActivity.this,CommentsActivity.class);
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
            public PostsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.all_posts_layout,parent,false);
                PostsViewHolder holder = new PostsViewHolder(view);
                return holder;
            }
        };
        postList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();

        updateUserStatus("online");

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
        File imageFolder = new File(MainActivity.this.getCacheDir(),"images");
        Uri uri = null;
        try {
            imageFolder.mkdirs();
            File file = new File(imageFolder,"shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(MainActivity.this,"tewodros.com.example.socialcircle.fileprovider",file);
        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

        }
        return uri;
    }

    private void SendUserToPostActivity() {
        Intent addNewPostIntent = new Intent(MainActivity.this,PostActivity.class);
        startActivity(addNewPostIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            SendUserToLoginActivity();
        }
        else{
            CheckUserExistence();
        }
    }

    private void CheckUserExistence() {
        final String currentUserId = mAuth.getCurrentUser().getUid();

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(currentUserId)){
                    SendUserToSetupActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUserToSetupActivity() {
        Intent setupIntent = new Intent(MainActivity.this,SetupActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
    private void SendUserToSettingsActivity() {
        Intent settingsIntent = new Intent(MainActivity.this,SettingsActivity.class);
        startActivity(settingsIntent);

    }
    private void SendUserToProfileActivity() {
        Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(profileIntent);

    }
    private void SendUserToFindFriendsActivity() {
        Intent findFriendsIntent = new Intent(MainActivity.this, FindFriendsActivity.class);
        startActivity(findFriendsIntent);

    }
    private void SendUserToFriendsActivity() {
        Intent friendsIntent = new Intent(MainActivity.this, FriendsActivity.class);
        startActivity(friendsIntent);

    }
    private void SendUserToMessagesActivity() {
        Intent messagesIntent = new Intent(MainActivity.this, FriendsActivity.class);
        startActivity(messagesIntent);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(actionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void UserMenuSelector(MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.nav_profile:
                SendUserToProfileActivity();
                break;
            case R.id.nav_friends:
                SendUserToFriendsActivity();
                break;
            case R.id.nav_find_friends:
                SendUserToFindFriendsActivity();
                break;
            case R.id.nav_messages:
                SendUserToMessagesActivity();
                break;
            case R.id.nav_settings:
                SendUserToSettingsActivity();
                break;



        }
    }

    public static class PostsViewHolder extends RecyclerView.ViewHolder{


        TextView username;
        CircleImageView image;
        TextView postTime;
        TextView postDate;
        TextView postDescription;
        ImageView postImage;
        View mView;
        ImageButton likePostButton,commentPostButton,sharePostButton;

        TextView displayNumberOfLikes,commentText,shareText;
        int countLikes;
        String currentUserId;
        DatabaseReference likesRef;




        public PostsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
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
